package org.hopto.energy;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import net.minecraft.launcher.Launcher;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class Main {
    private static final Logger LOGGER = LogManager.getLogger();

    public static void main(String[] args)
            throws IOException {

        double version = Double.parseDouble(System.getProperty("java.specification.version"));
        if (version < 1.8) {
            LOGGER.fatal("Java 8 or higher is not available, goodbye");
            JOptionPane.showMessageDialog(null, "Please install Java 8 or higher versions of Java.", "Java version is too old :(", JOptionPane.ERROR_MESSAGE);
            try {
                java.util.Timer ignored = new Timer();
                ignored.schedule(new TimerTask() {
                    public void run() {
                        Runtime.getRuntime().halt(0);
                    }
                }, 10000L);
                System.exit(0);
            } catch (Throwable var2) {
                Runtime.getRuntime().halt(0);
            }
        }
        Launcher.setLookAndFeel();

        System.setProperty("java.net.preferIPv4Stack", "true");

        OptionParser optionParser = new OptionParser();
        optionParser.allowsUnrecognizedOptions();

        optionParser.accepts("help", "Show help").forHelp();
        optionParser.accepts("force", "Force updating");

        OptionSpec proxyHostOption = optionParser.accepts("proxyHost", "Optional").withRequiredArg();
        OptionSpec proxyPortOption = optionParser.accepts("proxyPort", "Optional").withRequiredArg().defaultsTo("8080", new String[0]).ofType(Integer.class);
        OptionSpec proxyUserOption = optionParser.accepts("proxyUser", "Optional").withRequiredArg();
        OptionSpec proxyPassOption = optionParser.accepts("proxyPass", "Optional").withRequiredArg();
        OptionSpec workingDirectoryOption = optionParser.accepts("workdir", "Optional").withRequiredArg().ofType(File.class).defaultsTo(Util.getWorkingDirectory(), new File[0]);
        OptionSpec nonOptions = optionParser.nonOptions();
        OptionSet optionSet;
        try {
            optionSet = optionParser.parse(args);
        } catch (OptionException e) {
            optionParser.printHelpOn(System.out);
            System.out.println("(to pass in arguments to minecraft directly use: '--' followed by your arguments");
            return;
        }

        if (optionSet.has("help")) {
            optionParser.printHelpOn(System.out);
            return;
        }

        String hostName = (String) optionSet.valueOf(proxyHostOption);
        Proxy proxy = Proxy.NO_PROXY;
        if (hostName != null) {
            try {
                proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(hostName, ((Integer) optionSet.valueOf(proxyPortOption)).intValue()));
            } catch (Exception ignored) {
            }
        }
        String proxyUser = (String) optionSet.valueOf(proxyUserOption);
        String proxyPass = (String) optionSet.valueOf(proxyPassOption);
        PasswordAuthentication passwordAuthentication = null;
        if ((!proxy.equals(Proxy.NO_PROXY)) && (stringHasValue(proxyUser)) && (stringHasValue(proxyPass))) {
            passwordAuthentication = new PasswordAuthentication(proxyUser, proxyPass.toCharArray());

            final PasswordAuthentication auth = passwordAuthentication;
            Authenticator.setDefault(new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return auth;
                }

            });
        }

        File workingDirectory = (File) optionSet.valueOf(workingDirectoryOption);
        if ((workingDirectory.exists()) && (!workingDirectory.isDirectory()))
            throw new RuntimeException("Invalid working directory: " + workingDirectory);
        if ((!workingDirectory.exists()) && (!workingDirectory.mkdirs())) {
            throw new RuntimeException("Unable to create directory: " + workingDirectory);
        }

        List strings = optionSet.valuesOf(nonOptions);
        String[] remainderArgs = (String[]) strings.toArray(new String[strings.size()]);

        boolean force = optionSet.has("force");

        JFrame frame = new JFrame();
        workingDirectory = InstallDirSettings.loadAtStartup(frame, workingDirectory);
        System.out.println(workingDirectory);

        new Launcher(frame, workingDirectory, proxy, passwordAuthentication, args, 4);
    }

    public static boolean stringHasValue(String string) {
        return (string != null) && (!string.isEmpty());
    }
}
