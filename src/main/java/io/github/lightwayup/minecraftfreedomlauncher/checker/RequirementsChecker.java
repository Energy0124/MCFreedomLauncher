package io.github.lightwayup.minecraftfreedomlauncher.checker;

import com.mojang.launcher.OperatingSystem;
import io.github.lightwayup.minecraftfreedomlauncher.userinterface.DialogDisplay;
import io.github.lightwayup.minecraftfreedomlauncher.utility.LauncherShutdown;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.net.URL;

import static net.minecraft.launcher.LauncherConstants.*;

public class RequirementsChecker {
    private static final Logger LOGGER = LogManager.getLogger();

    public static void checkRequirements() {
        try {
            if (!isSupportedJava()) {
                LOGGER.fatal("Java version check failed! Quitting...");
                DialogDisplay.showError(MESSAGE_SUPPORTED_JAVA_9_UNAVAILABLE);
                LauncherShutdown.forcefullyShutdown("Java version check did not pass");
            }
        } catch (NullPointerException | SecurityException | IllegalArgumentException exception) {
            DialogDisplay.showError(MESSAGE_UNKNOWN_ERROR + "\nIf the error persists, trying installing Java again.");
        }
        try {
            if (!isSupportedResolution()) {
                LOGGER.fatal("Resolution check failed! Quitting...");
                DialogDisplay.showError(MESSAGE_RESOLUTION_UNSUPPORTED);
                LauncherShutdown.forcefullyShutdown("resolution check did not pass");
            }
        } catch (HeadlessException headlessException) {
            DialogDisplay.showError(MESSAGE_UNKNOWN_ERROR + "\nIf the error persists, trying installing Java again.");
        }
        if (!resourcesExist()) {
            LOGGER.fatal("Resources check failed! Quitting...");
            DialogDisplay.showError(MESSAGE_LAUNCHER_CORRUPTED);
            LauncherShutdown.forcefullyShutdown("resources check did not pass");
        }
    }

    private static boolean isSupportedJava() throws NullPointerException, SecurityException, IllegalArgumentException {
        return ((Double.parseDouble(System.getProperty("java.specification.version")) == 9) && (System.getProperty("java.specification.vendor").toLowerCase().contains("oracle")));
    }

    private static boolean isSupportedResolution() throws HeadlessException {
        final GraphicsDevice graphicsDevice = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        final int width = graphicsDevice.getDisplayMode().getWidth();
        final int height = graphicsDevice.getDisplayMode().getHeight();
        LOGGER.info("Screen resolution is " + width + "x" + height + ".");
        return ((width > 1365) && (height > 767));
    }

    private static boolean resourcesExist() {
        URL minecraftLogoUrl = RequirementsChecker.class.getResource(IMAGE_MINECRAFT_LOGO);
        URL faviconUrl = RequirementsChecker.class.getResource(IMAGE_FAVICON);
        URL backgroundUrl = RequirementsChecker.class.getResource(IMAGE_LAUNCHER_BACKGROUND);
        if (OperatingSystem.getCurrentPlatform() == OperatingSystem.MACOS) {
            URL macosFaviconUrl = RequirementsChecker.class.getResource(IMAGE_MACOS_FAVICON);
            return (minecraftLogoUrl != null && faviconUrl != null && backgroundUrl != null && macosFaviconUrl != null);
        } else {
            return (minecraftLogoUrl != null && faviconUrl != null && backgroundUrl != null);
        }
    }
}