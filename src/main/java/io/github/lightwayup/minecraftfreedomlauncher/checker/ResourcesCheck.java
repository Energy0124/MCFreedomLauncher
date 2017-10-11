package io.github.lightwayup.minecraftfreedomlauncher.checker;

import com.mojang.launcher.OperatingSystem;
import net.minecraft.launcher.LauncherConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.net.URL;

import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static net.minecraft.launcher.LauncherConstants.*;

class ResourcesCheck {
    private static final Logger LOGGER = LogManager.getLogger();

    static boolean resourcesCheck() {
        boolean resourcesExist;
        try {
            URL minecraftLogoUrl = ResourcesCheck.class.getResource(IMAGE_MINECRAFT_LOGO);
            URL faviconUrl = ResourcesCheck.class.getResource(IMAGE_FAVICON);
            URL macosFaviconUrl = ResourcesCheck.class.getResource(IMAGE_MACOS_FAVICON);
            URL backgroundUrl = ResourcesCheck.class.getResource(IMAGE_LAUNCHER_BACKGROUND);
            if (OperatingSystem.getCurrentPlatform() != OperatingSystem.MACOS) {
                if (minecraftLogoUrl == null || faviconUrl == null || backgroundUrl == null) {
                    LOGGER.warn("Images are gone!");
                    try {
                        JOptionPane.showMessageDialog(null, MESSAGE_LAUNCHER_CORRUPTED, MINECRAFT_FREEDOM_LAUNCHER_WINDOW_TITLE, ERROR_MESSAGE, LauncherConstants.getFavicon());
                    } catch (Exception e1) {
                        LOGGER.debug("An Exception is caught!");
                    }
                    resourcesExist = false;
                } else {
                    resourcesExist = true;
                }
            } else {
                if (minecraftLogoUrl == null || faviconUrl == null || backgroundUrl == null || macosFaviconUrl == null) {
                    LOGGER.warn("Images are gone!");
                    try {
                        JOptionPane.showMessageDialog(null, MESSAGE_LAUNCHER_CORRUPTED, MINECRAFT_FREEDOM_LAUNCHER_WINDOW_TITLE, ERROR_MESSAGE, LauncherConstants.getFavicon());
                    } catch (Exception e2) {
                        LOGGER.debug("An Exception is caught!");
                    }
                    resourcesExist = false;
                } else {
                    resourcesExist = true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.error("Can't get image resources!");
            try {
                JOptionPane.showMessageDialog(null, MESSAGE_UNKNOWN_ERROR, MINECRAFT_FREEDOM_LAUNCHER_WINDOW_TITLE, ERROR_MESSAGE, LauncherConstants.getFavicon());
            } catch (Exception e3) {
                LOGGER.debug("An Exception is caught!");
            }
            resourcesExist = false;
        }
        return resourcesExist;
    }
}