package net.minecraft.launcher.game;

import com.google.common.collect.Iterators;
import com.mojang.launcher.versions.ReleaseTypeFactory;

import java.util.Iterator;

public class MinecraftReleaseTypeFactory implements ReleaseTypeFactory<MinecraftReleaseType> {
    private static final MinecraftReleaseTypeFactory FACTORY = new MinecraftReleaseTypeFactory();

    public static MinecraftReleaseTypeFactory instance() {
        return MinecraftReleaseTypeFactory.FACTORY;
    }

    @Override
    public MinecraftReleaseType getTypeByName(final String name) {
        return MinecraftReleaseType.getByName(name);
    }

    @Override
    public MinecraftReleaseType[] getAllTypes() {
        return MinecraftReleaseType.values();
    }

    @Override
    public Class<MinecraftReleaseType> getTypeClass() {
        return MinecraftReleaseType.class;
    }

    @Override
    public Iterator<MinecraftReleaseType> iterator() {
        return Iterators.forArray(MinecraftReleaseType.values());
    }
}
