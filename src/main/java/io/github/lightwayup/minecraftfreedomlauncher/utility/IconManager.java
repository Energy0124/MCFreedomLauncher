package io.github.lightwayup.minecraftfreedomlauncher.utility;

import net.minecraft.launcher.LauncherConstants;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.InputStream;

public class IconManager {
    private static ImageIcon ICON;
    private static Image IMAGE;
    private static InputStream ICONSTREAM;
    private static boolean IS_ICONSTREAM_OPEN = false;

    public static void initializeIcon() throws Exception {
        ICONSTREAM = IconManager.class.getResourceAsStream(LauncherConstants.IMAGE_FAVICON);
        IS_ICONSTREAM_OPEN = true;
        IMAGE = ImageIO.read(ICONSTREAM);
        ICON = new ImageIcon(IMAGE);
    }

    public static ImageIcon getIcon() throws Exception {
        if (!IS_ICONSTREAM_OPEN) {
            initializeIcon();
        }
        return ICON;
    }

    public static Image getImage() throws Exception {
        if (!IS_ICONSTREAM_OPEN) {
            initializeIcon();
        }
        return IMAGE;
    }

    public static void closeIconStream() throws Exception {
        if (IS_ICONSTREAM_OPEN) {
            ICON = null;
            IMAGE = null;
            ICONSTREAM.close();
            IS_ICONSTREAM_OPEN = false;
        }
    }
}
