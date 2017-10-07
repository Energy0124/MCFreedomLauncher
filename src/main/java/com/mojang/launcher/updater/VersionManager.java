package com.mojang.launcher.updater;

import com.mojang.launcher.events.RefreshedVersionsListener;
import com.mojang.launcher.updater.download.DownloadJob;
import com.mojang.launcher.versions.CompleteVersion;
import com.mojang.launcher.versions.ReleaseType;
import com.mojang.launcher.versions.Version;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

public interface VersionManager {
    void refreshVersions() throws IOException;

    List<VersionSyncInfo> getVersions();

    List<VersionSyncInfo> getVersions(final VersionFilter<? extends ReleaseType> p0);

    VersionSyncInfo getVersionSyncInfo(final Version p0);

    VersionSyncInfo getVersionSyncInfo(final String p0);

    VersionSyncInfo getVersionSyncInfo(final Version p0, final Version p1);

    List<VersionSyncInfo> getInstalledVersions();

    CompleteVersion getLatestCompleteVersion(final VersionSyncInfo p0) throws IOException;

    void downloadVersion(final VersionSyncInfo p0, final DownloadJob p1) throws IOException;

    void downloadResources(final DownloadJob p0, final CompleteVersion p1);

    ThreadPoolExecutor getExecutorService();

    void addRefreshedVersionsListener(final RefreshedVersionsListener p0);

    void removeRefreshedVersionsListener(final RefreshedVersionsListener p0);

    VersionSyncInfo syncVersion(final VersionSyncInfo p0) throws IOException;

    void installVersion(final CompleteVersion p0) throws IOException;

    void uninstallVersion(final CompleteVersion p0);
}
