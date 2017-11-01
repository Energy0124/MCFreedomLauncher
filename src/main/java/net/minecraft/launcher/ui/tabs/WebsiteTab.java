package net.minecraft.launcher.ui.tabs;

import io.github.lightwayup.minecraftfreedomlauncher.userinterface.DialogDisplay;
import io.github.lightwayup.minecraftfreedomlauncher.utility.LauncherShutdown;
import net.minecraft.launcher.Launcher;
import net.minecraft.launcher.ui.tabs.website.Browser;
import net.minecraft.launcher.ui.tabs.website.JFXBrowser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

import static net.minecraft.launcher.LauncherConstants.MESSAGE_JAVAFX_UNAVAILABLE;

public class WebsiteTab extends JPanel {
    private static final Logger LOGGER = LogManager.getLogger();

    private final Browser browser;
    private final Launcher minecraftLauncher;

    public WebsiteTab(final Launcher minecraftLauncher) {
        this.browser = this.selectBrowser();
        if (browser == null) {
            DialogDisplay.showError(MESSAGE_JAVAFX_UNAVAILABLE);
            LauncherShutdown.forcefullyShutdown("JavaFX is unavailable");
        }
        this.minecraftLauncher = minecraftLauncher;
        this.setLayout(new BorderLayout());
        try {
            this.add(this.browser.getComponent(), "Center");
        } catch (NullPointerException e) {
            LOGGER.debug("A NullPointerException is caught!");
        }
        try {
            this.browser.resize(this.getSize());
        } catch (NullPointerException e) {
            LOGGER.debug("A NullPointerException is caught!");
        }
        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(final ComponentEvent e) {
                WebsiteTab.this.browser.resize(e.getComponent().getSize());
            }
        });
    }

    public static void addToSystemClassLoader(final File file) {
        if (ClassLoader.getSystemClassLoader() instanceof URLClassLoader) {
            final URLClassLoader classLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
            try {
                final Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
                method.setAccessible(true);
                method.invoke(classLoader, file.toURI().toURL());
            } catch (Throwable t) {
                WebsiteTab.LOGGER.warn("Couldn't add " + file + " to system classloader", t);
            }
        }
    }

    private Browser selectBrowser() {
        if (this.hasJFX()) {
            WebsiteTab.LOGGER.info("JFX is already initialized");
            return new JFXBrowser();
        }
        final File jfxrt = new File(System.getProperty("java.home"), "lib/jfxrt.jar");
        if (jfxrt.isFile()) {
            WebsiteTab.LOGGER.debug("Attempting to load {}...", jfxrt);
            int i = 0;
            while (i < 3) {
                try {
                    addToSystemClassLoader(jfxrt);
                    WebsiteTab.LOGGER.info("JFX has been detected & successfully loaded");
                    return new JFXBrowser();
                } catch (Throwable e) {
                    WebsiteTab.LOGGER.debug("JFX has been detected but unsuccessfully loaded", e);
                    ++i;
                }
            }
            WebsiteTab.LOGGER.fatal("JFX has been detected but unsuccessfully loaded too many times, goodbye");
            return null;
        }
        WebsiteTab.LOGGER.fatal("JFX was not found at {}, goodbye", jfxrt);
        return null;
    }

    public void setPage(final String url) {
        this.browser.loadUrl(url);
    }

    public Launcher getMinecraftLauncher() {
        return this.minecraftLauncher;
    }

    public boolean hasJFX() {
        try {
            this.getClass().getClassLoader().loadClass("javafx.embed.swing.JFXPanel");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
