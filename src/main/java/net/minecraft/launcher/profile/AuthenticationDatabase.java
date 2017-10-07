package net.minecraft.launcher.profile;

import com.google.gson.*;
import com.mojang.authlib.AuthenticationService;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.UserAuthentication;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import net.minecraft.launcher.Launcher;

import java.lang.reflect.Type;
import java.util.*;

public class AuthenticationDatabase {
    public static final String DEMO_UUID_PREFIX = "demo-";
    private final Map<String, UserAuthentication> authById;
    private final AuthenticationService authenticationService;

    public AuthenticationDatabase(final AuthenticationService authenticationService) {
        this(new HashMap<>(), authenticationService);
    }

    public AuthenticationDatabase(final Map<String, UserAuthentication> authById, final AuthenticationService authenticationService) {
        this.authById = authById;
        this.authenticationService = authenticationService;
    }

    public static String getUserFromDemoUUID(final String uuid) {
        if (uuid.startsWith(DEMO_UUID_PREFIX) && uuid.length() > DEMO_UUID_PREFIX.length()) {
            return "Demo User " + uuid.substring(DEMO_UUID_PREFIX.length());
        }
        return "Demo User";
    }

    public UserAuthentication getByName(final String name) {
        if (name == null) {
            return null;
        }
        for (final Map.Entry<String, UserAuthentication> entry : this.authById.entrySet()) {
            final GameProfile profile = entry.getValue().getSelectedProfile();
            if (profile != null && profile.getName().equals(name)) {
                return entry.getValue();
            }
            if (profile == null && getUserFromDemoUUID(entry.getKey()).equals(name)) {
                return entry.getValue();
            }
        }
        return null;
    }

    public UserAuthentication getByUUID(final String uuid) {
        return this.authById.get(uuid);
    }

    public Collection<String> getKnownNames() {
        final List<String> names = new ArrayList<>();
        for (final Map.Entry<String, UserAuthentication> entry : this.authById.entrySet()) {
            final GameProfile profile = entry.getValue().getSelectedProfile();
            if (profile != null) {
                names.add(profile.getName());
            } else {
                names.add(getUserFromDemoUUID(entry.getKey()));
            }
        }
        return names;
    }

    public void register(final String uuid, final UserAuthentication authentication) {
        this.authById.put(uuid, authentication);
    }

    public Set<String> getknownUUIDs() {
        return this.authById.keySet();
    }

    public void removeUUID(final String uuid) {
        this.authById.remove(uuid);
    }

    public AuthenticationService getAuthenticationService() {
        return this.authenticationService;
    }

    public static class Serializer implements JsonDeserializer<AuthenticationDatabase>, JsonSerializer<AuthenticationDatabase> {
        private final Launcher launcher;

        public Serializer(final Launcher launcher) {
            this.launcher = launcher;
        }

        @Override
        public AuthenticationDatabase deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) throws JsonParseException {
            final Map<String, UserAuthentication> services = new HashMap<>();
            final Map<String, Map<String, Object>> credentials = this.deserializeCredentials((JsonObject) json, context);
            final YggdrasilAuthenticationService authService = new YggdrasilAuthenticationService(this.launcher.getLauncher().getProxy(), this.launcher.getClientToken().toString());
            for (final Map.Entry<String, Map<String, Object>> entry : credentials.entrySet()) {
                final UserAuthentication auth = authService.createUserAuthentication(this.launcher.getLauncher().getAgent());
                auth.loadFromStorage(entry.getValue());
                services.put(entry.getKey(), auth);
            }
            return new AuthenticationDatabase(services, authService);
        }

        protected Map<String, Map<String, Object>> deserializeCredentials(final JsonObject json, final JsonDeserializationContext context) {
            final Map<String, Map<String, Object>> result = new LinkedHashMap<>();
            for (final Map.Entry<String, JsonElement> authEntry : json.entrySet()) {
                final Map<String, Object> credentials = new LinkedHashMap<>();
                for (final Map.Entry<String, JsonElement> credentialsEntry : ((JsonObject) authEntry.getValue()).entrySet()) {
                    credentials.put(credentialsEntry.getKey(), this.deserializeCredential(credentialsEntry.getValue()));
                }
                result.put(authEntry.getKey(), credentials);
            }
            return result;
        }

        private Object deserializeCredential(final JsonElement element) {
            if (element instanceof JsonObject) {
                final Map<String, Object> result = new LinkedHashMap<>();
                for (final Map.Entry<String, JsonElement> entry : ((JsonObject) element).entrySet()) {
                    result.put(entry.getKey(), this.deserializeCredential(entry.getValue()));
                }
                return result;
            }
            if (element instanceof JsonArray) {
                final List<Object> result2 = new ArrayList<>();
                for (final JsonElement entry2 : (JsonArray) element) {
                    result2.add(this.deserializeCredential(entry2));
                }
                return result2;
            }
            return element.getAsString();
        }

        @Override
        public JsonElement serialize(final AuthenticationDatabase src, final Type typeOfSrc, final JsonSerializationContext context) {
            final Map<String, Map<String, Object>> credentials = new HashMap<>();
            for (final Map.Entry<String, UserAuthentication> entry : src.authById.entrySet()) {
                credentials.put(entry.getKey(), entry.getValue().saveForStorage());
            }
            return context.serialize(credentials);
        }
    }
}
