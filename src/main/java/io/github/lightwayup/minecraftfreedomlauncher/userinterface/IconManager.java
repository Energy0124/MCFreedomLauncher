package io.github.lightwayup.minecraftfreedomlauncher.userinterface;

import io.github.lightwayup.minecraftfreedomlauncher.utility.LauncherShutdown;
import net.minecraft.launcher.LauncherConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;

public class IconManager {
    private static final Logger LOGGER = LogManager.getLogger();
    private static Image IMAGE = null;
    private static boolean ATTEMPTED = false;

    private static void initializeImage() {
        InputStream inputStream = IconManager.class.getResourceAsStream(LauncherConstants.IMAGE_FAVICON);
        if (inputStream != null) {
            try {
                IMAGE = ImageIO.read(inputStream);
            } catch (IOException exception1) {
                LOGGER.error("Unable to read input stream of favicon image");
            } finally {
                try {
                    inputStream.close();
                } catch (IOException exception2) {
                    LOGGER.error("Unable to close input stream of favicon image");
                }
            }
        } else {
            LOGGER.error("Unable to get favicon image as input stream");
        }
        ATTEMPTED = true;
    }

    public static ImageIcon getIcon() {
        if (getImage() != null) {
            return new ImageIcon(getImage());
        } else {
            return null;
        }
    }

    private static Image getImage() {
        if (!ATTEMPTED) {
            initializeImage();
        }
        if (IMAGE != null) {
            return IMAGE.getScaledInstance(64, 64, Image.SCALE_SMOOTH);
        } else {
            JOptionPane.showMessageDialog(null, LauncherConstants.MESSAGE_UNKNOWN_ERROR, new LauncherConstants().windowTitle, JOptionPane.ERROR_MESSAGE);
            LauncherShutdown.forcefullyShutdown("favicon can't be loaded");
            return null;
        }
    }
}
