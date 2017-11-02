package io.github.lightwayup.minecraftfreedomlauncher.userinterface;

import net.minecraft.launcher.LauncherConstants;

import javax.swing.*;

public class DialogDisplay {
    public static void showPlain(String message) {
        show(message, JOptionPane.PLAIN_MESSAGE);
    }

    public static void showInfo(String message) {
        show(message, JOptionPane.INFORMATION_MESSAGE);
    }

    public static void showWarning(String message) {
        show(message, JOptionPane.WARNING_MESSAGE);
    }

    public static void showQuestion(String message) {
        show(message, JOptionPane.QUESTION_MESSAGE);
    }

    public static void showError(String message) {
        show(message, JOptionPane.ERROR_MESSAGE);
    }

    private static void show(String message, int type) {
        JOptionPane.showMessageDialog(null, message, new LauncherConstants().windowTitle, type, IconManager.getIcon());
    }
}
