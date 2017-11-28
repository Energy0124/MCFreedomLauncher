package net.minecraft.launcher.ui.popups.login;

import com.mojang.launcher.OperatingSystem;
import net.minecraft.launcher.Launcher;
import net.minecraft.launcher.LauncherConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import static net.minecraft.launcher.LauncherConstants.*;

public class LogInPopup extends JPanel implements ActionListener {
    private static final Logger LOGGER = LogManager.getLogger();

    private static JCheckBox premiumModeCheckBox;
    private final Launcher minecraftLauncher;
    private final Callback callback;
    private final AuthErrorForm errorForm;
    private final ExistingUserListForm existingUserListForm;
    private final LogInForm logInForm;
    private final JButton loginButton;
    private final JButton registerButton;
    private final JProgressBar progressBar;

    public LogInPopup(final Launcher minecraftLauncher, final Callback callback) {
        super(true);
        premiumModeCheckBox = new JCheckBox("Premium Mode");
        this.loginButton = new JButton("Log In");
        this.registerButton = new JButton("Register");
        this.progressBar = new JProgressBar();
        this.minecraftLauncher = minecraftLauncher;
        this.callback = callback;
        this.errorForm = new AuthErrorForm(this);
        this.existingUserListForm = new ExistingUserListForm(this);
        this.logInForm = new LogInForm(this);
        this.createInterface();
        this.loginButton.addActionListener(this);
        this.registerButton.addActionListener(this);
    }

    public static boolean isPremium() {
        if (premiumModeCheckBox != null) {
            return premiumModeCheckBox.isSelected();
        } else {
            return false; // Need a proper fix
        }
    }

    private void createInterface() {
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.setBorder(new EmptyBorder(5, 15, 5, 15));
        this.setPreferredSize(new Dimension(1024, 576));
        try {
            final InputStream stream = LogInPopup.class.getResourceAsStream(IMAGE_MINECRAFT_LOGO);
            if (stream != null) {
                final BufferedImage image = ImageIO.read(stream);
                final JLabel label = new JLabel(new ImageIcon(image));
                label.setCursor(new Cursor(Cursor.HAND_CURSOR));
                label.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(final MouseEvent e) {
                        OperatingSystem.openLink(LauncherConstants.constantURI(URL_MINECRAFT));
                    }
                });
                final JPanel imagePanel = new JPanel();
                imagePanel.add(label);
                this.add(imagePanel);
                this.add(Box.createVerticalStrut(10));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            LOGGER.debug("An Exception is caught!");
        }
        if (!this.minecraftLauncher.getProfileManager().getAuthDatabase().getKnownNames().isEmpty()) {
            this.add(this.existingUserListForm);
        }
        this.add(this.errorForm);
        this.add(this.logInForm);
        this.add(Box.createVerticalStrut(15));
        final JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(1, 2, 10, 0));
        buttonPanel.add(this.registerButton);
        buttonPanel.add(this.loginButton);
        this.add(buttonPanel);
        final JPanel checkBoxPanel = new JPanel();
        checkBoxPanel.setLayout(new FlowLayout());
        checkBoxPanel.add(premiumModeCheckBox);
        // Premium mode does NOT work in 2.0.0
        // this.add(checkBoxPanel);
        this.progressBar.setIndeterminate(true);
        this.progressBar.setVisible(false);
        this.add(this.progressBar);
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        if (e.getSource() == this.loginButton) {
            this.logInForm.tryLogIn();
        } else if (e.getSource() == this.registerButton) {
            OperatingSystem.openLink(LauncherConstants.constantURI(URL_REGISTER));
        }
    }

    public Launcher getMinecraftLauncher() {
        return this.minecraftLauncher;
    }

    void setCanLogIn(final boolean enabled) {
        if (SwingUtilities.isEventDispatchThread()) {
            this.loginButton.setEnabled(enabled);
            this.progressBar.setIndeterminate(false);
            this.progressBar.setIndeterminate(true);
            this.progressBar.setVisible(!enabled);
            this.repack();
        } else {
            SwingUtilities.invokeLater(() -> LogInPopup.this.setCanLogIn(enabled));
        }
    }

    public LogInForm getLogInForm() {
        return this.logInForm;
    }

    AuthErrorForm getErrorForm() {
        return this.errorForm;
    }

    public ExistingUserListForm getExistingUserListForm() {
        return this.existingUserListForm;
    }

    void setLoggedIn(final String uuid) {
        this.callback.onLogIn(uuid);
    }

    void repack() {
        final Window window = SwingUtilities.windowForComponent(this);
        if (window != null) {
            window.pack();
        }
    }

    public interface Callback {
        void onLogIn(final String p0);
    }
}
