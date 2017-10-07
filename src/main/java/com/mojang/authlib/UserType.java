package com.mojang.authlib;

import java.util.HashMap;
import java.util.Map;

public enum UserType {
    LEGACY("legacy"),
    MOJANG("mojang");

    private static final Map<String, UserType> BY_NAME = new HashMap<>();

    static {
        for (final UserType type : values()) {
            UserType.BY_NAME.put(type.name, type);
        }
    }

    private final String name;

    UserType(final String name) {
        this.name = name;
    }

    public static UserType byName(final String name) {
        return UserType.BY_NAME.get(name.toLowerCase());
    }

    public String getName() {
        return this.name;
    }
}
