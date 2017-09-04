package net.minecraft.launcher.ui.popups.login;

import com.google.common.base.Objects;
import com.mojang.authlib.UserAuthentication;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.util.UUIDTypeAdapter;
import net.minecraft.launcher.profile.AuthenticationDatabase;
import net.minecraft.launcher.profile.ProfileManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

public class ExistingUserListForm
        extends JPanel
        implements ActionListener {
    private static final Logger LOGGER;

    static {
        LOGGER = LogManager.getLogger();
    }

    private final LogInPopup popup;
    private final JComboBox userDropdown = new JComboBox();
    private final AuthenticationDatabase authDatabase;
    private final JButton playButton = new JButton("Play");
    private final JButton logOutButton = new JButton("Log Out");
    private final ProfileManager profileManager;

    public ExistingUserListForm(LogInPopup popup) {
        this.popup = popup;
        this.profileManager = popup.getMinecraftLauncher().getProfileManager();
        this.authDatabase = popup.getMinecraftLauncher().getProfileManager().getAuthDatabase();

        fillUsers();
        createInterface();

        this.playButton.addActionListener(this);
        this.logOutButton.addActionListener(this);
    }

    private void fillUsers() {
        for (String user : this.authDatabase.getKnownNames()) {
            this.userDropdown.addItem(user);
            if ((this.profileManager.getSelectedUser() != null) && (Objects.equal(this.authDatabase.getByUUID(this.profileManager.getSelectedUser()), this.authDatabase.getByName(user)))) {
                this.userDropdown.setSelectedItem(user);
            }
        }
    }

    protected void createInterface() {
        setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = 2;
        constraints.gridx = 0;
        constraints.gridy = -1;
        constraints.gridwidth = 2;
        constraints.weightx = 1.0D;

        add(Box.createGlue());

        String currentUser = this.authDatabase.getKnownNames().size() + " different users";
        String thisOrThese = this.authDatabase.getKnownNames().size() == 1 ? "this account" : "one of these accounts";
        add(new JLabel("You're already logged in as " + currentUser + "."), constraints);
        add(new JLabel("You may use " + thisOrThese + " and skip authentication."), constraints);

        add(Box.createVerticalStrut(5), constraints);

        JLabel usernameLabel = new JLabel("Existing User:");
        Font labelFont = usernameLabel.getFont().deriveFont(1);

        usernameLabel.setFont(labelFont);
        add(usernameLabel, constraints);

        constraints.gridwidth = 1;
        add(this.userDropdown, constraints);

        constraints.gridx = 1;
        constraints.gridy = 5;
        constraints.weightx = 0.0D;
        constraints.insets = new Insets(0, 5, 0, 0);
        add(this.playButton, constraints);
        constraints.gridx = 2;
        add(this.logOutButton, constraints);
        constraints.insets = new Insets(0, 0, 0, 0);
        constraints.weightx = 1.0D;
        constraints.gridx = 0;
        constraints.gridy = -1;

        constraints.gridwidth = 2;

        add(Box.createVerticalStrut(5), constraints);
        add(new JLabel("Alternatively, log in with a new account below:"), constraints);
        add(new JPopupMenu.Separator(), constraints);
    }

    public void actionPerformed(ActionEvent e) {
        final Object selected = this.userDropdown.getSelectedItem();
        final UserAuthentication auth;
        final String uuid;
        if ((selected != null) && ((selected instanceof String))) {
            auth = this.authDatabase.getByName((String) selected);
            if (auth.getSelectedProfile() == null) {
                uuid = "demo-" + auth.getUserID();
            } else {
                uuid = UUIDTypeAdapter.fromUUID(auth.getSelectedProfile().getId());
            }
        } else {
            auth = null;
            uuid = null;
        }
        if (e.getSource() == this.playButton) {
            this.popup.setCanLogIn(false);

            this.popup.getMinecraftLauncher().getLauncher().getVersionManager().getExecutorService().execute(new Runnable() {
                public void run() {
                    if ((auth != null) && (uuid != null)) {
                        try {
                            if (!auth.canPlayOnline()) {
                                auth.logIn();
                            }
                            ExistingUserListForm.this.popup.setLoggedIn(uuid);
                        } catch (AuthenticationException ex) {
                            ExistingUserListForm.this.popup.getErrorForm().displayError(ex, new String[]{"We couldn't log you back in as " + selected + ".", "Please try to log in again."});

                            ExistingUserListForm.this.removeUser((String) selected, uuid);

                            ExistingUserListForm.this.popup.setCanLogIn(true);
                        }
                    } else {
                        ExistingUserListForm.this.popup.setCanLogIn(true);
                    }
                }
            });
        } else if (e.getSource() == this.logOutButton) {
            removeUser((String) selected, uuid);
        }
    }

    protected void removeUser(final String name, final String uuid) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    ExistingUserListForm.this.removeUser(name, uuid);
                }
            });
        } else {
            this.userDropdown.removeItem(name);
            this.authDatabase.removeUUID(uuid);
            try {
                this.profileManager.saveProfiles();
            } catch (IOException e) {
                LOGGER.error("Couldn't save profiles whilst removing " + name + " / " + uuid + " from database", e);
            }
            if (this.userDropdown.getItemCount() == 0) {
                this.popup.remove(this);
            }
        }
    }
}