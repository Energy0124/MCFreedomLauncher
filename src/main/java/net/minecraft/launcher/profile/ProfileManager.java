package net.minecraft.launcher.profile;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.launcher.updater.DateTypeAdapter;
import com.mojang.launcher.updater.FileTypeAdapter;
import com.mojang.launcher.updater.LowerCaseEnumTypeAdapterFactory;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;

public class ProfileManager {
    public static final String DEFAULT_PROFILE_NAME = "(Default)";
    private final net.minecraft.launcher.Launcher launcher;
    private final JsonParser parser = new JsonParser();
    private final Gson gson;
    private final Map<String, Profile> profiles = new HashMap();
    private final File profileFile;
    private final List<RefreshedProfilesListener> refreshedProfilesListeners = Collections.synchronizedList(new ArrayList());
    private final List<UserChangedListener> userChangedListeners = Collections.synchronizedList(new ArrayList());
    private String selectedProfile;
    private String selectedUser;
    private AuthenticationDatabase authDatabase;

    public ProfileManager(net.minecraft.launcher.Launcher launcher) {
        this.launcher = launcher;
        this.profileFile = new File(launcher.getLauncher().getWorkingDirectory(), "launcher_profiles.json");

        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapterFactory(new LowerCaseEnumTypeAdapterFactory());
        builder.registerTypeAdapter(Date.class, new DateTypeAdapter());
        builder.registerTypeAdapter(File.class, new FileTypeAdapter());
        builder.registerTypeAdapter(AuthenticationDatabase.class, new AuthenticationDatabase.Serializer(launcher));
        builder.registerTypeAdapter(RawProfileList.class, new ProfileManager.RawProfileList.Serializer(launcher));
        builder.setPrettyPrinting();
        this.gson = builder.create();
        this.authDatabase = new AuthenticationDatabase(new YggdrasilAuthenticationService(launcher.getLauncher().getProxy(), launcher.getClientToken().toString()));
    }

    public void saveProfiles()
            throws IOException {
        RawProfileList rawProfileList = new RawProfileList(this.profiles, getSelectedProfile().getName(), this.selectedUser, this.launcher.getClientToken(), this.authDatabase);

        FileUtils.writeStringToFile(this.profileFile, this.gson.toJson(rawProfileList));
    }

    public boolean loadProfiles()
            throws IOException {
        this.profiles.clear();
        this.selectedProfile = null;
        this.selectedUser = null;
        if (this.profileFile.isFile()) {
            JsonObject object = this.parser.parse(FileUtils.readFileToString(this.profileFile)).getAsJsonObject();
            if (object.has("clientToken")) {
                this.launcher.setClientToken((UUID) this.gson.fromJson(object.get("clientToken"), UUID.class));
            }
            RawProfileList rawProfileList = (RawProfileList) this.gson.<RawProfileList>fromJson(object, RawProfileList.class);

            this.profiles.putAll(rawProfileList.profiles);
            this.selectedProfile = rawProfileList.selectedProfile;
            this.selectedUser = rawProfileList.selectedUser;
            this.authDatabase = rawProfileList.authenticationDatabase;

            fireRefreshEvent();
            fireUserChangedEvent();
            return true;
        }
        fireRefreshEvent();
        fireUserChangedEvent();
        return false;
    }

    public void fireRefreshEvent() {
        for (RefreshedProfilesListener listener : Lists.newArrayList(this.refreshedProfilesListeners)) {
            listener.onProfilesRefreshed(this);
        }
    }

    public void fireUserChangedEvent() {
        for (UserChangedListener listener : Lists.newArrayList(this.userChangedListeners)) {
            listener.onUserChanged(this);
        }
    }

    public Profile getSelectedProfile() {
        if ((this.selectedProfile == null) || (!this.profiles.containsKey(this.selectedProfile))) {
            if (this.profiles.get("(Default)") != null) {
                this.selectedProfile = "(Default)";
            } else if (this.profiles.size() > 0) {
                this.selectedProfile = ((Profile) this.profiles.values().iterator().next()).getName();
            } else {
                this.selectedProfile = "(Default)";
                this.profiles.put("(Default)", new Profile(this.selectedProfile));
            }
        }
        return (Profile) this.profiles.get(this.selectedProfile);
    }

