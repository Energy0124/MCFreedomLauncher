package io.github.lightwayup.minecraftfreedomlauncher.userinterface;

import net.minecraft.launcher.LauncherConstants;

import javax.swing.*;
import java.awt.*;

public class StartupFrame extends JFrame {
    private static boolean isDisposed = false;

    public StartupFrame() {
        super(LauncherConstants.getTitle());
        this.initialize();
    }

    public static void hide(final JFrame frame) {
        if (!isDisposed) {
            frame.dispose();
            isDisposed = true;
        }
    }

    private void initialize() {
        this.setVisible(false);
        this.setLayout(new GridBagLayout());
        this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        this.setPreferredSize(new Dimension(250, 100));
        this.setResizable(false);
        this.setUndecorated(true);
        this.getContentPane().setBackground(new Color(40, 40, 40));
        this.setFocusableWindowState(false);
        this.pack();
        this.setLocationRelativeTo(null);
        this.setAlwaysOnTop(true);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        JLabel loadingText = new JLabel("Loading...");
        loadingText.setFont(new Font("", Font.PLAIN, 16));
        loadingText.setForeground(new Color(255, 255, 255));
        this.getContentPane().add(loadingText, constraints);
        this.setVisible(true);
    }
}
