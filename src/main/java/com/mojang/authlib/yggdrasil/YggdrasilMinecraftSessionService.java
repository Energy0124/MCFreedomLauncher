package com.mojang.authlib.yggdrasil;

import com.google.common.collect.Iterables;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.HttpAuthenticationService;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.minecraft.HttpMinecraftSessionService;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.yggdrasil.request.JoinMinecraftServerRequest;
import com.mojang.authlib.yggdrasil.response.HasJoinedMinecraftServerResponse;
import com.mojang.authlib.yggdrasil.response.MinecraftProfilePropertiesResponse;
import com.mojang.authlib.yggdrasil.response.MinecraftTexturesPayload;
import com.mojang.authlib.yggdrasil.response.Response;
import com.mojang.util.UUIDTypeAdapter;
import org.apache.commons.codec.Charsets;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URL;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;

public class YggdrasilMinecraftSessionService
        extends HttpMinecraftSessionService {
    private static final Logger LOGGER;
    private static final String BASE_URL = "https://sessionserver.mojang.com/session/minecraft/";
    private static final URL JOIN_URL = HttpAuthenticationService.constantURL("https://sessionserver.mojang.com/session/minecraft/join");
    private static final URL CHECK_URL = HttpAuthenticationService.constantURL("https://sessionserver.mojang.com/session/minecraft/hasJoined");

    static {
        LOGGER = LogManager.getLogger();
    }

    private final PublicKey publicKey;
    private final Gson gson = new GsonBuilder().registerTypeAdapter(UUID.class, new UUIDTypeAdapter()).create();

    protected YggdrasilMinecraftSessionService(YggdrasilAuthenticationService authenticationService) {
        super(authenticationService);
        try {
            X509EncodedKeySpec spec = new X509EncodedKeySpec(IOUtils.toByteArray(YggdrasilMinecraftSessionService.class.getResourceAsStream("/yggdrasil_session_pubkey.der")));
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            this.publicKey = keyFactory.generatePublic(spec);
        } catch (Exception e) {
            throw new Error("Missing/invalid yggdrasil public key!");
        }
    }

    public void joinServer(GameProfile profile, String authenticationToken, String serverId)
            throws AuthenticationException {
        JoinMinecraftServerRequest request = new JoinMinecraftServerRequest();
        request.accessToken = authenticationToken;
        request.selectedProfile = profile.getId();
        request.serverId = serverId;

        getAuthenticationService().makeRequest(JOIN_URL, request, Response.class, profile.getName());
    }

    public GameProfile hasJoinedServer(GameProfile user, String serverId)
            throws AuthenticationUnavailableException {
        Map<String, Object> arguments = new HashMap();

        arguments.put("username", user.getName());
        arguments.put("serverId", serverId);

        URL url = HttpAuthenticationService.concatenateURL(CHECK_URL, HttpAuthenticationService.buildQuery(arguments));
        try {
            HasJoinedMinecraftServerResponse response = (HasJoinedMinecraftServerResponse) getAuthenticationService().makeRequest(url, null, HasJoinedMinecraftServerResponse.class, user.getName());
            if ((response != null) && (response.getId() != null)) {
                GameProfile result = new GameProfile(response.getId(), user.getName());
                if (response.getProperties() != null) {
                    result.getProperties().putAll(response.getProperties());
                }
                return result;
            }
            return null;
        } catch (AuthenticationUnavailableException e) {
            throw e;
        } catch (AuthenticationException e) {
        }
        return null;
    }

    public Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> getTextures(GameProfile profile, boolean requireSecure) {
        Property textureProperty = (Property) Iterables.getFirst(profile.getProperties().get("textures"), null);
        if (textureProperty == null) {
            return new HashMap();
        }
        if (!textureProperty.hasSignature()) {
            LOGGER.error("Signature is missing from textures payload");
            return new HashMap();
        }
        if (!textureProperty.isSignatureValid(this.publicKey)) {
            LOGGER.error("Textures payload has been tampered with (signature invalid)");
            return new HashMap();
        }
        MinecraftTexturesPayload result;
        try {
            String json = new String(Base64.decodeBase64(textureProperty.getValue()), Charsets.UTF_8);
            result = (MinecraftTexturesPayload) this.gson.fromJson(json, MinecraftTexturesPayload.class);
        } catch (JsonParseException e) {
            LOGGER.error("Could not decode textures payload", e);
            return new HashMap();
        }
        if ((result.getProfileId() == null) || (!result.getProfileId().equals(profile.getId()))) {
            LOGGER.error("Decrypted textures payload was for another user (expected id {} but was for {})", profile.getId(), result.getProfileId());
            return new HashMap();
        }
        if ((result.getProfileName() == null) || (!result.getProfileName().equals(profile.getName()))) {
            LOGGER.error("Decrypted textures payload was for another user (expected name {} but was for {})", profile.getName(), result.getProfileName());
            return new HashMap();
        }
        if (requireSecure) {
            if (result.isPublic()) {
                LOGGER.error("Decrypted textures payload was public but we require secure data");
                return new HashMap();
            }
            Calendar limit = Calendar.getInstance();
            limit.add(5, -1);
            Date validFrom = new Date(result.getTimestamp());
            if (validFrom.before(limit.getTime())) {
                LOGGER.error("Decrypted textures payload is too old ({0}, but we need it to be at least {1})", validFrom, limit);
                return new HashMap();
            }
        }
        return result.getTextures() == null ? new HashMap() : result.getTextures();
    }

    public GameProfile fillProfileProperties(GameProfile profile) {
        if (profile.getId() == null) {
            return profile;
        }
        try {
            URL url = HttpAuthenticationService.constantURL("https://sessionserver.mojang.com/session/minecraft/profile/" + UUIDTypeAdapter.fromUUID(profile.getId()));
            MinecraftProfilePropertiesResponse response = (MinecraftProfilePropertiesResponse) getAuthenticationService().makeRequest(url, null, MinecraftProfilePropertiesResponse.class, profile.getName());
            if (response == null) {
                LOGGER.debug("Couldn't fetch profile properties for " + profile + " as the profile does not exist");
                return profile;
            }
            LOGGER.debug("Successfully fetched profile properties for " + profile);
            GameProfile result = new GameProfile(response.getId(), response.getName());
            result.getProperties().putAll(response.getProperties());
            profile.getProperties().putAll(response.getProperties());
            return result;
        } catch (AuthenticationException e) {
            LOGGER.warn("Couldn't look up profile properties for " + profile, e);
        }
        return profile;
    }

    public YggdrasilAuthenticationService getAuthenticationService() {
        return (YggdrasilAuthenticationService) super.getAuthenticationService();
    }
}