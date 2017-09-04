package com.mojang.launcher.versions;

public abstract interface ReleaseTypeFactory<T extends ReleaseType>
        extends Iterable<T> {
    public abstract T getTypeByName(String paramString);

    public abstract T[] getAllTypes();

    public abstract Class<T> getTypeClass();
}