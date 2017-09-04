package net.minecraft.launcher;

import com.mojang.launcher.UserInterface;
import com.mojang.launcher.events.GameOutputLogProcessor;
import net.minecraft.launcher.game.MinecraftGameRunner;

public abstract interface MinecraftUserInterface
        extends UserInterface {
    public abstract void showOutdatedNotice();

    public abstract String getTitle();

    public abstract GameOutputLogProcessor showGameOutputTab(MinecraftGameRunner paramMinecraftGameRunner);
}