package net.minecraft.launcher;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.SettableFuture;
import com.mojang.authlib.UserAuthentication;
import com.mojang.launcher.OperatingSystem;
import com.mojang.launcher.events.GameOutputLogProcessor;
import com.mojang.launcher.updater.DownloadProgress;
import com.mojang.launcher.versions.CompleteVersion;
import io.github.lightwayup.minecraftfreedomlauncher.userinterface.DialogDisplay;
import io.github.lightwayup.minecraftfreedomlauncher.userinterface.IconManager;
import io.github.lightwayup.minecraftfreedomlauncher.utility.LauncherShutdown;
import net.minecraft.launcher.game.MinecraftGameRunner;
import net.minecraft.launcher.profile.Profile;
import net.minecraft.launcher.profile.ProfileManager;
import net.minecraft.launcher.ui.LauncherPanel;
import net.minecraft.launcher.ui.popups.login.LogInPopup;
import net.minecraft.launcher.ui.tabs.CrashReportTab;
import net.minecraft.launcher.ui.tabs.GameOutputTab;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;

import static java.awt.event.WindowEvent.WINDOW_CLOSING;
import static javax.swing.JOptionPane.*;
import static net.minecraft.launcher.LauncherConstants.*;

public class SwingUserInterface implements MinecraftUserInterface {
    private static final Logger LOGGER = LogManager.getLogger();

    private final Launcher minecraftLauncher;
    private final JFrame frame;
    private LauncherPanel launcherPanel;

    SwingUserInterface(final Launcher minecraftLauncher, final JFrame frame) {
        this.minecraftLauncher = minecraftLauncher;
        this.frame = frame;
    }

    private void showLoginPrompt(final Launcher minecraftLauncher, final LogInPopup.Callback callback) {
        SwingUtilities.invokeLater(() -> {
            final LogInPopup popup = new LogInPopup(minecraftLauncher, callback);
            SwingUserInterface.this.launcherPanel.setCard("login", popup);
        });
    }

