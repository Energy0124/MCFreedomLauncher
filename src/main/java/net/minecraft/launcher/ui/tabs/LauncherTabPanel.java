package net.minecraft.launcher.ui.tabs;

import net.minecraft.launcher.Launcher;

import javax.swing.*;
import java.awt.*;

public class LauncherTabPanel extends JTabbedPane {
    private final Launcher minecraftLauncher;
    private final WebsiteTab blog;
    private final ConsoleTab console;
    private final WebsiteTab releases;
    private CrashReportTab crashReportTab;

    public LauncherTabPanel(final Launcher minecraftLauncher) {
        super(TOP);
        this.minecraftLauncher = minecraftLauncher;
        this.blog = new WebsiteTab(minecraftLauncher);
        this.console = new ConsoleTab(minecraftLauncher);
        this.releases = new WebsiteTab(minecraftLauncher);
        this.createInterface();
    }

    private void createInterface() {
        this.addTab("Update Notes", this.blog);
        // Launcher log isn't displayed for some reason...
        // this.addTab("Launcher Log", this.console);
        this.addTab("Profile Editor", new ProfileListTab(this.minecraftLauncher));
        this.addTab("Launcher Releases", this.releases);
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
