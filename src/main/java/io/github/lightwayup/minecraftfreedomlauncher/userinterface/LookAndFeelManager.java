package io.github.lightwayup.minecraftfreedomlauncher.userinterface;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;

public class LookAndFeelManager {
    private static final Logger LOGGER = LogManager.getLogger();

    public static void setLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            LOGGER.error("Your java failed to provide normal and cross-platform look and feel");
        }
    }
}
