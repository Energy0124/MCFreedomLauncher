package com.mojang.launcher.versions;

import java.util.Date;

public interface CompleteVersion extends Version {
    String getId();

    ReleaseType getType();

    Date getUpdatedTime();

    Date getReleaseTime();

    int getMinimumLauncherVersion();

    boolean appliesToCurrentEnvironment();

    String getIncompatibilityReason();

    boolean isSynced();

    void setSynced(final boolean p0);
}
