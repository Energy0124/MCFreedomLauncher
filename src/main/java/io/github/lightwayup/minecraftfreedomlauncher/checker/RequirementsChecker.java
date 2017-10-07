package io.github.lightwayup.minecraftfreedomlauncher.checker;

import io.github.lightwayup.minecraftfreedomlauncher.utility.LauncherShutdown;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RequirementsChecker {
    private static final Logger LOGGER = LogManager.getLogger();

    public static void checkRequirements() {
        if (!JavaVersionCheck.isSupportedJava9()) {
            LOGGER.fatal("Java version check failed! Quitting...");
            LauncherShutdown.forcefullyShutdown("Java version check did not pass");
        }
        if (!JavaVersionCheck.isSupportedResolution()) {
            LOGGER.fatal("Resolution check failed! Quitting...");
            LauncherShutdown.forcefullyShutdown("resolution check did not pass");
        }
        if (!ResourcesCheck.resourcesCheck()) {
            LOGGER.fatal("Resources check failed! Quitting...");
            LauncherShutdown.forcefullyShutdown("resources check did not pass");
        }
    }
}