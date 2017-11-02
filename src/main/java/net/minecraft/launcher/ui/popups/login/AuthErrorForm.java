package net.minecraft.launcher.ui.popups.login;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.launcher.Http;
import com.mojang.launcher.updater.LowerCaseEnumTypeAdapterFactory;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.net.URL;
import java.util.Map;

import static net.minecraft.launcher.LauncherConstants.*;

public class AuthErrorForm extends JPanel {
    private static final Logger LOGGER = LogManager.getLogger();

    private final LogInPopup popup;
    private final JLabel errorLabel;
    private final Gson gson;

    public AuthErrorForm(final LogInPopup popup) {
        this.errorLabel = new JLabel();
        this.gson = new GsonBuilder().registerTypeAdapterFactory(new LowerCaseEnumTypeAdapterFactory()).create();
        this.popup = popup;
        this.createInterface();
        this.clear();
    }

    private void createInterface() {
        this.setBorder(new EmptyBorder(0, 0, 15, 0));
        this.setPreferredSize(new Dimension(924, 100));
        this.errorLabel.setFont(this.errorLabel.getFont().deriveFont(Font.PLAIN));
        this.add(this.errorLabel);
    }

    private void clear() {
        this.setVisible(false);
    }

    @Override
    public void setVisible(final boolean value) {
        super.setVisible(value);
        this.popup.repack();
    }

    public void displayError(final Throwable throwable, final String... lines) {
        if (SwingUtilities.isEventDispatchThread()) {
            StringBuilder error = new StringBuilder();
            for (final String line : lines) {
                error.append("<p>").append(line).append("</p>");
            }
            if (throwable != null) {
                error.append("<p>(").append(ExceptionUtils.getRootCauseMessage(throwable)).append(")</p>");
            }
            this.errorLabel.setText("<html><div style='text-align: center;'>" + error + " </div></html>");
            if (!this.isVisible()) {
                this.refreshStatuses();
            }
            this.setVisible(true);
        } else {
            SwingUtilities.invokeLater(() -> AuthErrorForm.this.displayError(throwable, lines));
        }
    }

    private void refreshStatuses() {
        this.popup.getMinecraftLauncher().getLauncher().getVersionManager().getExecutorService().submit(() -> {
            try {
                final TypeToken<Map<String, ServerStatus>> token = new TypeToken<>() {
                };
                final Map<String, ServerStatus> statuses = AuthErrorForm.this.gson.fromJson(Http.performGet(new URL(URL_STATUS_CHECKER + URL_MOJANG_AUTH_SERVER_WITHOUT_HTTPS_PREFIX), AuthErrorForm.this.popup.getMinecraftLauncher().getLauncher().getProxy()), token.getType());
                if (statuses.get(URL_MOJANG_AUTH_SERVER_WITHOUT_HTTPS_PREFIX) == ServerStatus.RED) {
                    AuthErrorForm.this.displayError(null, MESSAGE_SERVER_DOWN, MESSAGE_SERVER_DOWN_FIXING, MESSAGE_TRY_AGAIN_LATER);
                }
            } catch (Exception ex) {
                LOGGER.debug("An Exception is caught!");
            }
        });
    }

    public enum ServerStatus {
        GREEN("Online, no problems detected."),
        YELLOW("May be experiencing issues."),
        RED("Offline, experiencing problems.");

        private final String title;

        ServerStatus(final String title) {
            this.title = title;
        }
    }
}
