package net.minecraft.launcher.game;

import com.google.common.collect.Maps;
import com.mojang.launcher.versions.ReleaseType;

import java.util.Map;

import static net.minecraft.launcher.LauncherConstants.MESSAGE_VERSIONS_DEVELOPMENT;
import static net.minecraft.launcher.LauncherConstants.MESSAGE_VERSIONS_OLD;

public enum MinecraftReleaseType implements ReleaseType {
    SNAPSHOT("snapshot", "Enable experimental development versions (\"snapshots\")"),
    RELEASE("release", null),
    OLD_BETA("old_beta", "Allow use of old Beta Minecraft versions"),
    OLD_ALPHA("old_alpha", "Allow use of old Alpha Minecraft versions");

    private static final String POPUP_DEV_VERSIONS = MESSAGE_VERSIONS_DEVELOPMENT;
    private static final String POPUP_OLD_VERSIONS = MESSAGE_VERSIONS_OLD;
    private static final Map<String, MinecraftReleaseType> LOOKUP;

    static {
        LOOKUP = Maps.newHashMap();
        for (final MinecraftReleaseType type : values()) {
            MinecraftReleaseType.LOOKUP.put(type.getName(), type);
        }
    }

    private final String name;
    private final String description;

    MinecraftReleaseType(final String name, final String description) {
        this.name = name;
        this.description = description;
    }

    public static MinecraftReleaseType getByName(final String name) {
        return MinecraftReleaseType.LOOKUP.get(name);
    }

    @Override
    public String getName() {
        return this.name;
    }

    public String getDescription() {
        return this.description;
    }

    public String getPopupWarning() {
        if (this.description == null) {
            return null;
        }
        if (this == MinecraftReleaseType.SNAPSHOT) {
            return POPUP_DEV_VERSIONS;
        }
        if (this == MinecraftReleaseType.OLD_BETA) {
            return POPUP_OLD_VERSIONS;
        }
        if (this == MinecraftReleaseType.OLD_ALPHA) {
            return POPUP_OLD_VERSIONS;
        }
        return null;
    }
}
