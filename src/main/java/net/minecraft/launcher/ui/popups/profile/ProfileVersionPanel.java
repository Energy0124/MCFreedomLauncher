package net.minecraft.launcher.ui.popups.profile;

import net.minecraft.launcher.events.RefreshedVersionsListener;
import net.minecraft.launcher.locale.LocaleHelper;
import net.minecraft.launcher.profile.Profile;
import net.minecraft.launcher.updater.VersionManager;
import net.minecraft.launcher.updater.VersionSyncInfo;
import net.minecraft.launcher.versions.ReleaseType;
import net.minecraft.launcher.versions.Version;

import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.text.MessageFormat;
import java.util.*;
import java.util.List;

public class ProfileVersionPanel extends JPanel
        implements RefreshedVersionsListener {
    private ResourceBundle resourceBundle = LocaleHelper.getMessages();
    private final ProfileEditorPopup editor;
    private final JComboBox versionList = new JComboBox();
    private final List<ReleaseTypeCheckBox> customVersionTypes = new ArrayList();

    public ProfileVersionPanel(ProfileEditorPopup editor) {
        this.editor = editor;

        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createTitledBorder(resourceBundle.getString("version.selection")));

        createInterface();
        addEventHandlers();

        List versions = editor.getLauncher().getVersionManager().getVersions(editor.getProfile().getVersionFilter());

        if (versions.isEmpty())
            editor.getLauncher().getVersionManager().addRefreshedVersionsListener(this);
        else
            populateVersions(versions);
    }

    protected void createInterface() {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(2, 2, 2, 2);
        constraints.anchor = 17;

        constraints.gridy = 0;

        for (ReleaseType type : ReleaseType.values()) {
            if (type.getDescription() != null) {
                ReleaseTypeCheckBox checkbox = new ReleaseTypeCheckBox(type);
                checkbox.setSelected(this.editor.getProfile().getVersionFilter().getTypes().contains(type));
                this.customVersionTypes.add(checkbox);

                constraints.fill = 2;
                constraints.weightx = 1.0D;
                constraints.gridwidth = 0;
                add(checkbox, constraints);
                constraints.gridwidth = 1;
                constraints.weightx = 0.0D;
                constraints.fill = 0;

                constraints.gridy += 1;
            }
        }
        add(new JLabel(resourceBundle.getString("use.version")), constraints);
        constraints.fill = 2;
        constraints.weightx = 1.0D;
        add(this.versionList, constraints);
        constraints.weightx = 0.0D;
        constraints.fill = 0;

        constraints.gridy += 1;

        this.versionList.setRenderer(new VersionListRenderer());
    }

    protected void addEventHandlers() {
        this.versionList.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                ProfileVersionPanel.this.updateVersionSelection();
            }
        });
        for (final ReleaseTypeCheckBox type : this.customVersionTypes)
            type.addItemListener(new ItemListener() {
                private boolean isUpdating = false;

                public void itemStateChanged(ItemEvent e) {
                    if (this.isUpdating) return;
                    if ((e.getStateChange() == 1) && (type.getType().getPopupWarning() != null)) {
                        int result = JOptionPane.showConfirmDialog(ProfileVersionPanel.this.editor.getLauncher().getFrame(), MessageFormat.format("{0}\n\n" + resourceBundle.getString("0.n.nare.you.sure.you.want.to.continue"), type.getType().getPopupWarning()));

                        this.isUpdating = true;
                        if (result == 0) {
                            type.setSelected(true);
                            ProfileVersionPanel.this.updateCustomVersionFilter();
                        } else {
                            type.setSelected(false);
                        }
                        this.isUpdating = false;
                    } else {
                        ProfileVersionPanel.this.updateCustomVersionFilter();
                    }
                }
            });
    }

    private void updateCustomVersionFilter() {
        Profile profile = this.editor.getProfile();
        Set newTypes = new HashSet(Profile.DEFAULT_RELEASE_TYPES);

        for (ReleaseTypeCheckBox type : this.customVersionTypes) {
            if (type.isSelected())
                newTypes.add(type.getType());
            else {
                newTypes.remove(type.getType());
            }
        }

        if (newTypes.equals(Profile.DEFAULT_RELEASE_TYPES))
            profile.setAllowedReleaseTypes(null);
        else {
            profile.setAllowedReleaseTypes(newTypes);
        }

        populateVersions(this.editor.getLauncher().getVersionManager().getVersions(this.editor.getProfile().getVersionFilter()));
        this.editor.getLauncher().getVersionManager().removeRefreshedVersionsListener(this);
    }

    private void updateVersionSelection() {
        Object selection = this.versionList.getSelectedItem();

        if ((selection instanceof VersionSyncInfo)) {
            Version version = ((VersionSyncInfo) selection).getLatestVersion();
            this.editor.getProfile().setLastVersionId(version.getId());
        } else {
            this.editor.getProfile().setLastVersionId(null);
        }
    }

    private void populateVersions(List<VersionSyncInfo> versions) {
        String previous = this.editor.getProfile().getLastVersionId();
        VersionSyncInfo selected = null;

        this.versionList.removeAllItems();
        this.versionList.addItem(resourceBundle.getString("use.latest.version"));

        for (VersionSyncInfo version : versions) {
            if (version.getLatestVersion().getId().equals(previous)) {
                selected = version;
            }

            this.versionList.addItem(version);
        }

        if ((selected == null) && (!versions.isEmpty()))
            this.versionList.setSelectedIndex(0);
        else
            this.versionList.setSelectedItem(selected);
    }

    public void onVersionsRefreshed(VersionManager manager) {
        List versions = manager.getVersions(this.editor.getProfile().getVersionFilter());
        populateVersions(versions);
        this.editor.getLauncher().getVersionManager().removeRefreshedVersionsListener(this);
    }

    public boolean shouldReceiveEventsInUIThread() {
        return true;
    }

    private static class ReleaseTypeCheckBox extends JCheckBox {
        private final ReleaseType type;

        private ReleaseTypeCheckBox(ReleaseType type) {
            super(type.getDescription());
            this.type = type;
        }

        public ReleaseType getType() {
            return this.type;
        }
    }

    private static class VersionListRenderer extends BasicComboBoxRenderer {
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            if ((value instanceof VersionSyncInfo)) {
                VersionSyncInfo syncInfo = (VersionSyncInfo) value;
                Version version = syncInfo.getLatestVersion();

                value = String.format("%s %s", new Object[]{version.getType().getName(), version.getId()});
            }

            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            return this;
        }
    }
}