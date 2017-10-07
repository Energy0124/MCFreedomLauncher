package io.github.lightwayup.minecraftfreedomlauncher.utility;

import com.mojang.launcher.OperatingSystem;

import java.io.File;

public class WorkingDirectory {
    public static File getWorkingDirectory() {
        final String userHome = System.getProperty("user.home", ".");
        File defaultWorkingDirectory;
        switch (OperatingSystem.getCurrentPlatform()) {
            case LINUX: {
                defaultWorkingDirectory = new File(userHome, ".minecraft/");
                break;
            }
            case WINDOWS: {
                final String applicationData = System.getenv("APPDATA");
                final String folder = (applicationData != null) ? applicationData : userHome;
                defaultWorkingDirectory = new File(folder, ".minecraft/");
                break;
            }
            case MACOS: {
                defaultWorkingDirectory = new File(userHome, "Library/Application Support/minecraft");
                break;
            }
            default: {
                defaultWorkingDirectory = new File(userHome, "minecraft/");
                break;
            }
        }
        return defaultWorkingDirectory;
    }
}