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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import static java.awt.GridBagConstraints.BOTH;
import static java.awt.GridBagConstraints.CENTER;
import static javax.swing.JOptionPane.*;
import static net.minecraft.launcher.LauncherConstants.*;

public class PlayButtonPanel extends JPanel implements RefreshedVersionsListener, RefreshedProfilesListener, UserChangedListener {
    private static final Logger LOGGER = LogManager.getLogger();
    private final Launcher minecraftLauncher;
    private final JButton playButton;
    private final JLabel demoHelpLink;

    public PlayButtonPanel(final Launcher minecraftLauncher) {
        this.playButton = new JButton("Play");
        this.demoHelpLink = new JLabel("Why Only Demo?");
        this.minecraftLauncher = minecraftLauncher;
        minecraftLauncher.getProfileManager().addRefreshedProfilesListener(this);
        minecraftLauncher.getProfileManager().addUserChangedListener(this);
        this.checkState();
        this.createInterface();
        this.playButton.addActionListener(e -> {
            final GameLaunchDispatcher dispatcher = PlayButtonPanel.this.getMinecraftLauncher().getLaunchDispatcher();
            if (dispatcher.isRunningInSameFolder()) {
                try {
                    final int result = JOptionPane.showConfirmDialog(((SwingUserInterface) PlayButtonPanel.this.getMinecraftLauncher().getUserInterface()).getFrame(), MESSAGE_MORE_THAN_ONE_INSTANCE, MINECRAFT_FREEDOM_LAUNCHER_WINDOW_TITLE, YES_NO_OPTION, QUESTION_MESSAGE, LauncherConstants.getFavicon());
                    if (result == YES_OPTION) {
                        dispatcher.play();
                    }
                } catch (Exception e1) {
                    LOGGER.debug("An Exception is caught!");
                }
            } else {
                dispatcher.play();
            }
        });
    }

    protected void createInterface() {
        this.setLayout(new GridBagLayout());
        final GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = BOTH;
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;
        constraints.gridy = 0;
        constraints.gridx = 0;
        this.add(this.playButton, constraints);
        ++constraints.gridy;
        constraints.weighty = 0.0;
        constraints.anchor = CENTER;
        final Font smalltextFont = this.demoHelpLink.getFont().deriveFont(this.demoHelpLink.getFont().getSize() - 2.0f);
        this.demoHelpLink.setCursor(new Cursor(Cursor.HAND_CURSOR));
        this.demoHelpLink.setFont(smalltextFont);
        this.demoHelpLink.setHorizontalAlignment(SwingConstants.CENTER);
        this.demoHelpLink.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                OperatingSystem.openLink(LauncherConstants.constantURI(URL_DEMO_HELP));
            }
        });
        this.add(this.demoHelpLink, constraints);
        this.playButton.setFont(this.playButton.getFont().deriveFont(Font.PLAIN, this.playButton.getFont().getSize() + 2));
    }

    @Override
    public void onProfilesRefreshed(final ProfileManager manager) {
        this.checkState();
    }

    public void checkState() {
        final GameLaunchDispatcher.PlayStatus status = this.minecraftLauncher.getLaunchDispatcher().getStatus();
        this.playButton.setText(status.getName());
        this.playButton.setEnabled(status.canPlay());
        this.demoHelpLink.setVisible(status == GameLaunchDispatcher.PlayStatus.CAN_PLAY_DEMO);
        if (status == GameLaunchDispatcher.PlayStatus.DOWNLOADING) {
            final GameInstanceStatus instanceStatus = this.minecraftLauncher.getLaunchDispatcher().getInstanceStatus();
            if (instanceStatus != GameInstanceStatus.IDLE) {
                this.playButton.setText(instanceStatus.getName());
            }
        }
    }

    @Override
    public void onVersionsRefreshed(final VersionManager manager) {
        SwingUtilities.invokeLater(PlayButtonPanel.this::checkState);
    }

    public Launcher getMinecraftLauncher() {
        return this.minecraftLauncher;
    }

    @Override
    public void onUserChanged(final ProfileManager manager) {
        SwingUtilities.invokeLater(PlayButtonPanel.this::checkState);
    }
}
