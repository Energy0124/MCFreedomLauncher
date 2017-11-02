package net.minecraft.launcher.updater;

import com.google.common.collect.Maps;
import com.mojang.launcher.Http;
import com.mojang.launcher.OperatingSystem;
import com.mojang.launcher.versions.CompleteVersion;
import com.mojang.launcher.versions.Version;
import net.minecraft.launcher.game.MinecraftReleaseType;

import java.io.IOException;
import java.net.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RemoteVersionList extends VersionList {
    private final URL manifestUrl;
    private final Proxy proxy;

    public RemoteVersionList(final URL manifestUrl, final Proxy proxy) {
        this.manifestUrl = manifestUrl;
        this.proxy = proxy;
    }

    @Override
    public CompleteMinecraftVersion getCompleteVersion(final Version version) throws IOException {
        if (version instanceof CompleteVersion) {
            return (CompleteMinecraftVersion) version;
        }
        if (!(version instanceof PartialVersion)) {
            throw new IllegalArgumentException("Version must be a partial");
        }
        final PartialVersion partial = (PartialVersion) version;
        final CompleteMinecraftVersion complete = this.gson.fromJson(Http.performGet(partial.getUrl(), this.proxy), CompleteMinecraftVersion.class);
        this.replacePartialWithFull(partial, complete);
        return complete;
    }

    @Override
    public void refreshVersions() throws IOException {
        this.clearCache();
        final RawVersionList versionList = this.gson.fromJson(this.getContent(this.manifestUrl), RawVersionList.class);
        for (final Version version : versionList.getVersions()) {
            this.versions.add(version);
            this.versionsByName.put(version.getId(), version);
        }
        for (final MinecraftReleaseType type : MinecraftReleaseType.values()) {
            this.latestVersions.put(type, this.versionsByName.get(versionList.getLatestVersions().get(type)));
        }
    }

    @Override
    public boolean hasAllFiles(final CompleteMinecraftVersion version, final OperatingSystem os) {
        return true;
    }

    private String getContent(final URL url) throws IOException {
        return Http.performGet(url, this.proxy);
    }

    public Proxy getProxy() {
        return this.proxy;
    }

    private static class RawVersionList {
        private final List<PartialVersion> versions;
        private final Map<MinecraftReleaseType, String> latest;

        private RawVersionList() {
            this.versions = new ArrayList<>();
            this.latest = Maps.newEnumMap(MinecraftReleaseType.class);
        }

        List<PartialVersion> getVersions() {
            return this.versions;
        }

        Map<MinecraftReleaseType, String> getLatestVersions() {
            return this.latest;
        }
    }
}
