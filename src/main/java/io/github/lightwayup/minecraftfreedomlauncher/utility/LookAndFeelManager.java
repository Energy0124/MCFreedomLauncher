package io.github.lightwayup.minecraftfreedomlauncher.utility;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;

public class LookAndFeelManager {
    private static final Logger LOGGER = LogManager.getLogger();

    public static void setLookAndFeel(Component[] components) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            if (components != null) {
                for (Component component : components) {
                    SwingUtilities.updateComponentTreeUI(component);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Your java failed to provide normal and cross-platform look and feel");
        }
    }
}
