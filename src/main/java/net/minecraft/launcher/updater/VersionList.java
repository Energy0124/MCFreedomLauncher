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
import java.util.*;

public abstract class VersionList {
    protected final Gson gson;
    protected final Map<String, Version> versionsByName;
    protected final List<Version> versions;
    protected final Map<MinecraftReleaseType, Version> latestVersions;

    public VersionList() {
        this.versionsByName = new HashMap<>();
        this.versions = new ArrayList<>();
        this.latestVersions = Maps.newEnumMap(MinecraftReleaseType.class);
        final GsonBuilder builder = new GsonBuilder();
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

    public Version getLatestVersion(final MinecraftReleaseType type) {
        if (type == null) {
            throw new IllegalArgumentException("Type cannot be null");
        }
        return this.latestVersions.get(type);
    }

    public Version getVersion(final String name) {
        if (name == null || name.length() == 0) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }
        return this.versionsByName.get(name);
    }

    public abstract CompleteMinecraftVersion getCompleteVersion(final Version p0) throws IOException;

    protected void replacePartialWithFull(final PartialVersion version, final CompleteVersion complete) {
        Collections.replaceAll(this.versions, version, complete);
        this.versionsByName.put(version.getId(), complete);
        if (this.latestVersions.get(version.getType()) == version) {
            this.latestVersions.put(version.getType(), complete);
        }
    }

    protected void clearCache() {
        this.versionsByName.clear();
        this.versions.clear();
        this.latestVersions.clear();
    }

    public abstract void refreshVersions() throws IOException;

    public void addVersion(final CompleteVersion version) {
        if (version.getId() == null) {
            throw new IllegalArgumentException("Cannot add blank version");
        }
        if (this.getVersion(version.getId()) != null) {
            throw new IllegalArgumentException("Version '" + version.getId() + "' is already tracked");
        }
        this.versions.add(version);
        this.versionsByName.put(version.getId(), version);
    }

    public void removeVersion(final String name) {
        if (name == null || name.length() == 0) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }
        final Version version = this.getVersion(name);
        if (version == null) {
            throw new IllegalArgumentException("Unknown version - cannot remove null");
        }
        this.removeVersion(version);
    }

    public void removeVersion(final Version version) {
        if (version == null) {
            throw new IllegalArgumentException("Cannot remove null version");
        }
        this.versions.remove(version);
        this.versionsByName.remove(version.getId());
        for (final MinecraftReleaseType type : MinecraftReleaseType.values()) {
            if (this.getLatestVersion(type) == version) {
                this.latestVersions.remove(type);
            }
        }
    }

    public void setLatestVersion(final Version version) {
        if (version == null) {
            throw new IllegalArgumentException("Cannot set latest version to null");
        }
        this.latestVersions.put((MinecraftReleaseType) version.getType(), version);
    }

    public void setLatestVersion(final String name) {
        if (name == null || name.length() == 0) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }
        final Version version = this.getVersion(name);
        if (version == null) {
            throw new IllegalArgumentException("Unknown version - cannot set latest version to null");
        }
        this.setLatestVersion(version);
    }

    public String serializeVersion(final CompleteVersion version) {
        if (version == null) {
            throw new IllegalArgumentException("Cannot serialize null!");
        }
        return this.gson.toJson(version);
    }

    public abstract boolean hasAllFiles(final CompleteMinecraftVersion p0, final OperatingSystem p1);

    public void uninstallVersion(final Version version) {
        this.removeVersion(version);
    }
}
