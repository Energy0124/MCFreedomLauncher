package io.github.lightwayup.minecraftfreedomlauncher.userinterface;

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
    private static Image IMAGE;
    private static InputStream ICONSTREAM;
    private static boolean IS_ICONSTREAM_OPEN = false;

    private static void initializeImage() throws IOException, IllegalArgumentException {
        ICONSTREAM = IconManager.class.getResourceAsStream(LauncherConstants.IMAGE_FAVICON);
        IS_ICONSTREAM_OPEN = true;
        IMAGE = ImageIO.read(ICONSTREAM);
        LOGGER.debug("Favicon image initialized");
    }

    public static ImageIcon getIcon() throws IOException, IllegalArgumentException {
        return new ImageIcon(getImage());
    }

    public static ImageIcon getIcon(int scaledWidth, int scaledHeight, int scalingMode) throws IOException, IllegalArgumentException {
        return new ImageIcon(getImage(scaledWidth, scaledHeight, scalingMode));
    }

    public static Image getImage() throws IOException, IllegalArgumentException {
        return getImage(64, 64, Image.SCALE_SMOOTH);
    }

    public static Image getImage(int scaledWidth, int scaledHeight, int scalingMode) throws IOException, IllegalArgumentException {
        if (!IS_ICONSTREAM_OPEN) {
            initializeImage();
        }
        return IMAGE.getScaledInstance(scaledWidth, scaledHeight, scalingMode);
    }

    public static void closeIconStream() throws IOException {
        if (IS_ICONSTREAM_OPEN) {
            IMAGE = null;
            ICONSTREAM.close();
            IS_ICONSTREAM_OPEN = false;
        }
    }
}
