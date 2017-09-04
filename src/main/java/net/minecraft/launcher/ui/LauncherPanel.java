package net.minecraft.launcher.ui;

import net.minecraft.launcher.Launcher;
import net.minecraft.launcher.ui.tabs.LauncherTabPanel;

import javax.swing.*;
import java.awt.*;

public class LauncherPanel
        extends JPanel {
    public static final String CARD_DIRT_BACKGROUND = "loading";
    public static final String CARD_LOGIN = "login";
    public static final String CARD_LAUNCHER = "launcher";
    private final CardLayout cardLayout;
    private final LauncherTabPanel tabPanel;
    private final BottomBarPanel bottomBar;
    private final JProgressBar progressBar;
    private final Launcher minecraftLauncher;
    private final JPanel loginPanel;

    public LauncherPanel(Launcher minecraftLauncher) {
        this.minecraftLauncher = minecraftLauncher;
        this.cardLayout = new CardLayout();
        setLayout(this.cardLayout);

        this.progressBar = new JProgressBar();
        this.bottomBar = new BottomBarPanel(minecraftLauncher);
        this.tabPanel = new LauncherTabPanel(minecraftLauncher);
        this.loginPanel = new TexturedPanel("/dirt.png");
        createInterface();
    }

    protected void createInterface() {
        add(createLauncherInterface(), "launcher");
        add(createDirtInterface(), "loading");
        add(createLoginInterface(), "login");
    }

    protected JPanel createLauncherInterface() {
        JPanel result = new JPanel(new BorderLayout());

        this.tabPanel.getBlog().setPage("https://minecraft.net/en-us/");
		this.tabPanel.getReleases().setPage("https://energy0124.github.io/MCFreedomLauncher");

        JPanel topWrapper = new JPanel();
        topWrapper.setLayout(new BorderLayout());
        topWrapper.add(this.tabPanel, "Center");
        topWrapper.add(this.progressBar, "South");

        this.progressBar.setVisible(false);
        this.progressBar.setMinimum(0);
        this.progressBar.setMaximum(100);
        this.progressBar.setStringPainted(true);

        result.add(topWrapper, "Center");
        result.add(this.bottomBar, "South");

        return result;
    }

    protected JPanel createDirtInterface() {
        return new TexturedPanel("/dirt.png");
    }

    protected JPanel createLoginInterface() {
        this.loginPanel.setLayout(new GridBagLayout());
        return this.loginPanel;
    }

    public LauncherTabPanel getTabPanel() {
        return this.tabPanel;
    }

    public BottomBarPanel getBottomBar() {
        return this.bottomBar;
    }

    public JProgressBar getProgressBar() {
        return this.progressBar;
    }

    public Launcher getMinecraftLauncher() {
        return this.minecraftLauncher;
    }

    public void setCard(String card, JPanel additional) {
        if (card.equals("login")) {
            this.loginPanel.removeAll();
            this.loginPanel.add(additional);
        }
        this.cardLayout.show(this, card);
    }
}
