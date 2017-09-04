package net.minecraft.launcher.profile;

import com.google.gson.*;
import com.mojang.authlib.AuthenticationService;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.UserAuthentication;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;

import java.lang.reflect.Type;
import java.util.*;

public class AuthenticationDatabase {
    public static final String DEMO_UUID_PREFIX = "demo-";
    private final Map<String, UserAuthentication> authById;
    private final AuthenticationService authenticationService;

    public AuthenticationDatabase(AuthenticationService authenticationService) {
        this(new HashMap(), authenticationService);
    }

    public AuthenticationDatabase(Map<String, UserAuthentication> authById, AuthenticationService authenticationService) {
        this.authById = authById;
        this.authenticationService = authenticationService;
    }

    public static String getUserFromDemoUUID(String uuid) {
        if ((uuid.startsWith("demo-")) && (uuid.length() > "demo-".length())) {
            return "Demo User " + uuid.substring("demo-".length());
        }
        return "Demo User";
    }

    public UserAuthentication getByName(String name) {
        if (name == null) {
            return null;
        }
        for (Map.Entry<String, UserAuthentication> entry : this.authById.entrySet()) {
            GameProfile profile = ((UserAuthentication) entry.getValue()).getSelectedProfile();
            if ((profile != null) && (profile.getName().equals(name))) {
                return (UserAuthentication) entry.getValue();
            }
            if ((profile == null) && (getUserFromDemoUUID((String) entry.getKey()).equals(name))) {
                return (UserAuthentication) entry.getValue();
            }
        }
        return null;
    }

    public UserAuthentication getByUUID(String uuid) {
        return (UserAuthentication) this.authById.get(uuid);
    }

    public Collection<String> getKnownNames() {
        List<String> names = new ArrayList();
        for (Map.Entry<String, UserAuthentication> entry : this.authById.entrySet()) {
            GameProfile profile = ((UserAuthentication) entry.getValue()).getSelectedProfile();
            if (profile != null) {
                names.add(profile.getName());
            } else {
                names.add(getUserFromDemoUUID((String) entry.getKey()));
            }
        }
        return names;
    }

    public void register(String uuid, UserAuthentication authentication) {
        this.authById.put(uuid, authentication);
    }

    public Set<String> getknownUUIDs() {
        return this.authById.keySet();
    }

    public void removeUUID(String uuid) {
        this.authById.remove(uuid);
    }

    public AuthenticationService getAuthenticationService() {
        return this.authenticationService;
    }

    public static class Serializer
            implements JsonDeserializer<AuthenticationDatabase>, JsonSerializer<AuthenticationDatabase> {
        private final net.minecraft.launcher.Launcher launcher;

        public Serializer(net.minecraft.launcher.Launcher launcher) {
            this.launcher = launcher;
        }

        public AuthenticationDatabase deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            Map<String, UserAuthentication> services = new HashMap();
            Map<String, Map<String, Object>> credentials = deserializeCredentials((JsonObject) json, context);
            YggdrasilAuthenticationService authService = new YggdrasilAuthenticationService(this.launcher.getLauncher().getProxy(), this.launcher.getClientToken().toString());
            for (Map.Entry<String, Map<String, Object>> entry : credentials.entrySet()) {
                UserAuthentication auth = authService.createUserAuthentication(this.launcher.getLauncher().getAgent());
                auth.loadFromStorage((Map) entry.getValue());
                services.put(entry.getKey(), auth);
            }
            return new AuthenticationDatabase(services, authService);
        }

        protected Map<String, Map<String, Object>> deserializeCredentials(JsonObject json, JsonDeserializationContext context) {
            Map<String, Map<String, Object>> result = new LinkedHashMap();
            for (Map.Entry<String, JsonElement> authEntry : json.entrySet()) {
                Map<String, Object> credentials = new LinkedHashMap();
                for (Map.Entry<String, JsonElement> credentialsEntry : ((JsonObject) authEntry.getValue()).entrySet()) {
                    credentials.put(credentialsEntry.getKey(), deserializeCredential((JsonElement) credentialsEntry.getValue()));
                }
                result.put(authEntry.getKey(), credentials);
            }
            return result;
        }

        private Object deserializeCredential(JsonElement element) {
            if ((element instanceof JsonObject)) {
                Map<String, Object> result = new LinkedHashMap();
                for (Map.Entry<String, JsonElement> entry : ((JsonObject) element).entrySet()) {
                    result.put(entry.getKey(), deserializeCredential((JsonElement) entry.getValue()));
                }
                return result;
            }
            if ((element instanceof JsonArray)) {
                List<Object> result = new ArrayList();
                for (JsonElement entry : (JsonArray) element) {
                    result.add(deserializeCredential(entry));
                }
                return result;
            }
            return element.getAsString();
        }

        public JsonElement serialize(AuthenticationDatabase src, Type typeOfSrc, JsonSerializationContext context) {
            Map<String, UserAuthentication> services = src.authById;
            Map<String, Map<String, Object>> credentials = new HashMap();
            for (Map.Entry<String, UserAuthentication> entry : services.entrySet()) {
                credentials.put(entry.getKey(), ((UserAuthentication) entry.getValue()).saveForStorage());
            }
            return context.serialize(credentials);
        }
    }
}