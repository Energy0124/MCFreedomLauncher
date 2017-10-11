package net.minecraft.launcher.ui.bottombar;

import com.mojang.authlib.UserAuthentication;
import com.mojang.launcher.OperatingSystem;
import com.mojang.launcher.events.RefreshedVersionsListener;
import com.mojang.launcher.updater.VersionManager;
import com.mojang.launcher.updater.VersionSyncInfo;
import net.minecraft.launcher.Launcher;
import net.minecraft.launcher.LauncherConstants;
import net.minecraft.launcher.profile.Profile;
import net.minecraft.launcher.profile.ProfileManager;
import net.minecraft.launcher.profile.RefreshedProfilesListener;
import net.minecraft.launcher.profile.UserChangedListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import static java.awt.GridBagConstraints.HORIZONTAL;
import static java.awt.GridBagConstraints.NONE;
import static net.minecraft.launcher.LauncherConstants.URL_HELP;

public class PlayerInfoPanel extends JPanel implements RefreshedVersionsListener, RefreshedProfilesListener, UserChangedListener {
    private final Launcher minecraftLauncher;
    private final JLabel welcomeText;
    private final JLabel versionText;
    private final JButton switchUserButton;
    private final JButton supportButton;

    public PlayerInfoPanel(final Launcher minecraftLauncher) {
        this.welcomeText = new JLabel("", SwingConstants.CENTER);
        this.versionText = new JLabel("", SwingConstants.CENTER);
        this.supportButton = new JButton("Support");
        this.supportButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                OperatingSystem.openLink(LauncherConstants.constantURI(URL_HELP));
            }
        });
        this.switchUserButton = new JButton("Switch User");
        this.minecraftLauncher = minecraftLauncher;
        minecraftLauncher.getProfileManager().addRefreshedProfilesListener(this);
        minecraftLauncher.getProfileManager().addUserChangedListener(this);
        this.checkState();
        this.createInterface();
        this.switchUserButton.setEnabled(false);
        this.switchUserButton.addActionListener(e -> minecraftLauncher.getUserInterface().showLoginPrompt());
    }

    protected void createInterface() {
        this.setLayout(new GridBagLayout());
        final GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = HORIZONTAL;
        constraints.gridy = 0;
        constraints.weightx = 1.0;
        constraints.gridwidth = 2;
        this.add(this.welcomeText, constraints);
        constraints.gridwidth = 1;
        constraints.weightx = 0.0;
        ++constraints.gridy;
        constraints.weightx = 1.0;
        constraints.gridwidth = 2;
        this.add(this.versionText, constraints);
        constraints.gridwidth = 1;
        constraints.weightx = 0.0;
        ++constraints.gridy;
        constraints.weightx = 0.5;
        constraints.fill = NONE;
        this.add(this.switchUserButton, constraints);
        this.add(this.supportButton, constraints);
        constraints.weightx = 0.0;
        ++constraints.gridy;
    }

    @Override
    public void onProfilesRefreshed(final ProfileManager manager) {
        SwingUtilities.invokeLater(PlayerInfoPanel.this::checkState);
    }

    public void checkState() {
        final ProfileManager profileManager = this.minecraftLauncher.getProfileManager();
        final UserAuthentication auth = (profileManager.getSelectedUser() == null) ? null : profileManager.getAuthDatabase().getByUUID(profileManager.getSelectedUser());
        if (auth == null || !auth.isLoggedIn()) {
            this.welcomeText.setText("<html>Welcome, player! Please log in.</html>");
        } else if (auth.getSelectedProfile() == null) {
            this.welcomeText.setText("<html>Welcome, player!</html>");
        } else {
            this.welcomeText.setText("<html>Welcome, " + auth.getSelectedProfile().getName() + "</html>");
        }
        final Profile profile = profileManager.getProfiles().isEmpty() ? null : profileManager.getSelectedProfile();
        final List<VersionSyncInfo> versions = (profile == null) ? null : this.minecraftLauncher.getLauncher().getVersionManager().getVersions(profile.getVersionFilter());
        VersionSyncInfo version = (profile == null || versions.isEmpty()) ? null : versions.get(0);
        if (profile != null && profile.getLastVersionId() != null) {
            final VersionSyncInfo requestedVersion = this.minecraftLauncher.getLauncher().getVersionManager().getVersionSyncInfo(profile.getLastVersionId());
            if (requestedVersion != null && requestedVersion.getLatestVersion() != null) {
                version = requestedVersion;
            }
        }
        if (version == null) {
            this.versionText.setText("Loading versions...");
        } else if (version.isUpToDate()) {
            this.versionText.setText("Ready to play Minecraft " + version.getLatestVersion().getId());
        } else if (version.isInstalled()) {
            this.versionText.setText("Ready to update & play Minecraft " + version.getLatestVersion().getId());
        } else if (version.isOnRemote()) {
            this.versionText.setText("Ready to download & play Minecraft " + version.getLatestVersion().getId());
        }
        this.switchUserButton.setEnabled(true);
    }

    @Override
    public void onVersionsRefreshed(final VersionManager manager) {
        SwingUtilities.invokeLater(PlayerInfoPanel.this::checkState);
    }

    public Launcher getMinecraftLauncher() {
        return this.minecraftLauncher;
    }

    @Override
    public void onUserChanged(final ProfileManager manager) {
        SwingUtilities.invokeLater(PlayerInfoPanel.this::checkState);
    }
}
