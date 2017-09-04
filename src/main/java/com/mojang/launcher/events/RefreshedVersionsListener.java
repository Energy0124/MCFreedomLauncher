package com.mojang.launcher.events;

import com.mojang.launcher.updater.VersionManager;

public abstract interface RefreshedVersionsListener {
    public abstract void onVersionsRefreshed(VersionManager paramVersionManager);
}