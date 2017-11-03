package net.minecraft.launcher.ui.tabs;

import net.minecraft.launcher.Launcher;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;

class VersionInfoTab extends JPanel {
    private static final Logger LOGGER = LogManager.getLogger();

    private final Launcher minecraftLauncher;

    VersionInfoTab(final Launcher minecraftLauncher) {
        this.minecraftLauncher = minecraftLauncher;
        this.setLayout(new GridBagLayout());
        this.createInterface();
    }

    private void createInterface() {
        ;
    }

    public Launcher getMinecraftLauncher() {
        return this.minecraftLauncher;
    }
}
