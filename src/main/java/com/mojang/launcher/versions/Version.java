package com.mojang.launcher.versions;

import java.util.Date;

public abstract interface Version {
    public abstract String getId();

    public abstract ReleaseType getType();

    public abstract Date getUpdatedTime();

    public abstract Date getReleaseTime();
}