package org.hopto.energy;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class Main {
    private static final Logger LOGGER = LogManager.getLogger();

    public static void main(final String[] args) {
        LOGGER.debug("main() called!");
        net.minecraft.launcher.Main.startLauncher(false, args);
    }
}