    void initializeFrame() {
        this.frame.getContentPane().removeAll();
        this.frame.setTitle(new LauncherConstants().windowTitle);
        this.frame.setPreferredSize(new Dimension(1280, 720));
        this.frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        this.frame.setResizable(false);
        this.frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(final WindowEvent e) {
                SwingUserInterface.LOGGER.info("Window closed, shutting down.");
                SwingUserInterface.this.frame.setVisible(false);
                SwingUserInterface.this.frame.dispose();
                SwingUserInterface.LOGGER.info("Halting executors");
                SwingUserInterface.this.minecraftLauncher.getLauncher().getVersionManager().getExecutorService().shutdown();
                SwingUserInterface.LOGGER.info("Awaiting termination.");
                try {
                    SwingUserInterface.this.minecraftLauncher.getLauncher().getVersionManager().getExecutorService().awaitTermination(10L, TimeUnit.SECONDS);
                } catch (InterruptedException e2) {
                    SwingUserInterface.LOGGER.info("Termination took too long.");
                }
                SwingUserInterface.LOGGER.info("Goodbye.");
                LauncherShutdown.forcefullyShutdown("launcher is requested to shutdown");
            }
        });
        try {
            final InputStream in = Launcher.class.getResourceAsStream(IMAGE_FAVICON);
            if (in != null) {
                this.frame.setIconImage(ImageIO.read(in));
            }
        } catch (IOException ex) {
            LOGGER.debug("An IOException is caught!");
        }
        this.launcherPanel = new LauncherPanel(this.minecraftLauncher);
        SwingUtilities.invokeLater(() -> {
            SwingUserInterface.this.frame.add(SwingUserInterface.this.launcherPanel);
            SwingUserInterface.this.frame.pack();
            SwingUserInterface.this.frame.setLocationRelativeTo(null);
            SwingUserInterface.this.frame.setVisible(true);
            SwingUserInterface.this.frame.setAlwaysOnTop(true);
            SwingUserInterface.this.frame.setAlwaysOnTop(false);
        });
    }

    @Override
    public void showOutdatedNotice() {
        this.frame.getContentPane().removeAll();
        DialogDisplay.showError(MESSAGE_LAUNCHER_OUTDATED);
        try {
            if (OperatingSystem.getCurrentPlatform() == OperatingSystem.WINDOWS) {
                OperatingSystem.openLink(new URI(URL_BOOTSTRAP_DOWNLOAD + ".exe"));
            } else if (OperatingSystem.getCurrentPlatform() == OperatingSystem.MACOS) {
                OperatingSystem.openLink(new URI(URL_BOOTSTRAP_DOWNLOAD + ".dmg"));
            } else {
                OperatingSystem.openLink(new URI(URL_BOOTSTRAP_DOWNLOAD + ".jar"));
            }
        } catch (URISyntaxException e) {
            SwingUserInterface.LOGGER.error("Couldn't open bootstrap download link", e);
        }
        this.minecraftLauncher.getLauncher().shutdownLauncher();
    }

    @Override
    public void showLoginPrompt() {
        final ProfileManager profileManager = this.minecraftLauncher.getProfileManager();
        try {
            profileManager.saveProfiles();
        } catch (IOException e) {
            SwingUserInterface.LOGGER.error("Couldn't save profiles before logging in!", e);
        }
        final Profile selectedProfile = profileManager.getSelectedProfile();
        this.showLoginPrompt(this.minecraftLauncher, uuid -> {
            final UserAuthentication auth = profileManager.getAuthDatabase().getByUUID(uuid);
            profileManager.setSelectedUser(uuid);
            if (selectedProfile.getName().equals("Minecraft") && auth.getSelectedProfile() != null) {
                final String playerName = auth.getSelectedProfile().getName();
                String profileName = auth.getSelectedProfile().getName();
                int count = 1;
                while (profileManager.getProfiles().containsKey(profileName)) {
                    profileName = playerName + " " + ++count;
                }
                final Profile newProfile = new Profile(selectedProfile);
                newProfile.setName(profileName);
                profileManager.getProfiles().put(profileName, newProfile);
                profileManager.getProfiles().remove("Minecraft");
                profileManager.setSelectedProfile(profileName);
            }
            try {
                profileManager.saveProfiles();
            } catch (IOException e) {
                SwingUserInterface.LOGGER.error("Couldn't save profiles after logging in!", e);
            }
            if (uuid == null) {
                SwingUserInterface.this.minecraftLauncher.getLauncher().shutdownLauncher();
            } else {
                profileManager.fireRefreshEvent();
            }
            SwingUserInterface.this.launcherPanel.setCard("launcher", null);
        });
    }

    @Override
    public void setVisible(final boolean visible) {
        SwingUtilities.invokeLater(() -> SwingUserInterface.this.frame.setVisible(visible));
    }

    @Override
    public void shutdownLauncher() {
        if (SwingUtilities.isEventDispatchThread()) {
            SwingUserInterface.LOGGER.info("Requesting window close");
            this.frame.dispatchEvent(new WindowEvent(this.frame, WINDOW_CLOSING));
        } else {
            SwingUtilities.invokeLater(SwingUserInterface.this::shutdownLauncher);
        }
    }

    @Override
    public void setDownloadProgress(final DownloadProgress downloadProgress) {
        SwingUtilities.invokeLater(() -> {
            SwingUserInterface.this.launcherPanel.getProgressBar().setVisible(true);
            SwingUserInterface.this.launcherPanel.getProgressBar().setValue((int) (downloadProgress.getPercent() * 100.0f));
            SwingUserInterface.this.launcherPanel.getProgressBar().setString(downloadProgress.getStatus());
        });
    }

    @Override
    public void hideDownloadProgress() {
        SwingUtilities.invokeLater(() -> SwingUserInterface.this.launcherPanel.getProgressBar().setVisible(false));
    }

    @Override
    public void showCrashReport(final CompleteVersion version, final File crashReportFile, final String crashReport) {
        SwingUtilities.invokeLater(() -> SwingUserInterface.this.launcherPanel.getTabPanel().setCrashReport(new CrashReportTab(SwingUserInterface.this.minecraftLauncher, version, crashReportFile, crashReport)));
    }

    @Override
    public void gameLaunchFailure(final String reason) {
        try {
            SwingUtilities.invokeLater(() -> DialogDisplay.showError(reason));
        } catch (Exception e) {
            LOGGER.debug("An Exception is caught!");
        }
    }

    @Override
    public void updatePlayState() {
        SwingUtilities.invokeLater(() -> SwingUserInterface.this.launcherPanel.getBottomBar().getPlayButtonPanel().checkState());
    }

    @Override
    public GameOutputLogProcessor showGameOutputTab(final MinecraftGameRunner gameRunner) {
        final SettableFuture<GameOutputLogProcessor> future = SettableFuture.create();
        SwingUtilities.invokeLater(() -> {
            final GameOutputTab tab = new GameOutputTab(SwingUserInterface.this.minecraftLauncher);
            future.set(tab);
            final UserAuthentication auth = gameRunner.getAuth();
            final String name = (auth.getSelectedProfile() == null) ? "Demo" : auth.getSelectedProfile().getName();
            SwingUserInterface.this.launcherPanel.getTabPanel().removeTab("Game Output (" + name + ")");
            SwingUserInterface.this.launcherPanel.getTabPanel().addTab("Game Output (" + name + ")", tab);
            SwingUserInterface.this.launcherPanel.getTabPanel().setSelectedComponent(tab);
        });
        return Futures.getUnchecked(future);
    }

    @Override
    public boolean shouldDowngradeProfiles() {
        try {
            final int result = JOptionPane.showConfirmDialog(this.frame, MESSAGE_LAUNCHER_NEWER_VERSION_USED, new LauncherConstants().windowTitle, YES_NO_OPTION, ERROR_MESSAGE, IconManager.getIcon());
            return result == YES_OPTION;
        } catch (Exception e) {
            LOGGER.debug("An Exception is caught!");
            return false;
        }
    }

    @Override
    public String getTitle() {
        return new LauncherConstants().windowTitle;
    }

    public JFrame getFrame() {
        return this.frame;
    }
}
