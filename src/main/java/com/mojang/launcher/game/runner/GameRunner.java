package com.mojang.launcher.game.runner;

import com.mojang.launcher.game.GameInstanceStatus;
import com.mojang.launcher.updater.VersionSyncInfo;
import com.mojang.launcher.updater.download.DownloadJob;

public interface GameRunner {
    GameInstanceStatus getStatus();

    void playGame(final VersionSyncInfo p0);

    boolean hasRemainingJobs();

    void addJob(final DownloadJob p0);
}
