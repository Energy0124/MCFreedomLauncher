package net.minecraft.launcher.ui.popups.profile;

import net.minecraft.launcher.profile.LauncherVisibilityRule;
import net.minecraft.launcher.profile.Profile;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.io.File;

import static java.awt.GridBagConstraints.*;

class ProfileInfoPanel extends JPanel {
    private final ProfileEditorPopup editor;
    private final JCheckBox gameDirCustom;
    private final JTextField profileName;
    private final JTextField gameDirField;
    private final JCheckBox resolutionCustom;
    private final JTextField resolutionWidth;
    private final JTextField resolutionHeight;
    private final JCheckBox useHopper;
    private final JCheckBox launcherVisibilityCustom;
    private final JComboBox launcherVisibilityOption;

    ProfileInfoPanel(final ProfileEditorPopup editor) {
        this.gameDirCustom = new JCheckBox("Game Directory:");
        this.profileName = new JTextField();
        this.gameDirField = new JTextField();
        this.resolutionCustom = new JCheckBox("Resolution:");
        this.resolutionWidth = new JTextField();
        this.resolutionHeight = new JTextField();
        this.useHopper = new JCheckBox("Automatically ask Mojang for assistance with fixing crashes");
        this.launcherVisibilityCustom = new JCheckBox("Launcher Visibility:");
        this.launcherVisibilityOption = new JComboBox();
        this.editor = editor;
        this.setLayout(new GridBagLayout());
        this.setBorder(BorderFactory.createTitledBorder("Profile Info"));
        this.createInterface();
        this.fillDefaultValues();
        this.addEventHandlers();
    }

    private void createInterface() {
        final GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(2, 2, 2, 2);
        constraints.anchor = WEST;
        constraints.gridy = 0;
        this.add(new JLabel("Profile Name:"), constraints);
        constraints.fill = HORIZONTAL;
        constraints.weightx = 1.0;
        this.add(this.profileName, constraints);
        constraints.weightx = 0.0;
        constraints.fill = NONE;
        ++constraints.gridy;
        this.add(this.gameDirCustom, constraints);
        constraints.fill = HORIZONTAL;
        constraints.weightx = 1.0;
        this.add(this.gameDirField, constraints);
        constraints.weightx = 0.0;
        constraints.fill = NONE;
        ++constraints.gridy;
        final JPanel resolutionPanel = new JPanel();
        resolutionPanel.setLayout(new BoxLayout(resolutionPanel, BoxLayout.X_AXIS));
        resolutionPanel.add(new JLabel("Width:"));
        resolutionPanel.add(Box.createHorizontalStrut(5));
        resolutionPanel.add(this.resolutionWidth);
        resolutionPanel.add(Box.createHorizontalStrut(10));
        resolutionPanel.add(new JLabel("Height:"));
        resolutionPanel.add(Box.createHorizontalStrut(5));
        resolutionPanel.add(this.resolutionHeight);
        this.add(this.resolutionCustom, constraints);
        constraints.fill = HORIZONTAL;
        constraints.weightx = 1.0;
        this.add(resolutionPanel, constraints);
        constraints.weightx = 0.0;
        constraints.fill = NONE;
        ++constraints.gridy;
        constraints.fill = HORIZONTAL;
        constraints.weightx = 1.0;
        constraints.gridwidth = 0;
        this.add(this.useHopper, constraints);
        constraints.gridwidth = 1;
        constraints.weightx = 0.0;
        constraints.fill = NONE;
        ++constraints.gridy;
        this.add(this.launcherVisibilityCustom, constraints);
        constraints.fill = HORIZONTAL;
        constraints.weightx = 1.0;
        this.add(this.launcherVisibilityOption, constraints);
        constraints.weightx = 0.0;
        constraints.fill = NONE;
        ++constraints.gridy;
        for (final LauncherVisibilityRule value : LauncherVisibilityRule.values()) {
            this.launcherVisibilityOption.addItem(value);
        }
    }

    private void fillDefaultValues() {
        this.profileName.setText(this.editor.getProfile().getName());
        final File gameDir = this.editor.getProfile().getGameDir();
        if (gameDir != null) {
            this.gameDirCustom.setSelected(true);
            this.gameDirField.setText(gameDir.getAbsolutePath());
        } else {
            this.gameDirCustom.setSelected(false);
            this.gameDirField.setText(this.editor.getMinecraftLauncher().getLauncher().getWorkingDirectory().getAbsolutePath());
        }
        this.updateGameDirState();
        Profile.Resolution resolution = this.editor.getProfile().getResolution();
        this.resolutionCustom.setSelected(resolution != null);
        if (resolution == null) {
            resolution = Profile.DEFAULT_RESOLUTION;
        }
        this.resolutionWidth.setText(String.valueOf(resolution.getWidth()));
        this.resolutionHeight.setText(String.valueOf(resolution.getHeight()));
        this.updateResolutionState();
        this.useHopper.setSelected(this.editor.getProfile().getUseHopperCrashService());
        final LauncherVisibilityRule visibility = this.editor.getProfile().getLauncherVisibilityOnGameClose();
        if (visibility != null) {
            this.launcherVisibilityCustom.setSelected(true);
            this.launcherVisibilityOption.setSelectedItem(visibility);
        } else {
            this.launcherVisibilityCustom.setSelected(false);
            this.launcherVisibilityOption.setSelectedItem(Profile.DEFAULT_LAUNCHER_VISIBILITY);
        }
        this.updateLauncherVisibilityState();
    }

