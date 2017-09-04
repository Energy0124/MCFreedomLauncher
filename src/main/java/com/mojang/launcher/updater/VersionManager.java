package com.mojang.launcher.updater;

import com.mojang.launcher.events.RefreshedVersionsListener;
import com.mojang.launcher.updater.download.DownloadJob;
import com.mojang.launcher.versions.CompleteVersion;
import com.mojang.launcher.versions.ReleaseType;
import com.mojang.launcher.versions.Version;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

public abstract interface VersionManager {
    public abstract void refreshVersions()
            throws IOException;

    public abstract List<VersionSyncInfo> getVersions();

    public abstract List<VersionSyncInfo> getVersions(VersionFilter<? extends ReleaseType> paramVersionFilter);

    public abstract VersionSyncInfo getVersionSyncInfo(Version paramVersion);

    public abstract VersionSyncInfo getVersionSyncInfo(String paramString);

    public abstract VersionSyncInfo getVersionSyncInfo(Version paramVersion1, Version paramVersion2);

    public abstract List<VersionSyncInfo> getInstalledVersions();

    public abstract CompleteVersion getLatestCompleteVersion(VersionSyncInfo paramVersionSyncInfo)
            throws IOException;

    public abstract DownloadJob downloadVersion(VersionSyncInfo paramVersionSyncInfo, DownloadJob paramDownloadJob)
            throws IOException;

    public abstract DownloadJob downloadResources(DownloadJob paramDownloadJob, CompleteVersion paramCompleteVersion)
            throws IOException;

    public abstract ThreadPoolExecutor getExecutorService();

    public abstract void addRefreshedVersionsListener(RefreshedVersionsListener paramRefreshedVersionsListener);

    public abstract void removeRefreshedVersionsListener(RefreshedVersionsListener paramRefreshedVersionsListener);

    public abstract VersionSyncInfo syncVersion(VersionSyncInfo paramVersionSyncInfo)
            throws IOException;

    public abstract void installVersion(CompleteVersion paramCompleteVersion)
            throws IOException;

    public abstract void uninstallVersion(CompleteVersion paramCompleteVersion)
            throws IOException;
}