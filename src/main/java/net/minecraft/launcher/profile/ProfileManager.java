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
import net.minecraft.launcher.Launcher;
import net.minecraft.launcher.LauncherConstants;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;

import static java.nio.charset.StandardCharsets.UTF_8;
import static net.minecraft.launcher.LauncherConstants.FORMAT_PROFILES;
import static net.minecraft.launcher.LauncherConstants.FORMAT_VERSION;

public class ProfileManager {
    public static final String DEFAULT_PROFILE_NAME = "Minecraft";
    private final Launcher launcher;
    private final JsonParser parser;
    private final Gson gson;
    private final Map<String, Profile> profiles;
    private final File profileFile;
    private final List<RefreshedProfilesListener> refreshedProfilesListeners;
    private final List<UserChangedListener> userChangedListeners;
    private String selectedProfile;
    private String selectedUser;
    private AuthenticationDatabase authDatabase;

    public ProfileManager(final Launcher launcher) {
        this.parser = new JsonParser();
        this.profiles = new HashMap<>();
        this.refreshedProfilesListeners = Collections.synchronizedList(new ArrayList<RefreshedProfilesListener>());
        this.userChangedListeners = Collections.synchronizedList(new ArrayList<UserChangedListener>());
        this.launcher = launcher;
        this.profileFile = new File(launcher.getLauncher().getWorkingDirectory(), "launcher_profiles.json");
        final GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapterFactory(new LowerCaseEnumTypeAdapterFactory());
        builder.registerTypeAdapter(Date.class, new DateTypeAdapter());
        builder.registerTypeAdapter(File.class, new FileTypeAdapter());
        builder.registerTypeAdapter(AuthenticationDatabase.class, new AuthenticationDatabase.Serializer(launcher));
        builder.registerTypeAdapter(RawProfileList.class, new RawProfileList.Serializer(launcher));
        builder.setPrettyPrinting();
        this.gson = builder.create();
        this.authDatabase = new AuthenticationDatabase(new YggdrasilAuthenticationService(launcher.getLauncher().getProxy(), launcher.getClientToken().toString()));
    }

    public void saveProfiles() throws IOException {
        final RawProfileList rawProfileList = new RawProfileList(this.profiles, this.getSelectedProfile().getName(), this.selectedUser, this.launcher.getClientToken(), this.authDatabase);
        FileUtils.writeStringToFile(this.profileFile, this.gson.toJson(rawProfileList), UTF_8);
    }

    public void loadProfiles() throws IOException {
        this.profiles.clear();
        this.selectedProfile = null;
        this.selectedUser = null;
        if (this.profileFile.isFile()) {
            final JsonObject object = this.parser.parse(FileUtils.readFileToString(this.profileFile, UTF_8)).getAsJsonObject();
            if (object.has("launcherVersion")) {
                final JsonObject version = object.getAsJsonObject("launcherVersion");
                if (version.has("profilesFormat") && version.getAsJsonPrimitive("profilesFormat").getAsInt() != 1) {
                    if (this.launcher.getUserInterface().shouldDowngradeProfiles()) {
                        final File target = new File(this.profileFile.getParentFile(), "launcher_profiles.old.json");
                        if (target.exists()) {
                            target.delete();
                        }
                        this.profileFile.renameTo(target);
                        this.fireRefreshEvent();
                        this.fireUserChangedEvent();
                        return;
                    }
                    this.launcher.getLauncher().shutdownLauncher();
                    System.exit(0);
                    return;
                }
            }
            if (object.has("clientToken")) {
                this.launcher.setClientToken(this.gson.fromJson(object.get("clientToken"), UUID.class));
            }
            final RawProfileList rawProfileList = this.gson.fromJson(object, RawProfileList.class);
            this.profiles.putAll(rawProfileList.profiles);
            this.selectedProfile = rawProfileList.selectedProfile;
            this.selectedUser = rawProfileList.selectedUser;
            this.authDatabase = rawProfileList.authenticationDatabase;
            this.fireRefreshEvent();
            this.fireUserChangedEvent();
            return;
        }
        this.fireRefreshEvent();
        this.fireUserChangedEvent();
    }

    public void fireRefreshEvent() {
        for (final RefreshedProfilesListener listener : Lists.newArrayList(this.refreshedProfilesListeners)) {
            listener.onProfilesRefreshed(this);
        }
    }

    public void fireUserChangedEvent() {
        for (final UserChangedListener listener : Lists.newArrayList(this.userChangedListeners)) {
            listener.onUserChanged(this);
        }
    }

