package net.minecraft.launcher.ui.popups.login;

import com.mojang.authlib.Agent;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.UserAuthentication;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.exceptions.InvalidCredentialsException;
import com.mojang.authlib.exceptions.UserMigratedException;
import com.mojang.launcher.OperatingSystem;
import com.mojang.util.UUIDTypeAdapter;
import net.minecraft.launcher.LauncherConstants;
import net.minecraft.launcher.profile.AuthenticationDatabase;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import static java.awt.GridBagConstraints.HORIZONTAL;
import static net.minecraft.launcher.LauncherConstants.*;

public class LogInForm extends JPanel implements ActionListener {
    private static final Logger LOGGER = LogManager.getLogger();

    private final LogInPopup popup;
    private final JTextField usernameField;
    private final JPasswordField passwordField;
    private final JComboBox userDropdown;
    private final JPanel userDropdownPanel;
    private final UserAuthentication authentication;

    public LogInForm(final LogInPopup popup) {
        this.usernameField = new JTextField();
        this.passwordField = new JPasswordField();
        this.userDropdown = new JComboBox();
        this.userDropdownPanel = new JPanel();
        this.popup = popup;
        this.authentication = popup.getMinecraftLauncher().getProfileManager().getAuthDatabase().getAuthenticationService().createUserAuthentication(Agent.MINECRAFT);
        this.usernameField.addActionListener(this);
        this.passwordField.addActionListener(this);
        this.createInterface();
    }

