package com.mojang.launcher.versions;

public interface ReleaseTypeFactory<T extends ReleaseType> extends Iterable<T> {
    T getTypeByName(final String p0);

    T[] getAllTypes();

    Class<T> getTypeClass();
}
