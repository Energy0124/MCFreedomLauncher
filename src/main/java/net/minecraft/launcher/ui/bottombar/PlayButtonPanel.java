package net.minecraft.launcher.ui.bottombar;

import com.mojang.launcher.OperatingSystem;
import com.mojang.launcher.events.RefreshedVersionsListener;
import com.mojang.launcher.game.GameInstanceStatus;
import com.mojang.launcher.updater.VersionManager;
import net.minecraft.launcher.Launcher;
import net.minecraft.launcher.LauncherConstants;
import net.minecraft.launcher.SwingUserInterface;
import net.minecraft.launcher.game.GameLaunchDispatcher;
import net.minecraft.launcher.profile.ProfileManager;
import net.minecraft.launcher.profile.RefreshedProfilesListener;
import net.minecraft.launcher.profile.UserChangedListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class PlayButtonPanel
        extends JPanel
        implements RefreshedVersionsListener, RefreshedProfilesListener, UserChangedListener {
    private final Launcher minecraftLauncher;
    private final JButton playButton = new JButton("Play");
    private final JLabel demoHelpLink = new JLabel("(Why can I only play demo?)");

    public PlayButtonPanel(Launcher minecraftLauncher) {
        this.minecraftLauncher = minecraftLauncher;

        minecraftLauncher.getProfileManager().addRefreshedProfilesListener(this);
        minecraftLauncher.getProfileManager().addUserChangedListener(this);
        checkState();
        createInterface();

        this.playButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                GameLaunchDispatcher dispatcher = PlayButtonPanel.this.getMinecraftLauncher().getLaunchDispatcher();
                if (dispatcher.isRunningInSameFolder()) {
                    int result = JOptionPane.showConfirmDialog(((SwingUserInterface) PlayButtonPanel.this.getMinecraftLauncher().getUserInterface()).getFrame(), "You already have an instance of Minecraft running. If you launch another one in the same folder, they may clash and corrupt your saves.\nThis could cause many issues, in singleplayer or otherwise. We will not be responsible for anything that goes wrong.\nDo you want to start another instance of Minecraft, despite this?\nYou may solve this issue by launching the game in a different folder (see the \"Edit Profile\" button)", "Duplicate instance warning", 0);
                    if (result == 0) {
                        dispatcher.play();
                    }
                } else {
                    dispatcher.play();
                }
            }
        });
    }

    protected void createInterface() {
        setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = 1;
        constraints.weightx = 1.0D;
        constraints.weighty = 1.0D;

        constraints.gridy = 0;
        constraints.gridx = 0;
        add(this.playButton, constraints);

        constraints.gridy += 1;
        constraints.weighty = 0.0D;
        constraints.anchor = 10;
        Font smalltextFont = this.demoHelpLink.getFont().deriveFont(this.demoHelpLink.getFont().getSize() - 2.0F);
        this.demoHelpLink.setCursor(new Cursor(12));
        this.demoHelpLink.setFont(smalltextFont);
        this.demoHelpLink.setHorizontalAlignment(0);
        this.demoHelpLink.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                OperatingSystem.openLink(LauncherConstants.URL_DEMO_HELP);
            }
        });
        add(this.demoHelpLink, constraints);

        this.playButton.setFont(this.playButton.getFont().deriveFont(1, this.playButton.getFont().getSize() + 2));
    }

    public void onProfilesRefreshed(ProfileManager manager) {
        checkState();
    }

    public void checkState() {
        GameLaunchDispatcher.PlayStatus status = this.minecraftLauncher.getLaunchDispatcher().getStatus();
        this.playButton.setText(status.getName());
        this.playButton.setEnabled(status.canPlay());
        this.demoHelpLink.setVisible(status == GameLaunchDispatcher.PlayStatus.CAN_PLAY_DEMO);
        if (status == GameLaunchDispatcher.PlayStatus.DOWNLOADING) {
            GameInstanceStatus instanceStatus = this.minecraftLauncher.getLaunchDispatcher().getInstanceStatus();
            if (instanceStatus != GameInstanceStatus.IDLE) {
                this.playButton.setText(instanceStatus.getName());
            }
        }
    }

    public void onVersionsRefreshed(VersionManager manager) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                PlayButtonPanel.this.checkState();
            }
        });
    }

    public Launcher getMinecraftLauncher() {
        return this.minecraftLauncher;
    }

    public void onUserChanged(ProfileManager manager) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                PlayButtonPanel.this.checkState();
            }
        });
    }
}