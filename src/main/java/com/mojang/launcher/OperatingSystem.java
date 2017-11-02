package com.mojang.launcher;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.URI;

public enum OperatingSystem {
    LINUX("linux", new String[]{"linux", "unix"}),
    WINDOWS("windows", new String[]{"win"}),
    MACOS("osx", new String[]{"mac"}),
    UNKNOWN("unknown", new String[0]);

    private static final Logger LOGGER = LogManager.getLogger();

    private final String name;
    private final String[] aliases;

    OperatingSystem(final String name, final String[] aliases) {
        this.name = name;
        this.aliases = ((aliases == null) ? new String[0] : aliases);
    }

    public static OperatingSystem getCurrentPlatform() {
        final String osName = System.getProperty("os.name").toLowerCase();
        for (final OperatingSystem os : values()) {
            for (final String alias : os.getAliases()) {
                if (osName.contains(alias)) {
                    return os;
                }
            }
        }
        return OperatingSystem.UNKNOWN;
    }

    public static void openLink(final URI link) {
        try {
            final Class<?> desktopClass = Class.forName("java.awt.Desktop");
            final Object o = desktopClass.getMethod("getDesktop", (Class<?>[]) new Class[0]).invoke(null, (Object[]) null);
            desktopClass.getMethod("browse", URI.class).invoke(o, link);
        } catch (Throwable e2) {
            if (getCurrentPlatform() == OperatingSystem.MACOS) {
                try {
                    Runtime.getRuntime().exec(new String[]{"/usr/bin/open", link.toString()});
                } catch (IOException e1) {
                    OperatingSystem.LOGGER.error("Failed to open link " + link.toString(), e1);
                }
            } else {
                OperatingSystem.LOGGER.error("Failed to open link " + link.toString(), e2);
            }
        }
    }

    public static void openFolder(final File path) {
        final String absolutePath = path.getAbsolutePath();
        final OperatingSystem os = getCurrentPlatform();
        Label_0140:
        {
            if (os == OperatingSystem.MACOS) {
                try {
                    Runtime.getRuntime().exec(new String[]{"/usr/bin/open", absolutePath});
                    return;
                } catch (IOException e) {
                    OperatingSystem.LOGGER.error("Couldn't open " + path + " through /usr/bin/open", e);
                    break Label_0140;
                }
            }
            if (os == OperatingSystem.WINDOWS) {
                final String cmd = String.format("cmd.exe /C start \"Open file\" \"%s\"", absolutePath);
                try {
                    Runtime.getRuntime().exec(cmd);
                    return;
                } catch (IOException e2) {
                    OperatingSystem.LOGGER.error("Couldn't open " + path + " through cmd.exe", e2);
                }
            }
            try {
                final Class<?> desktopClass = Class.forName("java.awt.Desktop");
                final Object desktop = desktopClass.getMethod("getDesktop", (Class<?>[]) new Class[0]).invoke(null, (Object[]) null);
                desktopClass.getMethod("browse", URI.class).invoke(desktop, path.toURI());
            } catch (Throwable e3) {
                OperatingSystem.LOGGER.error("Couldn't open " + path + " through Desktop.browse()", e3);
            }
        }
    }

    public String getName() {
        return this.name;
    }

    private String[] getAliases() {
        return this.aliases;
    }

    public boolean isSupported() {
        return this == OperatingSystem.UNKNOWN;
    }

    public String getJavaDir() {
        final String separator = System.getProperty("file.separator");
        final String path = System.getProperty("java.home") + separator + "bin" + separator;
        if (getCurrentPlatform() == OperatingSystem.WINDOWS && new File(path + "javaw.exe").isFile()) {
            return path + "javaw.exe";
        }
        return path + "java";
    }
}