    private void addEventHandlers() {
        this.profileName.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(final DocumentEvent e) {
                ProfileInfoPanel.this.updateProfileName();
            }

            @Override
            public void removeUpdate(final DocumentEvent e) {
                ProfileInfoPanel.this.updateProfileName();
            }

            @Override
            public void changedUpdate(final DocumentEvent e) {
                ProfileInfoPanel.this.updateProfileName();
            }
        });
        this.gameDirCustom.addItemListener(e -> ProfileInfoPanel.this.updateGameDirState());
        this.gameDirField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(final DocumentEvent e) {
                ProfileInfoPanel.this.updateGameDir();
            }

            @Override
            public void removeUpdate(final DocumentEvent e) {
                ProfileInfoPanel.this.updateGameDir();
            }

            @Override
            public void changedUpdate(final DocumentEvent e) {
                ProfileInfoPanel.this.updateGameDir();
            }
        });
        this.resolutionCustom.addItemListener(e -> ProfileInfoPanel.this.updateResolutionState());
        final DocumentListener resolutionListener = new DocumentListener() {
            @Override
            public void insertUpdate(final DocumentEvent e) {
                ProfileInfoPanel.this.updateResolution();
            }

            @Override
            public void removeUpdate(final DocumentEvent e) {
                ProfileInfoPanel.this.updateResolution();
            }

            @Override
            public void changedUpdate(final DocumentEvent e) {
                ProfileInfoPanel.this.updateResolution();
            }
        };
        this.resolutionWidth.getDocument().addDocumentListener(resolutionListener);
        this.resolutionHeight.getDocument().addDocumentListener(resolutionListener);
        this.useHopper.addItemListener(e -> ProfileInfoPanel.this.updateHopper());
        this.launcherVisibilityCustom.addItemListener(e -> ProfileInfoPanel.this.updateLauncherVisibilityState());
        this.launcherVisibilityOption.addItemListener(e -> ProfileInfoPanel.this.updateLauncherVisibilitySelection());
    }

    private void updateLauncherVisibilityState() {
        final Profile profile = this.editor.getProfile();
        if (this.launcherVisibilityCustom.isSelected() && this.launcherVisibilityOption.getSelectedItem() instanceof LauncherVisibilityRule) {
            profile.setLauncherVisibilityOnGameClose((LauncherVisibilityRule) this.launcherVisibilityOption.getSelectedItem());
            this.launcherVisibilityOption.setEnabled(true);
        } else {
            profile.setLauncherVisibilityOnGameClose(null);
            this.launcherVisibilityOption.setEnabled(false);
        }
    }

    private void updateLauncherVisibilitySelection() {
        final Profile profile = this.editor.getProfile();
        if (this.launcherVisibilityOption.getSelectedItem() instanceof LauncherVisibilityRule) {
            profile.setLauncherVisibilityOnGameClose((LauncherVisibilityRule) this.launcherVisibilityOption.getSelectedItem());
        }
    }

    private void updateHopper() {
        final Profile profile = this.editor.getProfile();
        if (this.useHopper.isSelected()) {
            profile.setUseHopperCrashService(true);
        } else {
            profile.setUseHopperCrashService(false);
        }
    }

    private void updateProfileName() {
        if (this.profileName.getText().length() > 0) {
            this.editor.getProfile().setName(this.profileName.getText());
        }
    }

    private void updateGameDirState() {
        if (this.gameDirCustom.isSelected()) {
            this.gameDirField.setEnabled(true);
            this.editor.getProfile().setGameDir(new File(this.gameDirField.getText()));
        } else {
            this.gameDirField.setEnabled(false);
            this.editor.getProfile().setGameDir(null);
        }
    }

    private void updateResolutionState() {
        if (this.resolutionCustom.isSelected()) {
            this.resolutionWidth.setEnabled(true);
            this.resolutionHeight.setEnabled(true);
            this.updateResolution();
        } else {
            this.resolutionWidth.setEnabled(false);
            this.resolutionHeight.setEnabled(false);
            this.editor.getProfile().setResolution(null);
        }
    }

    private void updateResolution() {
        try {
            final int width = Integer.parseInt(this.resolutionWidth.getText());
            final int height = Integer.parseInt(this.resolutionHeight.getText());
            this.editor.getProfile().setResolution(new Profile.Resolution(width, height));
        } catch (NumberFormatException ignored) {
            this.editor.getProfile().setResolution(null);
        }
    }

    private void updateGameDir() {
        final File file = new File(this.gameDirField.getText());
        this.editor.getProfile().setGameDir(file);
    }
}
