package io.github.lightwayup.minecraftfreedomlauncher.checker;

import com.mojang.launcher.OperatingSystem;
import io.github.lightwayup.minecraftfreedomlauncher.utility.IconManager;
import io.github.lightwayup.minecraftfreedomlauncher.utility.LauncherShutdown;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.net.URL;

import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static net.minecraft.launcher.LauncherConstants.*;

public class RequirementsChecker {
    private static final Logger LOGGER = LogManager.getLogger();

    public static void checkRequirements() {
        if (!isSupportedJava()) {
            LOGGER.fatal("Java version check failed! Quitting...");
            LauncherShutdown.forcefullyShutdown("Java version check did not pass");
        }
        if (!isSupportedResolution()) {
            LOGGER.fatal("Resolution check failed! Quitting...");
            LauncherShutdown.forcefullyShutdown("resolution check did not pass");
        }
        if (!resourcesCheck()) {
            LOGGER.fatal("Resources check failed! Quitting...");
            LauncherShutdown.forcefullyShutdown("resources check did not pass");
        }
    }

    private static boolean isSupportedJava() {
        try {
            if ((Double.parseDouble(System.getProperty("java.specification.version")) < 9) || !(System.getProperty("java.specification.vendor").toLowerCase().contains("oracle"))) {
                LOGGER.fatal("The Java version is not supported.");
                try {
                    JOptionPane.showMessageDialog(null, MESSAGE_SUPPORTED_JAVA_9_UNAVAILABLE, MINECRAFT_FREEDOM_LAUNCHER_WINDOW_TITLE, ERROR_MESSAGE, IconManager.getIcon());
                } catch (Exception e) {
                    LOGGER.debug("An Exception is caught!");
                }
                return false;
            } else {
                if (Double.parseDouble(System.getProperty("java.specification.version")) > 9) {
                    LOGGER.info("Java version newer than 9 is used.");
                    try {
                        JOptionPane.showMessageDialog(null, "You're using a Java version newer than 9. Support isn't guaranteed.", MINECRAFT_FREEDOM_LAUNCHER_WINDOW_TITLE, JOptionPane.INFORMATION_MESSAGE, IconManager.getIcon());
                    } catch (Exception e) {
                        LOGGER.debug("An Exception is caught!");
                    }
                }
                return true;
            }
        } catch (NullPointerException | SecurityException | IllegalArgumentException e) {
            LOGGER.error("Unable to verify Java compatibility.");
            try {
                JOptionPane.showMessageDialog(null, MESSAGE_UNKNOWN_ERROR + "\nIf the error persists, trying installing Java again.", MINECRAFT_FREEDOM_LAUNCHER_WINDOW_TITLE, ERROR_MESSAGE, IconManager.getIcon());
            } catch (Exception e1) {
                LOGGER.debug("An Exception is caught!");
            }
            return false;
        }
    }

    private static boolean isSupportedResolution() {
        try {
            final GraphicsDevice graphicsDevice = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
            final int width = graphicsDevice.getDisplayMode().getWidth();
            final int height = graphicsDevice.getDisplayMode().getHeight();
            if ((width < 1365) || (height < 767)) {
                LOGGER.fatal("Screen resolution " + width + "*" + height + " is not supported.");
                try {
                    JOptionPane.showMessageDialog(null, MESSAGE_RESOLUTION_UNSUPPORTED + "\nIf the error persists, trying installing Java again.", MINECRAFT_FREEDOM_LAUNCHER_WINDOW_TITLE, ERROR_MESSAGE, IconManager.getIcon());
                } catch (Exception e4) {
                    LOGGER.debug("An Exception is caught!");
                }
                return false;
            } else {
                LOGGER.info("Screen resolution is " + width + "*" + height + ".");
                return true;
            }
        } catch (HeadlessException he) {
            LOGGER.error("Unable to get screen resolution.");
            try {
                JOptionPane.showMessageDialog(null, MESSAGE_UNKNOWN_ERROR + "\nIf the error persists, trying installing Java again.", MINECRAFT_FREEDOM_LAUNCHER_WINDOW_TITLE, ERROR_MESSAGE, IconManager.getIcon());
            } catch (Exception e4) {
                LOGGER.debug("An Exception is caught!");
            }
            return false;
        }
    }

    private static boolean resourcesCheck() {
        boolean resourcesExist;
        try {
            URL minecraftLogoUrl = RequirementsChecker.class.getResource(IMAGE_MINECRAFT_LOGO);
            URL faviconUrl = RequirementsChecker.class.getResource(IMAGE_FAVICON);
            URL backgroundUrl = RequirementsChecker.class.getResource(IMAGE_LAUNCHER_BACKGROUND);
            if (OperatingSystem.getCurrentPlatform() != OperatingSystem.MACOS) {
                if (minecraftLogoUrl == null || faviconUrl == null || backgroundUrl == null) {
                    LOGGER.warn("Images are gone!");
                    try {
                        JOptionPane.showMessageDialog(null, MESSAGE_LAUNCHER_CORRUPTED, MINECRAFT_FREEDOM_LAUNCHER_WINDOW_TITLE, ERROR_MESSAGE, IconManager.getIcon());
                    } catch (Exception e1) {
                        LOGGER.debug("An Exception is caught!");
                    }
                    resourcesExist = false;
                } else {
                    resourcesExist = true;
                }
            } else {
                URL macosFaviconUrl = RequirementsChecker.class.getResource(IMAGE_MACOS_FAVICON);
                if (minecraftLogoUrl == null || faviconUrl == null || backgroundUrl == null || macosFaviconUrl == null) {
                    LOGGER.warn("Images are gone!");
                    try {
                        JOptionPane.showMessageDialog(null, MESSAGE_LAUNCHER_CORRUPTED, MINECRAFT_FREEDOM_LAUNCHER_WINDOW_TITLE, ERROR_MESSAGE, IconManager.getIcon());
                    } catch (Exception e2) {
                        LOGGER.debug("An Exception is caught!");
                    }
                    resourcesExist = false;
                } else {
                    resourcesExist = true;
                }
            }
        } catch (Exception e) {
            LOGGER.error("Can't get image resources!");
            try {
                JOptionPane.showMessageDialog(null, MESSAGE_UNKNOWN_ERROR, MINECRAFT_FREEDOM_LAUNCHER_WINDOW_TITLE, ERROR_MESSAGE, IconManager.getIcon());
            } catch (Exception e3) {
                LOGGER.debug("An Exception is caught!");
            }
            resourcesExist = false;
        }
        return resourcesExist;
    }
}