package com.mojang.launcher;

import com.mojang.launcher.updater.DownloadProgress;
import com.mojang.launcher.versions.CompleteVersion;

import java.io.File;

public abstract interface UserInterface {
    public abstract void showLoginPrompt();

    public abstract void setVisible(boolean paramBoolean);

    public abstract void shutdownLauncher();

    public abstract void hideDownloadProgress();

    public abstract void setDownloadProgress(DownloadProgress paramDownloadProgress);

    public abstract void showCrashReport(CompleteVersion paramCompleteVersion, File paramFile, String paramString);

    public abstract void gameLaunchFailure(String paramString);

    public abstract void updatePlayState();
}