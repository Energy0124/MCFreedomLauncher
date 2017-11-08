package net.minecraft.launcher.ui.popups.profile;

import com.mojang.launcher.OperatingSystem;
import net.minecraft.launcher.Launcher;
import net.minecraft.launcher.LauncherConstants;
import net.minecraft.launcher.SwingUserInterface;
import net.minecraft.launcher.profile.Profile;
import net.minecraft.launcher.profile.ProfileManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.Map;

public class ProfileEditorPopup extends JPanel implements ActionListener {
    private static final Logger LOGGER = LogManager.getLogger();

    private final Launcher minecraftLauncher;
    private final Profile originalProfile;
    private final Profile profile;
    private final JButton saveButton;
    private final JButton cancelButton;
    private final JButton browseButton;
    private final ProfileInfoPanel profileInfoPanel;
    private final ProfileVersionPanel profileVersionPanel;
    private final ProfileJavaPanel javaInfoPanel;

    private ProfileEditorPopup(final Launcher minecraftLauncher, final Profile profile) {
        super(true);
        this.saveButton = new JButton("Save Profile");
        this.cancelButton = new JButton("Cancel");
        this.browseButton = new JButton("Open Game Directory");
        this.minecraftLauncher = minecraftLauncher;
        this.originalProfile = profile;
        this.profile = new Profile(profile);
        this.profileInfoPanel = new ProfileInfoPanel(this);
        this.profileVersionPanel = new ProfileVersionPanel(this);
        this.javaInfoPanel = new ProfileJavaPanel(this);
        this.saveButton.addActionListener(this);
        this.cancelButton.addActionListener(this);
        this.browseButton.addActionListener(this);
        this.setBorder(new EmptyBorder(5, 5, 5, 5));
        this.setLayout(new BorderLayout(0, 5));
        this.createInterface();
    }

    public static void showEditProfileDialog(final Launcher minecraftLauncher, final Profile profile) {
        final JFrame frame = ((SwingUserInterface) minecraftLauncher.getUserInterface()).getFrame();
        final JDialog dialog = new JDialog(frame, LauncherConstants.getTitle(), true);
        final ProfileEditorPopup editor = new ProfileEditorPopup(minecraftLauncher, profile);
        dialog.setPreferredSize(new Dimension(1024, 576));
        dialog.setResizable(false);
        dialog.add(editor);
        dialog.pack();
        dialog.setLocationRelativeTo(frame);
        dialog.setVisible(true);
    }

    private void createInterface() {
        final JPanel standardPanels = new JPanel(true);
        standardPanels.setLayout(new BoxLayout(standardPanels, BoxLayout.Y_AXIS));
        standardPanels.add(this.profileInfoPanel);
        standardPanels.add(this.profileVersionPanel);
        standardPanels.add(this.javaInfoPanel);
        this.add(standardPanels, "Center");
        final JPanel buttonPannel = new JPanel();
        buttonPannel.setLayout(new BoxLayout(buttonPannel, BoxLayout.X_AXIS));
        buttonPannel.add(this.cancelButton);
        buttonPannel.add(Box.createGlue());
        buttonPannel.add(this.browseButton);
        buttonPannel.add(Box.createHorizontalStrut(5));
        buttonPannel.add(this.saveButton);
        this.add(buttonPannel, "South");
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        if (e.getSource() == this.saveButton) {
            try {
                final ProfileManager manager = this.minecraftLauncher.getProfileManager();
                final Map<String, Profile> profiles = manager.getProfiles();
                final String selected = manager.getSelectedProfile().getName();
                if (!this.originalProfile.getName().equals(this.profile.getName())) {
                    profiles.remove(this.originalProfile.getName());
                    while (profiles.containsKey(this.profile.getName())) {
                        this.profile.setName(this.profile.getName() + "_");
                    }
                }
                profiles.put(this.profile.getName(), this.profile);
                if (selected.equals(this.originalProfile.getName())) {
                    manager.setSelectedProfile(this.profile.getName());
                }
                manager.saveProfiles();
                manager.fireRefreshEvent();
            } catch (IOException ex) {
                ProfileEditorPopup.LOGGER.error("Couldn't save profiles whilst editing " + this.profile.getName(), ex);
            } catch (Exception ex) {
                ProfileEditorPopup.LOGGER.error("An Exception is caught!");
            }
            this.closeWindow();
        } else if (e.getSource() == this.browseButton) {
            OperatingSystem.openFolder((this.profile.getGameDir() == null) ? this.minecraftLauncher.getLauncher().getWorkingDirectory() : this.profile.getGameDir());
        } else {
            this.closeWindow();
        }
    }

    private void closeWindow() {
        if (SwingUtilities.isEventDispatchThread()) {
            final Window window = (Window) this.getTopLevelAncestor();
            window.dispatchEvent(new WindowEvent(window, 201));
        } else {
            SwingUtilities.invokeLater(ProfileEditorPopup.this::closeWindow);
        }
    }

    public Launcher getMinecraftLauncher() {
        return this.minecraftLauncher;
    }

    public Profile getProfile() {
        return this.profile;
    }
}
