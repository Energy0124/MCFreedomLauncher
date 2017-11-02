package com.mojang.authlib;

public class Agent {
    public static final Agent MINECRAFT = new Agent();

    private final String name = "Minecraft";
    private final int version = 1;

    public String getName() {
        return this.name;
    }

    public int getVersion() {
        return this.version;
    }

    @Override
    public String toString() {
        return "Agent{name='" + this.name + '\'' + ", version=" + this.version + '}';
    }
}
