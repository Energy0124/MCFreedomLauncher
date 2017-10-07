package net.minecraft.launcher;

import io.github.lightwayup.minecraftfreedomlauncher.checker.RequirementsChecker;
import io.github.lightwayup.minecraftfreedomlauncher.utility.WorkingDirectory;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.List;

import static net.minecraft.launcher.LauncherConstants.IMAGE_FAVICON;
import static net.minecraft.launcher.LauncherConstants.MINECRAFT_FREEDOM_LAUNCHER_WINDOW_TITLE;

public class Main {
    private static final Logger LOGGER = LogManager.getLogger();

    public static void startLauncher(boolean skipUpdate, String[] args) {
        RequirementsChecker.checkRequirements();
        final OptionParser parser = new OptionParser();
        parser.allowsUnrecognizedOptions();
        parser.accepts("winTen");
        final OptionSpec<String> proxyHostOption = parser.accepts("proxyHost").withRequiredArg();
        final OptionSpec<Integer> proxyPortOption = parser.accepts("proxyPort").withRequiredArg().defaultsTo("8080", new String[0]).ofType(Integer.class);
        final OptionSpec<File> workDirOption = parser.accepts("workDir").withRequiredArg().ofType(File.class).defaultsTo(WorkingDirectory.getWorkingDirectory());
        final OptionSpec<String> nonOption = parser.nonOptions();
        final OptionSet optionSet = parser.parse(args);
        final List<String> leftoverArgs = optionSet.valuesOf(nonOption);
        final String hostName = optionSet.valueOf(proxyHostOption);
        Proxy proxy = Proxy.NO_PROXY;
        if (hostName != null) {
            try {
                proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(hostName, optionSet.valueOf(proxyPortOption)));
            } catch (Exception ex) {
                LOGGER.debug("An Exception is caught!");
            }
        }
        final File workingDirectory = optionSet.valueOf(workDirOption);
        workingDirectory.mkdirs();
        Main.LOGGER.debug("About to create JFrame.");
        final Proxy finalProxy = proxy;
        final JFrame frame = new JFrame();
        frame.setTitle(MINECRAFT_FREEDOM_LAUNCHER_WINDOW_TITLE);
        frame.setPreferredSize(new Dimension(1280, 720));
        try {
            final InputStream in = Launcher.class.getResourceAsStream(IMAGE_FAVICON);
            if (in != null) {
                frame.setIconImage(ImageIO.read(in));
            }
        } catch (IOException ex2) {
            LOGGER.debug("An IOException is caught!");
        }
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        if (optionSet.has("winTen")) {
            System.setProperty("os.name", "Windows 10");
            System.setProperty("os.version", "10.0");
        }
        Main.LOGGER.debug("Starting up launcher.");
        final Launcher launcher = new Launcher(frame, workingDirectory, finalProxy, null, leftoverArgs.toArray(new String[leftoverArgs.size()]), 100);
        if (optionSet.has("winTen")) {
            launcher.setWinTenHack();
        }
        frame.setLocationRelativeTo(null);
        Main.LOGGER.debug("End of main.");
    }

}