    public Profile getSelectedProfile() {
        if (this.selectedProfile == null || !this.profiles.containsKey(this.selectedProfile)) {
            if (this.profiles.get(DEFAULT_PROFILE_NAME) != null) {
                this.selectedProfile = DEFAULT_PROFILE_NAME;
            } else if (this.profiles.size() > 0) {
                this.selectedProfile = this.profiles.values().iterator().next().getName();
            } else {
                this.selectedProfile = DEFAULT_PROFILE_NAME;
                this.profiles.put(DEFAULT_PROFILE_NAME, new Profile(this.selectedProfile));
            }
        }
        return this.profiles.get(this.selectedProfile);
    }

    public void setSelectedProfile(final String selectedProfile) {
        final boolean update = !this.selectedProfile.equals(selectedProfile);
        this.selectedProfile = selectedProfile;
        if (update) {
            this.fireRefreshEvent();
        }
    }

    public Map<String, Profile> getProfiles() {
        return this.profiles;
    }

    public void addRefreshedProfilesListener(final RefreshedProfilesListener listener) {
        this.refreshedProfilesListeners.add(listener);
    }

    public void addUserChangedListener(final UserChangedListener listener) {
        this.userChangedListeners.add(listener);
    }

    public String getSelectedUser() {
        return this.selectedUser;
    }

    public void setSelectedUser(final String selectedUser) {
        final boolean update = !Objects.equal(this.selectedUser, selectedUser);
        if (update) {
            this.selectedUser = selectedUser;
            this.fireUserChangedEvent();
        }
    }

    public AuthenticationDatabase getAuthDatabase() {
        return this.authDatabase;
    }

    private static class RawProfileList {
        public final String selectedProfile;
        public final String selectedUser;
        public final AuthenticationDatabase authenticationDatabase;
        public Map<String, Profile> profiles;
        public UUID clientToken;

        private RawProfileList(final Map<String, Profile> profiles, final String selectedProfile, final String selectedUser, final UUID clientToken, final AuthenticationDatabase authenticationDatabase) {
            this.profiles = new HashMap<>();
            this.clientToken = UUID.randomUUID();
            this.profiles = profiles;
            this.selectedProfile = selectedProfile;
            this.selectedUser = selectedUser;
            this.clientToken = clientToken;
            this.authenticationDatabase = authenticationDatabase;
        }

        public static class Serializer implements JsonDeserializer<RawProfileList>, JsonSerializer<RawProfileList> {
            private final Launcher launcher;

            public Serializer(final Launcher launcher) {
                this.launcher = launcher;
            }

            @Override
            public RawProfileList deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) throws JsonParseException {
                final JsonObject object = (JsonObject) json;
                Map<String, Profile> profiles = Maps.newHashMap();
                if (object.has("profiles")) {
                    profiles = context.deserialize(object.get("profiles"), new TypeToken<Map<String, Profile>>() {
                    }.getType());
                }
                String selectedProfile = null;
                if (object.has("selectedProfile")) {
                    selectedProfile = object.getAsJsonPrimitive("selectedProfile").getAsString();
                }
                UUID clientToken = UUID.randomUUID();
                if (object.has("clientToken")) {
                    clientToken = context.deserialize(object.get("clientToken"), UUID.class);
                }
                AuthenticationDatabase database = new AuthenticationDatabase(new YggdrasilAuthenticationService(this.launcher.getLauncher().getProxy(), this.launcher.getClientToken().toString()));
                if (object.has("authenticationDatabase")) {
                    database = context.deserialize(object.get("authenticationDatabase"), AuthenticationDatabase.class);
                }
                String selectedUser = null;
                if (object.has("selectedUser")) {
                    selectedUser = object.getAsJsonPrimitive("selectedUser").getAsString();
                } else if (!database.getknownUUIDs().isEmpty()) {
                    selectedUser = database.getknownUUIDs().iterator().next();
                }
                return new RawProfileList(profiles, selectedProfile, selectedUser, clientToken, database);
            }

            @Override
            public JsonElement serialize(final RawProfileList src, final Type typeOfSrc, final JsonSerializationContext context) {
                final JsonObject version = new JsonObject();
                version.addProperty("name", LauncherConstants.getVersionName());
                version.addProperty("format", FORMAT_VERSION);
                version.addProperty("profilesFormat", FORMAT_PROFILES);
                final JsonObject object = new JsonObject();
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
