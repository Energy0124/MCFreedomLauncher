package org.hopto.energy;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

class Main {
    private static final Logger LOGGER = LogManager.getLogger();

    public static void main(String[] args) {
        LOGGER.info("Starting launcher with " + args.length + " argument(s). Some arguments might not be supported");
        net.minecraft.launcher.Main.startLauncher(args);
    }
}