package org.hopto.energy;

import net.minecraft.launcher.Launcher;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Util {
    public static final String APPLICATION_NAME = "minecraft";
    public static String propFileLocation = "./FreeLauncher.properties";

    public static String getProperties(String key) {
        String value = "";
        File propFile = new File(propFileLocation);
        Properties prop = new Properties();
        if (!propFile.exists()) {
            return value;
        } else {
            try {
                prop.load(new FileInputStream(propFile));
            } catch (IOException ex) {
                Logger.getLogger(Launcher.class.getName()).log(Level.SEVERE, null, ex);
            }
            value = prop.getProperty(key);
        }

        return value;
    }

    public static Object setProperties(String key, String value) {
        Object finalValue = "";
        File propFile = new File(propFileLocation);
        Properties prop = new Properties();
        if (!propFile.exists()) {
            return finalValue;
        } else {
            try {
                prop.load(new FileInputStream(propFile));
            } catch (IOException ex) {
                Logger.getLogger(Launcher.class.getName()).log(Level.SEVERE, null, ex);
            }
            finalValue = prop.setProperty(key, value);
            try {
                prop.store(new FileOutputStream(propFile), "MCFreeLauncher");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return finalValue;
    }

    public static void restartApplication() throws URISyntaxException, IOException {
        final String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
        final File currentJar = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        System.out.println(currentJar.getAbsolutePath());
  /* is it a jar file? */
        if (currentJar.getName().endsWith("exe")) {

            final ArrayList<String> command = new ArrayList<String>();
            // command.add(javaBin);
            //command.add("-jar");
            command.add(currentJar.getPath());

            final ProcessBuilder builder = new ProcessBuilder(command);
            builder.start();
            System.exit(0);
        }

        if (!currentJar.getName().endsWith(".jar"))
            return;

  /* Build command: java -jar application.jar */
        final ArrayList<String> command = new ArrayList<String>();
        command.add(javaBin);
        command.add("-jar");
        command.add(currentJar.getPath());

        final ProcessBuilder builder = new ProcessBuilder(command);
        builder.start();
        System.exit(0);
    }

    public static int getPlatform() {                    // I can't get the wrokingDirectory to be set
        String osName = System.getProperty("os.name");   // correctly with the public static enum OS
        if (osName.startsWith("Windows")) {              // thingy, despite getPlatform detects OS properly.
			return 1;                                    // For now returning a integer matching the OS
		} else if (osName.startsWith("Mac")) {           // seems to work fine, but please don't use this
			return 2;                                    // dirty fix forever.
		} else if (osName.startsWith("Linux")) {
			return 3;
		} else {
			return 4;
		}
    }

    public static File getWorkingDirectory() {
        String userHome = System.getProperty("user.home", ".");
        File workingDirectory;
        switch (getPlatform()) {
            case 1:
                String applicationData = System.getenv("APPDATA");
                String folder = applicationData != null ? applicationData : userHome;

                workingDirectory = new File(folder, ".minecraft/");
				break;
			case 2:
                workingDirectory = new File(userHome, "Library/Application Support/minecraft");
                break;
			case 3:
                workingDirectory = new File(userHome, ".minecraft/");
                break;
            default:
                workingDirectory = new File(userHome, "minecraft/");
        }

        return workingDirectory;
    }

    //public static enum OS {
    //    WINDOWS, MACOS, LINUX, UNKNOWN;
    //}
}
