package net.minecraft.launcher.ui.bottombar;

import com.google.common.collect.Lists;
import net.minecraft.launcher.Launcher;
import net.minecraft.launcher.profile.Profile;
import net.minecraft.launcher.profile.ProfileManager;
import net.minecraft.launcher.profile.RefreshedProfilesListener;
import net.minecraft.launcher.ui.popups.profile.ProfileEditorPopup;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class ProfileSelectionPanel
        extends JPanel
        implements ActionListener, ItemListener, RefreshedProfilesListener {
    private static final Logger LOGGER;

    static {
        LOGGER = LogManager.getLogger();
    }

    private final JComboBox profileList = new JComboBox();
    private final JButton newProfileButton = new JButton("New Profile");
    private final JButton editProfileButton = new JButton("Edit Profile");
    private final Launcher minecraftLauncher;
    private boolean skipSelectionUpdate;

    public ProfileSelectionPanel(Launcher minecraftLauncher) {
        this.minecraftLauncher = minecraftLauncher;

        this.profileList.setRenderer(new ProfileListRenderer());
        this.profileList.addItemListener(this);
        this.profileList.addItem("Loading profiles...");

        this.newProfileButton.addActionListener(this);
        this.editProfileButton.addActionListener(this);

        createInterface();

        minecraftLauncher.getProfileManager().addRefreshedProfilesListener(this);
    }

    protected void createInterface() {
        setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = 2;
        constraints.weightx = 0.0D;

        constraints.gridy = 0;

        add(new JLabel("Profile: "), constraints);
        constraints.gridx = 1;
        add(this.profileList, constraints);
        constraints.gridx = 0;

        constraints.gridy += 1;

        JPanel buttonPanel = new JPanel(new GridLayout(1, 2));
        buttonPanel.setBorder(new EmptyBorder(2, 0, 0, 0));
        buttonPanel.add(this.newProfileButton);
        buttonPanel.add(this.editProfileButton);

        constraints.gridwidth = 2;
        add(buttonPanel, constraints);
        constraints.gridwidth = 1;

        constraints.gridy += 1;
    }

    public void onProfilesRefreshed(ProfileManager manager) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                ProfileSelectionPanel.this.populateProfiles();
            }
        });
    }

    public void populateProfiles() {
        String previous = this.minecraftLauncher.getProfileManager().getSelectedProfile().getName();
        Profile selected = null;
        List<Profile> profiles = Lists.newArrayList(this.minecraftLauncher.getProfileManager().getProfiles().values());
        this.profileList.removeAllItems();

        Collections.sort(profiles);

        this.skipSelectionUpdate = true;
        for (Profile profile : profiles) {
            if (previous.equals(profile.getName())) {
                selected = profile;
            }
            this.profileList.addItem(profile);
        }
        if (selected == null) {
            if (profiles.isEmpty()) {
                selected = this.minecraftLauncher.getProfileManager().getSelectedProfile();
                this.profileList.addItem(selected);
            }
            selected = (Profile) profiles.iterator().next();
        }
        this.profileList.setSelectedItem(selected);
        this.skipSelectionUpdate = false;
    }

    public void itemStateChanged(ItemEvent e) {
        if (e.getStateChange() != 1) {
            return;
        }
        if ((!this.skipSelectionUpdate) && ((e.getItem() instanceof Profile))) {
            Profile profile = (Profile) e.getItem();
            this.minecraftLauncher.getProfileManager().setSelectedProfile(profile.getName());
            try {
                this.minecraftLauncher.getProfileManager().saveProfiles();
            } catch (IOException e1) {
                LOGGER.error("Couldn't save new selected profile", e1);
            }
            this.minecraftLauncher.ensureLoggedIn();
        }
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == this.newProfileButton) {
            Profile profile = new Profile(this.minecraftLauncher.getProfileManager().getSelectedProfile());
            profile.setName("Copy of " + profile.getName());
            while (this.minecraftLauncher.getProfileManager().getProfiles().containsKey(profile.getName())) {
                profile.setName(profile.getName() + "_");
            }
            ProfileEditorPopup.showEditProfileDialog(getMinecraftLauncher(), profile);
            this.minecraftLauncher.getProfileManager().setSelectedProfile(profile.getName());
        } else if (e.getSource() == this.editProfileButton) {
            Profile profile = this.minecraftLauncher.getProfileManager().getSelectedProfile();
            ProfileEditorPopup.showEditProfileDialog(getMinecraftLauncher(), profile);
        }
    }

    public Launcher getMinecraftLauncher() {
        return this.minecraftLauncher;
    }

    private static class ProfileListRenderer
            extends BasicComboBoxRenderer {
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            if ((value instanceof Profile)) {
                value = ((Profile) value).getName();
            }
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            return this;
        }
    }
}