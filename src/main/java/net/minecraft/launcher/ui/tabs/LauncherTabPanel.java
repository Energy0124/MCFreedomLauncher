package net.minecraft.launcher.ui.tabs;

import net.minecraft.launcher.Launcher;

import javax.swing.*;
import java.awt.*;

public class LauncherTabPanel
        extends JTabbedPane {
    private final Launcher minecraftLauncher;
    private final WebsiteTab blog;
    private final ConsoleTab console;
	private final ReleasesTab releases;
    private CrashReportTab crashReportTab;

    public LauncherTabPanel(Launcher minecraftLauncher) {
        super(1);

        this.minecraftLauncher = minecraftLauncher;
        this.blog = new WebsiteTab(minecraftLauncher);
        this.console = new ConsoleTab(minecraftLauncher);
		this.releases = new ReleasesTab(minecraftLauncher);

        createInterface();
    }

    protected void createInterface() {
        addTab("Update Notes", this.blog);
        addTab("Launcher Log", this.console);
        addTab("Profile Editor", new ProfileListTab(this.minecraftLauncher));
		addTab("Launcher Releases", this.releases);
    }

    public Launcher getMinecraftLauncher() {
        return this.minecraftLauncher;
    }

    public WebsiteTab getBlog() {
        return this.blog;
    }
	
	public ReleasesTab getReleases() {
        return this.releases;
    }

    public ConsoleTab getConsole() {
        return this.console;
    }

    public void showConsole() {
        setSelectedComponent(this.console);
    }

    public void setCrashReport(CrashReportTab newTab) {
        if (this.crashReportTab != null) {
            removeTab(this.crashReportTab);
        }
        this.crashReportTab = newTab;
        addTab("Crash Report", this.crashReportTab);
        setSelectedComponent(newTab);
    }

    protected void removeTab(Component tab) {
        for (int i = 0; i < getTabCount(); i++) {
            if (getTabComponentAt(i) == tab) {
                removeTabAt(i);
                break;
            }
        }
    }

    public void removeTab(String name) {
        int index = indexOfTab(name);
        if (index > -1) {
            removeTabAt(index);
        }
    }
}