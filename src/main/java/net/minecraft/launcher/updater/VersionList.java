package net.minecraft.launcher.updater;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.launcher.OperatingSystem;
import com.mojang.launcher.updater.DateTypeAdapter;
import com.mojang.launcher.updater.LowerCaseEnumTypeAdapterFactory;
import com.mojang.launcher.versions.CompleteVersion;
import com.mojang.launcher.versions.ReleaseType;
import com.mojang.launcher.versions.ReleaseTypeAdapterFactory;
import com.mojang.launcher.versions.Version;
import net.minecraft.launcher.game.MinecraftReleaseType;
import net.minecraft.launcher.game.MinecraftReleaseTypeFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

public abstract class VersionList {
    protected final Gson gson;
    private final Map<String, Version> versionsByName = new HashMap();
    private final List<Version> versions = new ArrayList();
    private final Map<MinecraftReleaseType, Version> latestVersions = Maps.newEnumMap(MinecraftReleaseType.class);

    public VersionList() {
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapterFactory(new LowerCaseEnumTypeAdapterFactory());
        builder.registerTypeAdapter(Date.class, new DateTypeAdapter());
        builder.registerTypeAdapter(ReleaseType.class, new ReleaseTypeAdapterFactory(MinecraftReleaseTypeFactory.instance()));
        builder.enableComplexMapKeySerialization();
        builder.setPrettyPrinting();

        this.gson = builder.create();
    }

    public Collection<Version> getVersions() {
        return this.versions;
    }

    public Version getLatestVersion(MinecraftReleaseType type) {
        if (type == null) {
            throw new IllegalArgumentException("Type cannot be null");
        }
        return (Version) this.latestVersions.get(type);
    }

    public Version getVersion(String name) {
        if ((name == null) || (name.length() == 0)) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }
        return (Version) this.versionsByName.get(name);
    }

    public CompleteMinecraftVersion getCompleteVersion(String name)
            throws IOException {
        if ((name == null) || (name.length() == 0)) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }
        Version version = getVersion(name);
        if (version == null) {
            throw new IllegalArgumentException("Unknown version - cannot get complete version of null");
        }
        return getCompleteVersion(version);
    }

    public CompleteMinecraftVersion getCompleteVersion(Version version)
            throws IOException {
        if ((version instanceof CompleteVersion)) {
            return (CompleteMinecraftVersion) version;
        }
        if (version == null) {
            throw new IllegalArgumentException("Version cannot be null");
        }
        CompleteMinecraftVersion complete = (CompleteMinecraftVersion) this.gson.fromJson(getContent("versions/" + version.getId() + "/" + version.getId() + ".json"), CompleteMinecraftVersion.class);
        MinecraftReleaseType type = (MinecraftReleaseType) version.getType();

        Collections.replaceAll(this.versions, version, complete);
        this.versionsByName.put(version.getId(), complete);
        if (this.latestVersions.get(type) == version) {
            this.latestVersions.put(type, complete);
        }
        return complete;
    }

    protected void clearCache() {
        this.versionsByName.clear();
        this.versions.clear();
        this.latestVersions.clear();
    }

    public void refreshVersions()
            throws IOException {
        clearCache();

        RawVersionList versionList = (RawVersionList) this.gson.fromJson(getContent("versions/versions.json"), RawVersionList.class);
        for (Version version : versionList.getVersions()) {
            this.versions.add(version);
            this.versionsByName.put(version.getId(), version);
        }
        for (MinecraftReleaseType type : MinecraftReleaseType.values()) {
            this.latestVersions.put(type, this.versionsByName.get(versionList.getLatestVersions().get(type)));
        }
    }

    public CompleteVersion addVersion(CompleteVersion version) {
        if (version.getId() == null) {
            throw new IllegalArgumentException("Cannot add blank version");
        }
        if (getVersion(version.getId()) != null) {
            throw new IllegalArgumentException("Version '" + version.getId() + "' is already tracked");
        }
        this.versions.add(version);
        this.versionsByName.put(version.getId(), version);

        return version;
    }

    public void removeVersion(String name) {
        if ((name == null) || (name.length() == 0)) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }
        Version version = getVersion(name);
        if (version == null) {
            throw new IllegalArgumentException("Unknown version - cannot remove null");
        }
        removeVersion(version);
    }

    public void removeVersion(Version version) {
        if (version == null) {
            throw new IllegalArgumentException("Cannot remove null version");
        }
        this.versions.remove(version);
        this.versionsByName.remove(version.getId());
        for (MinecraftReleaseType type : MinecraftReleaseType.values()) {
            if (getLatestVersion(type) == version) {
                this.latestVersions.remove(type);
            }
        }
    }

    public void setLatestVersion(Version version) {
        if (version == null) {
            throw new IllegalArgumentException("Cannot set latest version to null");
        }
        this.latestVersions.put((MinecraftReleaseType) version.getType(), version);
    }

    public void setLatestVersion(String name) {
        if ((name == null) || (name.length() == 0)) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }
        Version version = getVersion(name);
        if (version == null) {
            throw new IllegalArgumentException("Unknown version - cannot set latest version to null");
        }
        setLatestVersion(version);
    }

    public String serializeVersionList() {
        RawVersionList list = new RawVersionList();
        for (MinecraftReleaseType type : MinecraftReleaseType.values()) {
            Version latest = getLatestVersion(type);
            if (latest != null) {
                list.getLatestVersions().put(type, latest.getId());
            }
        }
        for (Version version : getVersions()) {
            PartialVersion partial = null;
            if ((version instanceof PartialVersion)) {
                partial = (PartialVersion) version;
            } else {
                partial = new PartialVersion(version);
            }
            list.getVersions().add(partial);
        }
        return this.gson.toJson(list);
    }

    public String serializeVersion(CompleteVersion version) {
        if (version == null) {
            throw new IllegalArgumentException("Cannot serialize null!");
        }
        return this.gson.toJson(version);
    }

    public abstract boolean hasAllFiles(CompleteMinecraftVersion paramCompleteMinecraftVersion, OperatingSystem paramOperatingSystem);

    public abstract String getContent(String paramString)
            throws IOException;

    public abstract URL getUrl(String paramString)
            throws MalformedURLException;

    public void uninstallVersion(Version version) {
        removeVersion(version);
    }

    private static class RawVersionList {
        private List<PartialVersion> versions = new ArrayList();
        private Map<MinecraftReleaseType, String> latest = Maps.newEnumMap(MinecraftReleaseType.class);

        public List<PartialVersion> getVersions() {
            return this.versions;
        }

        public Map<MinecraftReleaseType, String> getLatestVersions() {
            return this.latest;
        }
    }
}