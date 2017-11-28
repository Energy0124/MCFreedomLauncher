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

    public static ImageIcon getIcon(int width, int height, int scaling) {
        Image scaledImage = getImage(width, height, scaling);
        if (scaledImage != null) {
            return new ImageIcon(scaledImage);
        } else {
            return null;
        }
    }

    public static Image getImage() {
        Image defaultScaledImage = getImage(64, 64, Image.SCALE_SMOOTH);
        if (defaultScaledImage != null) {
            return defaultScaledImage;
        } else {
            return null;
        }
    }

    public static Image getImage(int width, int height, int scaling) {
        if (!ATTEMPTED) {
            initializeImage();
        }
        if (IMAGE != null) {
            return IMAGE.getScaledInstance(width, height, scaling);
        } else {
            JOptionPane.showMessageDialog(null, LauncherConstants.MESSAGE_UNKNOWN_ERROR, LauncherConstants.getTitle(), JOptionPane.ERROR_MESSAGE);
            LauncherShutdown.forcefullyShutdown("favicon can't be loaded");
            return null;
        }
    }
}
