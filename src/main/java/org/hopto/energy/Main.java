package org.hopto.energy;

import io.github.lightwayup.minecraftfreedomlauncher.JavaVersionTest;
import io.github.lightwayup.minecraftfreedomlauncher.userinterface.StartupFrame;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;

class Main {
    private static final Logger LOGGER = LogManager.getLogger();

    public static void main(final String[] args) {
        JFrame loadingFrame = new StartupFrame();
        if ((args.length == 1) && (args[0].toLowerCase().equals("---jvertest"))) {
            LOGGER.debug("Java version test is run");
            JavaVersionTest.runJavaVersionTest();
        } else {
            LOGGER.info("Starting launcher with " + args.length + " argument(s). Some arguments might not be supported");
            net.minecraft.launcher.Main.startLauncher(args, loadingFrame);
        }
    }
}