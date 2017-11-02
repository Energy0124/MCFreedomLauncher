package com.mojang.launcher.updater;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.mojang.launcher.versions.ReleaseType;
import com.mojang.launcher.versions.ReleaseTypeFactory;

import java.util.Collections;
import java.util.Set;

public class VersionFilter<T extends ReleaseType> {
    private final Set<T> types;
    private int maxCount;

    public VersionFilter(final ReleaseTypeFactory<T> factory) {
        this.types = Sets.newHashSet();
        this.maxCount = 5;
        Iterables.addAll(this.types, factory);
    }

    public Set<T> getTypes() {
        return this.types;
    }

    @SafeVarargs
    public final void onlyForTypes(final T... types) {
        this.types.clear();
        this.includeTypes(types);
    }

    @SafeVarargs
    final void includeTypes(final T... types) {
        if (types != null) {
            Collections.addAll(this.types, types);
        }
    }

    @SafeVarargs
    public final VersionFilter<T> excludeTypes(final T... types) {
        if (types != null) {
            for (final T type : types) {
                this.types.remove(type);
            }
        }
        return this;
    }

    public int getMaxCount() {
        return this.maxCount;
    }

    public VersionFilter<T> setMaxCount(final int maxCount) {
        this.maxCount = maxCount;
        return this;
    }
}
