package com.mojang.launcher.game.runner;

import com.mojang.launcher.game.GameInstanceStatus;
import com.mojang.launcher.updater.VersionSyncInfo;
import com.mojang.launcher.updater.download.DownloadJob;

public abstract interface GameRunner {
    public abstract GameInstanceStatus getStatus();

    public abstract void playGame(VersionSyncInfo paramVersionSyncInfo);

    public abstract boolean hasRemainingJobs();

    public abstract void addJob(DownloadJob paramDownloadJob);
}