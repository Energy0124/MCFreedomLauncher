package net.minecraft.launcher.ui.tabs;

import net.minecraft.launcher.Launcher;
import net.minecraft.launcher.ui.tabs.website.Browser;
import net.minecraft.launcher.ui.tabs.website.JFXBrowser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.beans.IntrospectionException;
import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Timer;
import java.util.TimerTask;

public class ReleasesTab
        extends JPanel {
    private static final Logger LOGGER;

    static {
        LOGGER = LogManager.getLogger();
    }

    private final Browser browser = selectBrowser();
    private final Launcher minecraftLauncher;

    public ReleasesTab(Launcher minecraftLauncher) {
        this.minecraftLauncher = minecraftLauncher;
        if (browser == null) {
            JOptionPane.showMessageDialog(null, "JavaFX can't be loaded. Please check if JavaFX is present.", "Unable to load JavaFX", JOptionPane.ERROR_MESSAGE);
            try {
                java.util.Timer ignored = new Timer();
                ignored.schedule(new TimerTask() {
                    public void run() {
                        Runtime.getRuntime().halt(0);
                    }
                }, 10000L);
                System.exit(0);
            } catch (Throwable var2) {
                Runtime.getRuntime().halt(0);
            }
        }

        setLayout(new BorderLayout());
        add(this.browser.getComponent(), "Center");
        this.browser.resize(getSize());

        addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                ReleasesTab.this.browser.resize(e.getComponent().getSize());
            }
        });
    }

    public static void addToSystemClassLoader(File file)
            throws IntrospectionException {
        if ((ClassLoader.getSystemClassLoader() instanceof URLClassLoader)) {
            URLClassLoader classLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
            try {
                Method method = URLClassLoader.class.getDeclaredMethod("addURL", new Class[]{URL.class});
                method.setAccessible(true);
                method.invoke(classLoader, file.toURI().toURL());
            } catch (Throwable t) {
                LOGGER.warn("Couldn't add " + file + " to system classloader", t);
            }
        }
    }

    private Browser selectBrowser() {
        if (hasJFX()) {
            LOGGER.info("JFX is already initialized");
            return new JFXBrowser();
        }
        File jfxrt = new File(System.getProperty("java.home"), "lib/jfxrt.jar");
        if (jfxrt.isFile()) {
            LOGGER.debug("Attempting to load {}...", jfxrt);
            int i = 0;
            while(true) {
                try {
                    addToSystemClassLoader(jfxrt);

                    LOGGER.info("JFX has been detected & successfully loaded");
                    return new JFXBrowser();
                } catch (Throwable e) {
                    LOGGER.debug("JFX has been detected but unsuccessfully loaded, retrying...", e);
                    ++i;
                    if (i > 3) {
                        LOGGER.fatal("JFX cannot be successfully loaded, goodbye");
                        return null;
                    }
                }
            }
        }
        LOGGER.fatal("JFX was not found at {}, goodbye", jfxrt);
        return null;
    }

    public void setPage(String url) {
        this.browser.loadUrl(url);
    }

    public Launcher getMinecraftLauncher() {
        return this.minecraftLauncher;
    }

    public boolean hasJFX() {
        try {
            getClass().getClassLoader().loadClass("javafx.embed.swing.JFXPanel");
            return true;
        } catch (ClassNotFoundException e) {
        }
        return false;
    }
}