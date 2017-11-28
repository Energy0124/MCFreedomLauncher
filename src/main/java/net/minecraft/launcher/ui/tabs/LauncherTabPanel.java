package net.minecraft.launcher.ui.tabs;

import io.github.lightwayup.minecraftfreedomlauncher.userinterface.IconManager;
import net.minecraft.launcher.Launcher;

import javax.swing.*;
import java.awt.*;

public class LauncherTabPanel extends JTabbedPane {
    private final Launcher minecraftLauncher;
    private final WebsiteTab blog;
    private final ConsoleTab console;
    private final ProfileListTab profile;
    private final WebsiteTab releases;
    private final VersionInfoTab versionInfo;
    private CrashReportTab crashReportTab;

    public LauncherTabPanel(final Launcher minecraftLauncher) {
        super(TOP);
        this.minecraftLauncher = minecraftLauncher;
        this.blog = new WebsiteTab(minecraftLauncher);
        this.console = new ConsoleTab(minecraftLauncher);
        this.profile = new ProfileListTab(minecraftLauncher);
        this.releases = new WebsiteTab(minecraftLauncher);
        this.versionInfo = new VersionInfoTab(minecraftLauncher);
        this.createInterface();
    }

    private void createInterface() {
        ImageIcon icon = IconManager.getIcon(16, 16, Image.SCALE_SMOOTH);
        this.addTab("Update Notes", icon, this.blog, "Minecraft related news.");
        //this.addTab("Launcher Log", icon, this.console, "Logs for debugging purposes.");
        this.addTab("Profile Editor", icon, this.profile, "Overview of created profiles.");
        this.addTab("Launcher Releases", icon, this.releases, "Release notes of Minecraft Freedom Launcher.");
        //this.addTab("Version Info", icon, this.versionInfo, "Details of launcher and included libraries.");
    }

    public Launcher getMinecraftLauncher() {
        return this.minecraftLauncher;
    }

    public WebsiteTab getBlog() {
        return this.blog;
    }

    public WebsiteTab getReleases() {
        return this.releases;
    }

    public ConsoleTab getConsole() {
        return this.console;
    }

    public void showConsole() {
        this.setSelectedComponent(this.console);
    }

    public void setCrashReport(final CrashReportTab newTab) {
        if (this.crashReportTab != null) {
            this.removeTab(this.crashReportTab);
        }
        this.addTab("Crash Report", this.crashReportTab = newTab);
        this.setSelectedComponent(newTab);
    }

    private void removeTab(final Component tab) {
        for (int i = 0; i < this.getTabCount(); ++i) {
            if (this.getTabComponentAt(i) == tab) {
                this.removeTabAt(i);
                break;
            }
        }
    }

    public void removeTab(final String name) {
        final int index = this.indexOfTab(name);
        if (index > -1) {
            this.removeTabAt(index);
        }
    }
}
