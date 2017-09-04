package net.minecraft.launcher.ui.bottombar;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.launcher.Http;
import com.mojang.launcher.updater.LowerCaseEnumTypeAdapterFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.net.URL;
import java.util.List;
import java.util.Map;

public class StatusPanelForm
        extends SidebarGridForm {
    private static final Logger LOGGER;
    private static final String SERVER_SESSION = "session.minecraft.net";
    private static final String SERVER_LOGIN = "login.minecraft.net";

    static {
        LOGGER = LogManager.getLogger();
    }

    private final net.minecraft.launcher.Launcher minecraftLauncher;
    private final JLabel sessionStatus = new JLabel("???");
    private final JLabel loginStatus = new JLabel("???");
    private final Gson gson = new GsonBuilder().registerTypeAdapterFactory(new LowerCaseEnumTypeAdapterFactory()).create();

    public StatusPanelForm(net.minecraft.launcher.Launcher minecraftLauncher) {
        this.minecraftLauncher = minecraftLauncher;

        createInterface();
        refreshStatuses();
    }

    protected void populateGrid(GridBagConstraints constraints) {
        add(new JLabel("Multiplayer:", 2), constraints, 0, 0, 0, 1, 17);
        add(this.sessionStatus, constraints, 1, 0, 1, 1);

        add(new JLabel("Login:", 2), constraints, 0, 1, 0, 1, 17);
        add(this.loginStatus, constraints, 1, 1, 1, 1);
    }

    public JLabel getSessionStatus() {
        return this.sessionStatus;
    }

    public JLabel getLoginStatus() {
        return this.loginStatus;
    }

    public void refreshStatuses() {
        this.minecraftLauncher.getLauncher().getVersionManager().getExecutorService().submit(new Runnable() {
            public void run() {
                try {
                    TypeToken<List<Map<String, StatusPanelForm.ServerStatus>>> token = new TypeToken() {
                    };
                    List<Map<String, StatusPanelForm.ServerStatus>> statuses = (List) StatusPanelForm.this.gson.fromJson(Http.performGet(new URL("http://status.mojang.com/check"), StatusPanelForm.this.minecraftLauncher.getLauncher().getProxy()), token.getType());
                    for (Map<String, StatusPanelForm.ServerStatus> serverStatusInformation : statuses) {
                        if (serverStatusInformation.containsKey("login.minecraft.net")) {
                            StatusPanelForm.this.loginStatus.setText(((ServerStatus) serverStatusInformation.get("login.minecraft.net")).title);
                        } else if (serverStatusInformation.containsKey("session.minecraft.net")) {
                            StatusPanelForm.this.sessionStatus.setText(((ServerStatus) serverStatusInformation.get("session.minecraft.net")).title);
                        }
                    }
                } catch (Exception e) {
                    StatusPanelForm.LOGGER.error("Couldn't get server status", e);
                }
            }
        });
    }

    public static enum ServerStatus {
        GREEN("Online, no problems detected."), YELLOW("May be experiencing issues."), RED("Offline, experiencing problems.");

        private final String title;

        private ServerStatus(String title) {
            this.title = title;
        }
    }
}