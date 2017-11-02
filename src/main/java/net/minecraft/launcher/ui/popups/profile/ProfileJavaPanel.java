package net.minecraft.launcher.ui.popups.profile;

import com.mojang.launcher.OperatingSystem;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;

import static java.awt.GridBagConstraints.*;
import static net.minecraft.launcher.LauncherConstants.JVM_ARGUMENTS_DEFAULT;

class ProfileJavaPanel extends JPanel {
    private static final JCheckBox javaArgsCustom = new JCheckBox("Java Virtual Machine Arguments:");
    private static final JTextField javaArgsField = new JTextField();
    private final ProfileEditorPopup editor;
    private final JCheckBox javaPathCustom;
    private final JTextField javaPathField;

    public ProfileJavaPanel(final ProfileEditorPopup editor) {
        this.javaPathCustom = new JCheckBox("Executable:");
        this.javaPathField = new JTextField();
        this.editor = editor;
        this.setLayout(new GridBagLayout());
        this.setBorder(BorderFactory.createTitledBorder("Java Settings"));
        this.createInterface();
        this.fillDefaultValues();
        this.addEventHandlers();
    }

    private void createInterface() {
        final GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(2, 2, 2, 2);
        constraints.anchor = WEST;
        constraints.gridy = 0;
        this.add(this.javaPathCustom, constraints);
        constraints.fill = HORIZONTAL;
        constraints.weightx = 1.0;
        this.add(this.javaPathField, constraints);
        constraints.weightx = 0.0;
        constraints.fill = NONE;
        ++constraints.gridy;
        this.add(javaArgsCustom, constraints);
        constraints.fill = HORIZONTAL;
        constraints.weightx = 1.0;
        this.add(javaArgsField, constraints);
        constraints.weightx = 0.0;
        constraints.fill = NONE;
        ++constraints.gridy;
    }

    private void fillDefaultValues() {
        final String javaPath = this.editor.getProfile().getJavaPath();
        if (javaPath != null) {
            this.javaPathCustom.setSelected(true);
            this.javaPathField.setText(javaPath);
        } else {
            this.javaPathCustom.setSelected(false);
            this.javaPathField.setText(OperatingSystem.getCurrentPlatform().getJavaDir());
        }
        this.updateJavaPathState();
        final String args = this.editor.getProfile().getJavaArgs();
        if (args != null) {
            javaArgsCustom.setSelected(true);
            javaArgsField.setText(args);
        } else {
            javaArgsCustom.setSelected(false);
            javaArgsField.setText(JVM_ARGUMENTS_DEFAULT);
        }
        this.updateJavaArgsState();
    }

    private void addEventHandlers() {
        this.javaPathCustom.addItemListener(e -> ProfileJavaPanel.this.updateJavaPathState());
        this.javaPathField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(final DocumentEvent e) {
                ProfileJavaPanel.this.updateJavaPath();
            }

            @Override
            public void removeUpdate(final DocumentEvent e) {
                ProfileJavaPanel.this.updateJavaPath();
            }

            @Override
            public void changedUpdate(final DocumentEvent e) {
                ProfileJavaPanel.this.updateJavaPath();
            }
        });
        javaArgsCustom.addItemListener(e -> ProfileJavaPanel.this.updateJavaArgsState());
        javaArgsField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(final DocumentEvent e) {
                ProfileJavaPanel.this.updateJavaArgs();
            }

            @Override
            public void removeUpdate(final DocumentEvent e) {
                ProfileJavaPanel.this.updateJavaArgs();
            }

            @Override
            public void changedUpdate(final DocumentEvent e) {
                ProfileJavaPanel.this.updateJavaArgs();
            }
        });
    }

    private void updateJavaPath() {
        if (this.javaPathCustom.isSelected()) {
            this.editor.getProfile().setJavaDir(this.javaPathField.getText());
        } else {
            this.editor.getProfile().setJavaDir(null);
        }
    }

    private void updateJavaPathState() {
        if (this.javaPathCustom.isSelected()) {
            this.javaPathField.setEnabled(true);
            this.editor.getProfile().setJavaDir(this.javaPathField.getText());
        } else {
            this.javaPathField.setEnabled(false);
            this.editor.getProfile().setJavaDir(null);
        }
    }

    private void updateJavaArgs() {
        if (javaArgsCustom.isSelected()) {
            this.editor.getProfile().setJavaArgs(javaArgsField.getText());
        } else {
            this.editor.getProfile().setJavaArgs(null);
        }
    }

    private void updateJavaArgsState() {
        if (javaArgsCustom.isSelected()) {
            javaArgsField.setEnabled(true);
            this.editor.getProfile().setJavaArgs(javaArgsField.getText());
        } else {
            javaArgsField.setEnabled(false);
            this.editor.getProfile().setJavaArgs(null);
        }
    }
}
