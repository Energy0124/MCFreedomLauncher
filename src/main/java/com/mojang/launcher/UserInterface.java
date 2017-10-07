package com.mojang.launcher;

import com.mojang.launcher.updater.DownloadProgress;
import com.mojang.launcher.versions.CompleteVersion;

import java.io.File;

public interface UserInterface {
    void showLoginPrompt();

    void setVisible(final boolean p0);

    void shutdownLauncher();

    void hideDownloadProgress();

    void setDownloadProgress(final DownloadProgress p0);

    void showCrashReport(final CompleteVersion p0, final File p1, final String p2);

    void gameLaunchFailure(final String p0);

    void updatePlayState();
}
