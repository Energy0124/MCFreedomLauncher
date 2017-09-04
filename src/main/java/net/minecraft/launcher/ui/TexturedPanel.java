package net.minecraft.launcher.ui;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.io.IOException;

public class TexturedPanel
        extends JPanel {
    private static final Logger LOGGER;
    private static final long serialVersionUID = 1L;

    static {
        LOGGER = LogManager.getLogger();
    }

    private Image image;
    private Image bgImage;

    public TexturedPanel(String filename) {
        setOpaque(true);
        try {
            this.bgImage = ImageIO.read(TexturedPanel.class.getResource(filename)).getScaledInstance(32, 32, 16);
        } catch (IOException e) {
            LOGGER.error("Unexpected exception initializing textured panel", e);
        }
    }

    public void update(Graphics g) {
        paint(g);
    }

    public void paintComponent(Graphics graphics) {
        int width = getWidth() / 2 + 1;
        int height = getHeight() / 2 + 1;
        if ((this.image == null) || (this.image.getWidth(null) != width) || (this.image.getHeight(null) != height)) {
            this.image = createImage(width, height);
            copyImage(width, height);
        }
        graphics.drawImage(this.image, 0, 0, width * 2, height * 2, null);
    }

    protected void copyImage(int width, int height) {
        Graphics imageGraphics = this.image.getGraphics();
        for (int x = 0; x <= width / 32; x++) {
            for (int y = 0; y <= height / 32; y++) {
                imageGraphics.drawImage(this.bgImage, x * 32, y * 32, null);
            }
        }
        imageGraphics.dispose();
    }

}