    protected void createInterface() {
        this.setLayout(new GridBagLayout());
        final GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = HORIZONTAL;
        constraints.gridx = 0;
        constraints.gridy = -1;
        constraints.weightx = 1.0;
        this.add(Box.createGlue());
        final JLabel usernameLabel = new JLabel("Email Address or Username:");
        final Font labelFont = usernameLabel.getFont().deriveFont(Font.PLAIN);
        final Font smalltextFont = usernameLabel.getFont().deriveFont(labelFont.getSize() - 2.0f);
        usernameLabel.setFont(labelFont);
        this.add(usernameLabel, constraints);
        this.add(this.usernameField, constraints);
        final JLabel forgotUsernameLabel = new JLabel("Which to Use?");
        forgotUsernameLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        forgotUsernameLabel.setFont(smalltextFont);
        forgotUsernameLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        forgotUsernameLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                OperatingSystem.openLink(LauncherConstants.constantURI(URL_FORGOT_USERNAME));
            }
        });
        this.add(forgotUsernameLabel, constraints);
        this.add(Box.createVerticalStrut(10), constraints);
        final JLabel forgotMigratedEmailLabel = new JLabel("Forgot Migrated Email");
        forgotMigratedEmailLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        forgotMigratedEmailLabel.setFont(smalltextFont);
        forgotMigratedEmailLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        forgotMigratedEmailLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                OperatingSystem.openLink(LauncherConstants.constantURI(URL_FORGOT_MIGRATED_EMAIL));
            }
        });
        this.add(forgotMigratedEmailLabel, constraints);
        this.add(Box.createVerticalStrut(10), constraints);
        final JLabel passwordLabel = new JLabel("Password (Leave Blank if Cracked):");
        passwordLabel.setFont(labelFont);
        this.add(passwordLabel, constraints);
        this.add(this.passwordField, constraints);
        final JLabel forgotPasswordLabel = new JLabel("Forgot Password");
        forgotPasswordLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        forgotPasswordLabel.setFont(smalltextFont);
        forgotPasswordLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        forgotPasswordLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                OperatingSystem.openLink(LauncherConstants.constantURI(URL_FORGOT_PASSWORD_MINECRAFT));
            }
        });
        this.add(forgotPasswordLabel, constraints);
        this.add(this.userDropdownPanel, constraints);
        this.add(Box.createVerticalStrut(10), constraints);
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        if (e.getSource() == this.usernameField || e.getSource() == this.passwordField) {
            this.tryLogIn();
        }
    }

    public void tryLogIn() {
        if (this.authentication.isLoggedIn() && this.authentication.getSelectedProfile() == null && ArrayUtils.isNotEmpty(this.authentication.getAvailableProfiles())) {
            this.popup.setCanLogIn(false);
            GameProfile selectedProfile = null;
            for (final GameProfile profile : this.authentication.getAvailableProfiles()) {
                if (profile.getName().equals(this.userDropdown.getSelectedItem())) {
                    selectedProfile = profile;
                    break;
                }
            }
            if (selectedProfile == null) {
                selectedProfile = this.authentication.getAvailableProfiles()[0];
            }
            final GameProfile finalSelectedProfile = selectedProfile;
            this.popup.getMinecraftLauncher().getLauncher().getVersionManager().getExecutorService().execute(() -> {
                try {
                    LogInForm.this.authentication.selectGameProfile(finalSelectedProfile);
                    LogInForm.this.popup.getMinecraftLauncher().getProfileManager().getAuthDatabase().register(UUIDTypeAdapter.fromUUID(LogInForm.this.authentication.getSelectedProfile().getId()), LogInForm.this.authentication);
                    LogInForm.this.popup.setLoggedIn(UUIDTypeAdapter.fromUUID(LogInForm.this.authentication.getSelectedProfile().getId()));
                } catch (InvalidCredentialsException ex) {
                    LogInForm.LOGGER.error("Couldn't log in", ex);
                    LogInForm.this.popup.getErrorForm().displayError(ex, MESSAGE_CANNOT_LOG_IN, MESSAGE_TRY_AGAIN_LATER);
                    LogInForm.this.popup.setCanLogIn(true);
                } catch (AuthenticationException ex2) {
                    LogInForm.LOGGER.error("Couldn't log in", ex2);
                    LogInForm.this.popup.getErrorForm().displayError(ex2, MESSAGE_CANNOT_CONNECT_ONE, MESSAGE_CANNOT_CONNECT_TWO);
                    LogInForm.this.popup.setCanLogIn(true);
                }
            });
        } else {
            this.popup.setCanLogIn(false);
            this.authentication.logOut();
            this.authentication.setUsername(this.usernameField.getText());
            this.authentication.setPassword(String.valueOf(this.passwordField.getPassword()));
            final int passwordLength = this.passwordField.getPassword().length;
            this.passwordField.setText("");
            this.popup.getMinecraftLauncher().getLauncher().getVersionManager().getExecutorService().execute(() -> {
                try {
                    LogInForm.this.authentication.logIn();
                    final AuthenticationDatabase authDatabase = LogInForm.this.popup.getMinecraftLauncher().getProfileManager().getAuthDatabase();
                    if (LogInForm.this.authentication.getSelectedProfile() == null) {
                        if (ArrayUtils.isNotEmpty(LogInForm.this.authentication.getAvailableProfiles())) {
                            for (final GameProfile profile : LogInForm.this.authentication.getAvailableProfiles()) {
                                LogInForm.this.userDropdown.addItem(profile.getName());
                            }
                            SwingUtilities.invokeLater(() -> {
                                LogInForm.this.usernameField.setEditable(false);
                                LogInForm.this.passwordField.setEditable(false);
                                LogInForm.this.userDropdownPanel.setVisible(true);
                                LogInForm.this.popup.repack();
                                LogInForm.this.popup.setCanLogIn(true);
                                LogInForm.this.passwordField.setText(StringUtils.repeat('*', passwordLength));
                            });
                        } else {
                            final String uuid = "demo-" + LogInForm.this.authentication.getUserID();
                            authDatabase.register(uuid, LogInForm.this.authentication);
                            LogInForm.this.popup.setLoggedIn(uuid);
                        }
                    } else {
                        authDatabase.register(UUIDTypeAdapter.fromUUID(LogInForm.this.authentication.getSelectedProfile().getId()), LogInForm.this.authentication);
                        LogInForm.this.popup.setLoggedIn(UUIDTypeAdapter.fromUUID(LogInForm.this.authentication.getSelectedProfile().getId()));
                    }
                } catch (UserMigratedException ex) {
                    LogInForm.LOGGER.error("Couldn't log in", ex);
                    LogInForm.this.popup.getErrorForm().displayError(ex, MESSAGE_CANNOT_LOG_IN_WITH_USERNAME, MESSAGE_USE_EMAIL);
                    LogInForm.this.popup.setCanLogIn(true);
                } catch (InvalidCredentialsException ex2) {
                    LogInForm.LOGGER.error("Couldn't log in", ex2);
                    LogInForm.this.popup.getErrorForm().displayError(ex2, MESSAGE_USERNAME_OR_PASSWORD_INCORRECT, MESSAGE_TRY_AGAIN);
                    LogInForm.this.popup.setCanLogIn(true);
                } catch (AuthenticationException ex3) {
                    LogInForm.LOGGER.error("Couldn't log in", ex3);
                    LogInForm.this.popup.getErrorForm().displayError(ex3, MESSAGE_CANNOT_CONNECT_ONE, MESSAGE_CANNOT_CONNECT_TWO);
                    LogInForm.this.popup.setCanLogIn(true);
                }
            });
        }
    }
}
