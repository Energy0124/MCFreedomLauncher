package com.mojang.launcher.versions;

import java.util.Date;

public abstract interface CompleteVersion
        extends Version {
    public abstract String getId();

    public abstract ReleaseType getType();

    public abstract Date getUpdatedTime();

    public abstract Date getReleaseTime();

    public abstract int getMinimumLauncherVersion();

    public abstract boolean appliesToCurrentEnvironment();

    public abstract String getIncompatibilityReason();

    public abstract boolean isSynced();

    public abstract void setSynced(boolean paramBoolean);
}