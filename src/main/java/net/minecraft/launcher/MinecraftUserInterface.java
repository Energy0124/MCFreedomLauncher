package net.minecraft.launcher;

import com.mojang.launcher.UserInterface;
import com.mojang.launcher.events.GameOutputLogProcessor;
import net.minecraft.launcher.game.MinecraftGameRunner;

public interface MinecraftUserInterface extends UserInterface {
    void showOutdatedNotice();

    String getTitle();

    GameOutputLogProcessor showGameOutputTab(final MinecraftGameRunner p0);

    boolean shouldDowngradeProfiles();
}
