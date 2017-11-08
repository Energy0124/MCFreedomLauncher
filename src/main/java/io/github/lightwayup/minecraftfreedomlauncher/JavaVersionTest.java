package io.github.lightwayup.minecraftfreedomlauncher;

import io.github.lightwayup.minecraftfreedomlauncher.userinterface.LookAndFeelManager;
import net.minecraft.launcher.LauncherConstants;

import javax.swing.*;

public class JavaVersionTest {
    // This test is solely for JRE version checking.
    // As they're changing the versioning scheme frequently recently,
    // this test should be run before releasing a new version on GitHub.

    public static void runJavaVersionTest() {
        LookAndFeelManager.setLookAndFeel();
        System.out.println(Double.parseDouble(System.getProperty("java.specification.version")));
        System.out.println(System.getProperty("java.specification.vendor").toLowerCase());
        System.out.println(String.valueOf(Double.parseDouble(System.getProperty("java.specification.version")) == 9));
        System.out.println(String.valueOf(System.getProperty("java.specification.vendor").toLowerCase().contains("oracle")));
        JOptionPane.showMessageDialog(null, "Click \"OK\" to exit.", LauncherConstants.getTitle(), JOptionPane.INFORMATION_MESSAGE);
        System.exit(0);
    }
}
