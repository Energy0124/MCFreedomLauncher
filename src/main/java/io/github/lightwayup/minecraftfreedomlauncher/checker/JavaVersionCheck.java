package io.github.lightwayup.minecraftfreedomlauncher.checker;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;

import java.awt.*;

import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static net.minecraft.launcher.LauncherConstants.*;

class JavaVersionCheck {
    private static final Logger LOGGER = LogManager.getLogger();

    static boolean isSupportedJava9() {
        try {
            if ((Double.parseDouble(System.getProperty("java.specification.version")) < 9) || !(System.getProperty("java.specification.vendor").toLowerCase().contains("oracle"))) {
                LOGGER.fatal("The Java version is not supported.");
                try {
                    JOptionPane.showMessageDialog(null, MESSAGE_SUPPORTED_JAVA_9_UNAVAILABLE, MINECRAFT_FREEDOM_LAUNCHER_WINDOW_TITLE, ERROR_MESSAGE, new ImageIcon(IMAGE_FAVICON));
                } catch (Exception e) {
                    LOGGER.debug("An Exception is caught!");
                }
                return false;
            } else {
                return true;
            }
        } catch (NullPointerException | SecurityException | IllegalArgumentException e) {
            LOGGER.error("Unable to verify Java compatibility.");
            try {
                JOptionPane.showMessageDialog(null, MESSAGE_UNKNOWN_ERROR + "\nIf the error persists, trying installing Java again.", MINECRAFT_FREEDOM_LAUNCHER_WINDOW_TITLE, ERROR_MESSAGE, new ImageIcon(IMAGE_FAVICON));
            } catch (Exception e1) {
                LOGGER.debug("An Exception is caught!");
            }
            return false;
        }
    }

    static boolean isSupportedResolution() {
        try {
            final GraphicsDevice graphicsDevice = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
            final int width = graphicsDevice.getDisplayMode().getWidth();
            final int height = graphicsDevice.getDisplayMode().getHeight();
            if ((width < 1365) || (height < 767)) {
                LOGGER.fatal("Screen resolution " + width + "*" + height + " is not supported.");
                try {
                    JOptionPane.showMessageDialog(null, MESSAGE_RESOLUTION_UNSUPPORTED + "\nIf the error persists, trying installing Java again.", MINECRAFT_FREEDOM_LAUNCHER_WINDOW_TITLE, ERROR_MESSAGE, new ImageIcon(IMAGE_FAVICON));
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
                JOptionPane.showMessageDialog(null, MESSAGE_UNKNOWN_ERROR + "\nIf the error persists, trying installing Java again.", MINECRAFT_FREEDOM_LAUNCHER_WINDOW_TITLE, ERROR_MESSAGE, new ImageIcon(IMAGE_FAVICON));
            } catch (Exception e4) {
                LOGGER.debug("An Exception is caught!");
            }
            return false;
        }
    }
}