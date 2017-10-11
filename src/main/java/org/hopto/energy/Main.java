package org.hopto.energy;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Main {
    private static final Logger LOGGER = LogManager.getLogger();

    public static void main(final String[] args) {
        LOGGER.debug("main() called!");
        if (args.length != 0) {
            LOGGER.info("Launching with additional arguments, hope nothing breaks.");
        }
        net.minecraft.launcher.Main.startLauncher(args);
    }
}