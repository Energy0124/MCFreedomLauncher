package com.mojang.authlib.yggdrasil;

import com.google.gson.*;
import com.mojang.authlib.*;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.exceptions.InvalidCredentialsException;
import com.mojang.authlib.exceptions.UserMigratedException;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.authlib.yggdrasil.response.ProfileSearchResultsResponse;
import com.mojang.authlib.yggdrasil.response.Response;
import com.mojang.util.UUIDTypeAdapter;
import io.github.lightwayup.minecraftfreedomlauncher.utility.IconManager;
import net.minecraft.launcher.ui.popups.login.LogInPopup;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.Proxy;
import java.net.URL;
import java.util.UUID;

import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static net.minecraft.launcher.LauncherConstants.*;

public class YggdrasilAuthenticationService extends HttpAuthenticationService {
    private static final Logger LOGGER = LogManager.getLogger();
    private final String clientToken;
    private final Gson gson;

    public YggdrasilAuthenticationService(final Proxy proxy, final String clientToken) {
        super(proxy);
        this.clientToken = clientToken;
        final GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(GameProfile.class, new GameProfileSerializer());
        builder.registerTypeAdapter(PropertyMap.class, new PropertyMap.Serializer());
        builder.registerTypeAdapter(UUID.class, new UUIDTypeAdapter());
        builder.registerTypeAdapter(ProfileSearchResultsResponse.class, new ProfileSearchResultsResponse.Serializer());
        this.gson = builder.create();
    }

    @Override
    public UserAuthentication createUserAuthentication(final Agent agent) {
        return new YggdrasilUserAuthentication(this, agent);
    }

    @Override
    public MinecraftSessionService createMinecraftSessionService() {
        return new YggdrasilMinecraftSessionService(this);
    }

    @Override
    public GameProfileRepository createProfileRepository() {
        return new YggdrasilGameProfileRepository(this);
    }

    protected <T extends Response> T makeRequest(final URL url, final Object input, final Class<T> classOfT, String username) throws AuthenticationException {
        if (LogInPopup.isPremium()) {
            try {
                final String jsonResult = (input == null) ? this.performGetRequest(url) : this.performPostRequest(url, this.gson.toJson(input), "application/json");
                final T result = this.gson.fromJson(jsonResult, classOfT);
                if (result == null) {
                    return null;
                }
                if (!StringUtils.isNotBlank(result.getError())) {
                    return result;
                }
                if ("UserMigratedException".equals(result.getCause())) {
                    throw new UserMigratedException(result.getErrorMessage());
                }
                if (result.getError().equals("ForbiddenOperationException")) {
                    throw new InvalidCredentialsException(result.getErrorMessage());
                }
                throw new AuthenticationException(result.getErrorMessage());
            } catch (IOException | JsonParseException | IllegalStateException e) {
                throw new AuthenticationUnavailableException("Cannot contact authentication server", e);
            }
        } else {
            String uuidn = getUUID(username);
            String jsonResult = "{\n" + "    \"accessToken\": \"00000000000000000000000000000000\",\n" + "    \"clientToken\": \"" + getClientToken() + "\",\n" + "    \"selectedProfile\": {\"name\":\"" + username + "\",\"id\":\"" + uuidn + "\"},\n" + "    \"availableProfiles\":[{\"name\":\"" + username + "\",\"id\":\"" + uuidn + "\"}],\n" + "    \"error\": \"\",\n" + "    \"errorMessage\":\"\"\n" + "    \n" + "}";
            return this.gson.fromJson(jsonResult, classOfT);
        }
    }

    public String getClientToken() {
        return this.clientToken;
    }

    public String getUUID(String username) throws AuthenticationException {
        String uuid;
        try {
            URL url = new URL(URL_USERS_PROFILES_API + "minecraft/" + username);
            String jsonResult = performGetRequest(url);
            Profile result = this.gson.fromJson(jsonResult, Profile.class);
            if (result != null) {
                uuid = result.getId();
                LOGGER.info("Successfully retrieved " + username + "'s UUID " + uuid);
            } else {
                LOGGER.warn("Unable to get UUID, falling back to default");
                uuid = "00000000000000000000000000000000";
            }
        } catch (IOException | JsonParseException | IllegalStateException e) {
            LOGGER.error("Cannot contact authentication server");
            try {
                JOptionPane.showMessageDialog(null, MESSAGE_CANNOT_CONNECT_ONE + " " + MESSAGE_TRY_AGAIN, MINECRAFT_FREEDOM_LAUNCHER_WINDOW_TITLE, ERROR_MESSAGE, IconManager.getIcon());
            } catch (Exception e1) {
                LOGGER.debug("Unable to get favicon");
            }
            throw new AuthenticationUnavailableException("Cannot contact authentication server", e);
        }
        return uuid;
    }

    private static class GameProfileSerializer implements JsonSerializer<GameProfile>, JsonDeserializer<GameProfile> {
        @Override
        public GameProfile deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) throws JsonParseException {
            final JsonObject object = (JsonObject) json;
            final UUID id = object.has("id") ? context.deserialize(object.get("id"), UUID.class) : null;
            final String name = object.has("name") ? object.getAsJsonPrimitive("name").getAsString() : null;
            return new GameProfile(id, name);
        }

        @Override
        public JsonElement serialize(final GameProfile src, final Type typeOfSrc, final JsonSerializationContext context) {
            final JsonObject result = new JsonObject();
            if (src.getId() != null) {
                result.add("id", context.serialize(src.getId()));
            }
            if (src.getName() != null) {
                result.addProperty("name", src.getName());
            }
            return result;
        }
    }

    public class Profile {
        private String name;
        private String id;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getId() {
            return id;
        }
    }

}
