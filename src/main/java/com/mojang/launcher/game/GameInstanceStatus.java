package com.mojang.launcher.game;

public enum GameInstanceStatus {
    PREPARING("Preparing..."),
    DOWNLOADING("Downloading..."),
    INSTALLING("Installing..."),
    LAUNCHING("Launching..."),
    PLAYING("Playing..."),
    IDLE("Idle");

    private final String name;

    GameInstanceStatus(final String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
