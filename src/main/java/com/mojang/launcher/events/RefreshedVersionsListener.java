package com.mojang.launcher.events;

import com.mojang.launcher.updater.VersionManager;

public interface RefreshedVersionsListener {
    void onVersionsRefreshed(final VersionManager p0);
}