    public void setSelectedProfile(String selectedProfile) {
        boolean update = !this.selectedProfile.equals(selectedProfile);
        this.selectedProfile = selectedProfile;
        if (update) {
            fireRefreshEvent();
        }
    }

    public Map<String, Profile> getProfiles() {
        return this.profiles;
    }

    public void addRefreshedProfilesListener(RefreshedProfilesListener listener) {
        this.refreshedProfilesListeners.add(listener);
    }

    public void addUserChangedListener(UserChangedListener listener) {
        this.userChangedListeners.add(listener);
    }

    public String getSelectedUser() {
        return this.selectedUser;
    }

    public void setSelectedUser(String selectedUser) {
        boolean update = !Objects.equal(this.selectedUser, selectedUser);
        if (update) {
            this.selectedUser = selectedUser;
            fireUserChangedEvent();
        }
    }

    public AuthenticationDatabase getAuthDatabase() {
        return this.authDatabase;
    }

    private static class RawProfileList {
        public Map<String, Profile> profiles = new HashMap();
        public String selectedProfile;
        public String selectedUser;
        public UUID clientToken = UUID.randomUUID();
        public AuthenticationDatabase authenticationDatabase;

        private RawProfileList(Map<String, Profile> profiles, String selectedProfile, String selectedUser, UUID clientToken, AuthenticationDatabase authenticationDatabase) {
            this.profiles = profiles;
            this.selectedProfile = selectedProfile;
            this.selectedUser = selectedUser;
            this.clientToken = clientToken;
            this.authenticationDatabase = authenticationDatabase;
        }

        public static class Serializer
                implements JsonDeserializer<ProfileManager.RawProfileList>, JsonSerializer<ProfileManager.RawProfileList> {
            private final net.minecraft.launcher.Launcher launcher;

            public Serializer(net.minecraft.launcher.Launcher launcher) {
                this.launcher = launcher;
            }

            public ProfileManager.RawProfileList deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                    throws JsonParseException {
                JsonObject object = (JsonObject) json;
                Map<String, Profile> profiles = Maps.newHashMap();
                if (object.has("profiles")) {
                    profiles = (Map) context.deserialize(object.get("profiles"), new TypeToken<Map<String, Profile>>() {
                    }.getType());
                }
                String selectedProfile = null;
                if (object.has("selectedProfile")) {
                    selectedProfile = object.getAsJsonPrimitive("selectedProfile").getAsString();
                }
                UUID clientToken = UUID.randomUUID();
                if (object.has("clientToken")) {
                    clientToken = (UUID) context.deserialize(object.get("clientToken"), UUID.class);
                }
                AuthenticationDatabase database = new AuthenticationDatabase(new YggdrasilAuthenticationService(this.launcher.getLauncher().getProxy(), this.launcher.getClientToken().toString()));
                if (object.has("authenticationDatabase")) {
                    database = (AuthenticationDatabase) context.deserialize(object.get("authenticationDatabase"), AuthenticationDatabase.class);
                }
                String selectedUser = null;
                if (object.has("selectedUser")) {
                    selectedUser = object.getAsJsonPrimitive("selectedUser").getAsString();
                } else if (!database.getknownUUIDs().isEmpty()) {
                    selectedUser = (String) database.getknownUUIDs().iterator().next();
                }

                return new ProfileManager.RawProfileList(profiles, selectedProfile, selectedUser, clientToken, database);
            }

            public JsonElement serialize(ProfileManager.RawProfileList src, Type typeOfSrc, JsonSerializationContext context) {
                JsonObject version = new JsonObject();
                version.addProperty("name", "1.5.2");
                version.addProperty("format", Integer.valueOf(17));

                JsonObject object = new JsonObject();
                object.add("profiles", context.serialize(src.profiles));
                object.add("selectedProfile", context.serialize(src.selectedProfile));
                object.add("clientToken", context.serialize(src.clientToken));
                object.add("authenticationDatabase", context.serialize(src.authenticationDatabase));
                object.add("selectedUser", context.serialize(src.selectedUser));
                object.add("launcherVersion", version);

                return object;
            }
        }
    }
}