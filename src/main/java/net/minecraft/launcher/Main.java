package net.minecraft.launcher;

import io.github.lightwayup.minecraftfreedomlauncher.checker.RequirementsChecker;
import io.github.lightwayup.minecraftfreedomlauncher.userinterface.LookAndFeelManager;
import io.github.lightwayup.minecraftfreedomlauncher.userinterface.StartupFrame;
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

public class Main {
    private static final Logger LOGGER = LogManager.getLogger();

    public static void startLauncher(final String[] args, final JFrame loadingFrame) {
        LookAndFeelManager.setLookAndFeel();
        RequirementsChecker.checkRequirements(loadingFrame);
        final OptionParser parser = new OptionParser();
        parser.allowsUnrecognizedOptions();
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
        boolean success = workingDirectory.mkdirs();
        if (!success) {
            LOGGER.error("Unable to create directories");
        }
        Main.LOGGER.debug("About to create JFrame.");
        final Proxy finalProxy = proxy;
        final JFrame frame = new JFrame(LauncherConstants.getTitle());
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
        StartupFrame.hide(loadingFrame);
        frame.setVisible(true);
        Main.LOGGER.debug("Starting up launcher.");
        final Launcher launcher = new Launcher(frame, workingDirectory, finalProxy, null, leftoverArgs.toArray(new String[leftoverArgs.size()]), LauncherConstants.VERSION_SUPER_COOL_BOOTSTRAP);
    }

}
