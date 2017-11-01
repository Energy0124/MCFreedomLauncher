package net.minecraft.launcher.ui.popups.profile;

import com.google.common.collect.Sets;
import com.mojang.launcher.events.RefreshedVersionsListener;
import com.mojang.launcher.updater.VersionManager;
import com.mojang.launcher.updater.VersionSyncInfo;
import com.mojang.launcher.versions.Version;
import io.github.lightwayup.minecraftfreedomlauncher.userinterface.IconManager;
import net.minecraft.launcher.LauncherConstants;
import net.minecraft.launcher.SwingUserInterface;
import net.minecraft.launcher.game.MinecraftReleaseType;
import net.minecraft.launcher.profile.Profile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static java.awt.GridBagConstraints.*;
import static javax.swing.JOptionPane.*;

public class ProfileVersionPanel extends JPanel implements RefreshedVersionsListener {
    private static final Logger LOGGER = LogManager.getLogger();

    private final ProfileEditorPopup editor;
    private final JComboBox versionList;
    private final List<ReleaseTypeCheckBox> customVersionTypes;

    public ProfileVersionPanel(final ProfileEditorPopup editor) {
        this.versionList = new JComboBox();
        this.customVersionTypes = new ArrayList<>();
        this.editor = editor;
        this.setLayout(new GridBagLayout());
        this.setBorder(BorderFactory.createTitledBorder("Version Selection"));
        this.createInterface();
        this.addEventHandlers();
        final List<VersionSyncInfo> versions = editor.getMinecraftLauncher().getLauncher().getVersionManager().getVersions(editor.getProfile().getVersionFilter());
        if (versions.isEmpty()) {
            editor.getMinecraftLauncher().getLauncher().getVersionManager().addRefreshedVersionsListener(this);
        } else {
            this.populateVersions(versions);
        }
    }

    protected void createInterface() {
        final GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(2, 2, 2, 2);
        constraints.anchor = WEST;
        constraints.gridy = 0;
        for (final MinecraftReleaseType type : MinecraftReleaseType.values()) {
            if (type.getDescription() != null) {
                final ReleaseTypeCheckBox checkbox = new ReleaseTypeCheckBox(type);
                checkbox.setSelected(this.editor.getProfile().getVersionFilter().getTypes().contains(type));
                this.customVersionTypes.add(checkbox);
                constraints.fill = HORIZONTAL;
                constraints.weightx = 1.0;
                constraints.gridwidth = 0;
                this.add(checkbox, constraints);
                constraints.gridwidth = 1;
                constraints.weightx = 0.0;
                constraints.fill = NONE;
                ++constraints.gridy;
            }
        }
        this.add(new JLabel("Use version:"), constraints);
        constraints.fill = HORIZONTAL;
        constraints.weightx = 1.0;
        this.add(this.versionList, constraints);
        constraints.weightx = 0.0;
        constraints.fill = NONE;
        ++constraints.gridy;
        this.versionList.setRenderer(new VersionListRenderer());
    }

    protected void addEventHandlers() {
        this.versionList.addItemListener(e -> ProfileVersionPanel.this.updateVersionSelection());
        for (final ReleaseTypeCheckBox type : this.customVersionTypes) {
            type.addItemListener(new ItemListener() {
                private boolean isUpdating = false;

                @Override
                public void itemStateChanged(final ItemEvent e) {
                    if (this.isUpdating) {
                        return;
                    }
                    if (e.getStateChange() == ItemEvent.SELECTED && type.getType().getPopupWarning() != null) {
                        try {
                            final int result = JOptionPane.showConfirmDialog(((SwingUserInterface) ProfileVersionPanel.this.editor.getMinecraftLauncher().getUserInterface()).getFrame(), type.getType().getPopupWarning() + "\nAre you sure you want to continue?", new LauncherConstants().windowTitle, YES_NO_OPTION, QUESTION_MESSAGE, IconManager.getIcon());
                            this.isUpdating = true;
                            if (result == YES_OPTION) {
                                type.setSelected(true);
                                ProfileVersionPanel.this.updateCustomVersionFilter();
                            } else {
                                type.setSelected(false);
                            }
                            this.isUpdating = false;
                        } catch (Exception e1) {
                            LOGGER.debug("An Exception is caught!");
                            type.setSelected(false);
                            this.isUpdating = false;
                        }
                    } else {
                        ProfileVersionPanel.this.updateCustomVersionFilter();
                    }
                }
            });
        }
    }

    private void updateCustomVersionFilter() {
        final Profile profile = this.editor.getProfile();
        final Set<MinecraftReleaseType> newTypes = (Sets.newHashSet(Profile.DEFAULT_RELEASE_TYPES));
        for (final ReleaseTypeCheckBox type : this.customVersionTypes) {
            if (type.isSelected()) {
                newTypes.add(type.getType());
            } else {
                newTypes.remove(type.getType());
            }
        }
        if (newTypes.equals(Profile.DEFAULT_RELEASE_TYPES)) {
            profile.setAllowedReleaseTypes(null);
        } else {
            profile.setAllowedReleaseTypes(newTypes);
        }
        this.populateVersions(this.editor.getMinecraftLauncher().getLauncher().getVersionManager().getVersions(this.editor.getProfile().getVersionFilter()));
        this.editor.getMinecraftLauncher().getLauncher().getVersionManager().removeRefreshedVersionsListener(this);
    }

    private void updateVersionSelection() {
        final Object selection = this.versionList.getSelectedItem();
        if (selection instanceof VersionSyncInfo) {
            final Version version = ((VersionSyncInfo) selection).getLatestVersion();
            this.editor.getProfile().setLastVersionId(version.getId());
        } else {
            this.editor.getProfile().setLastVersionId(null);
        }
    }

    private void populateVersions(final List<VersionSyncInfo> versions) {
        final String previous = this.editor.getProfile().getLastVersionId();
        VersionSyncInfo selected = null;
        this.versionList.removeAllItems();
        this.versionList.addItem("Use Latest Version");
        for (final VersionSyncInfo version : versions) {
            if (version.getLatestVersion().getId().equals(previous)) {
                selected = version;
            }
            this.versionList.addItem(version);
        }
        if (selected == null && !versions.isEmpty()) {
            this.versionList.setSelectedIndex(0);
        } else {
            this.versionList.setSelectedItem(selected);
        }
    }

    @Override
    public void onVersionsRefreshed(final VersionManager manager) {
        SwingUtilities.invokeLater(() -> {
            final List<VersionSyncInfo> versions = manager.getVersions(ProfileVersionPanel.this.editor.getProfile().getVersionFilter());
            ProfileVersionPanel.this.populateVersions(versions);
            ProfileVersionPanel.this.editor.getMinecraftLauncher().getLauncher().getVersionManager().removeRefreshedVersionsListener(ProfileVersionPanel.this);
        });
    }

    private static class ReleaseTypeCheckBox extends JCheckBox {
        private final MinecraftReleaseType type;

        private ReleaseTypeCheckBox(final MinecraftReleaseType type) {
            super(type.getDescription());
            this.type = type;
        }

        public MinecraftReleaseType getType() {
            return this.type;
        }
    }

    private static class VersionListRenderer extends BasicComboBoxRenderer {
        @Override
        public Component getListCellRendererComponent(final JList list, Object value, final int index, final boolean isSelected, final boolean cellHasFocus) {
            if (value instanceof VersionSyncInfo) {
                final VersionSyncInfo syncInfo = (VersionSyncInfo) value;
                final Version version = syncInfo.getLatestVersion();
                value = String.format("%s %s", version.getType().getName(), version.getId());
            }
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            return this;
        }
    }
}
