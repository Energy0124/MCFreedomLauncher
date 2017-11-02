package net.minecraft.launcher.ui;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;

class TexturedPanel extends JPanel {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final long serialVersionUID = 1L;

    private Image image;
    private Image bgImage;

    public TexturedPanel(final String filename) {
        this.setOpaque(true);
        try {
            this.bgImage = ImageIO.read(TexturedPanel.class.getResource(filename)).getScaledInstance(32, 32, 16);
        } catch (IOException e) {
            TexturedPanel.LOGGER.error("Unexpected exception initializing textured panel", e);
        }
    }

    @Override
    public void update(final Graphics g) {
        this.paint(g);
    }

    public void paintComponent(final Graphics graphics) {
        final int width = this.getWidth() / 2 + 1;
        final int height = this.getHeight() / 2 + 1;
        if (this.image == null || this.image.getWidth(null) != width || this.image.getHeight(null) != height) {
            this.image = this.createImage(width, height);
            this.copyImage(width, height);
        }
        graphics.drawImage(this.image, 0, 0, width * 2, height * 2, null);
    }

    private void copyImage(final int width, final int height) {
        final Graphics imageGraphics = this.image.getGraphics();
        for (int x = 0; x <= width / 32; ++x) {
            for (int y = 0; y <= height / 32; ++y) {
                imageGraphics.drawImage(this.bgImage, x * 32, y * 32, null);
            }
        }
        imageGraphics.dispose();
    }

}
