package io.github.lightwayup.minecraftfreedomlauncher.utility;

import io.github.lightwayup.minecraftfreedomlauncher.userinterface.IconManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Timer;
import java.util.TimerTask;

public class LauncherShutdown {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final long MAX_SHUTDOWN_TIME = 10000L;

    public static void forcefullyShutdown(String reason) {
        LOGGER.debug("Launcher is shutting down because " + reason + " ...");
        try {
            IconManager.closeIconStream();
        } catch (Exception e) {
            LOGGER.error("Unable to close stream");
        }
        try {
            final Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    Runtime.getRuntime().halt(0);
                }
            }, MAX_SHUTDOWN_TIME);
            System.exit(0);
        } catch (Throwable ignored) {
            Runtime.getRuntime().halt(0);
        }
    }
}