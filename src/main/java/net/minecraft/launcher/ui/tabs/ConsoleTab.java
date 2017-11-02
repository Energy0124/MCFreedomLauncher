package net.minecraft.launcher.ui.tabs;

import com.mojang.util.QueueLogAppender;
import net.minecraft.launcher.Launcher;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import java.awt.*;
import java.awt.datatransfer.StringSelection;

public class ConsoleTab extends JScrollPane {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final Font MONOSPACED = new Font("Monospaced", Font.PLAIN, 12);

    private final JTextArea console;
    private final Launcher minecraftLauncher;

    public ConsoleTab(final Launcher minecraftLauncher) {
        this.console = new JTextArea();
        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem copyTextButton = new JMenuItem("Copy All Text");
        this.minecraftLauncher = minecraftLauncher;
        popupMenu.add(copyTextButton);
        this.console.setComponentPopupMenu(popupMenu);
        copyTextButton.addActionListener(e -> {
            try {
                final StringSelection ss = new StringSelection(ConsoleTab.this.console.getText());
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss, null);
            } catch (Exception ex) {
                LOGGER.debug("An Exception is caught!");
            }
        });
        this.console.setFont(ConsoleTab.MONOSPACED);
        this.console.setEditable(false);
        this.console.setMargin(null);
        this.setViewportView(this.console);
        final Thread thread = new Thread(() -> {
            String line;
            while ((line = QueueLogAppender.getNextLogEvent("DevelopmentConsole")) != null) {
                ConsoleTab.this.print(line);
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    public Launcher getMinecraftLauncher() {
        return this.minecraftLauncher;
    }

    private void print(final String line) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> ConsoleTab.this.print(line));
            return;
        }
        final Document document = this.console.getDocument();
        final JScrollBar scrollBar = this.getVerticalScrollBar();
        boolean shouldScroll = false;
        if (this.getViewport().getView() == this.console) {
            shouldScroll = (scrollBar.getValue() + scrollBar.getSize().getHeight() + ConsoleTab.MONOSPACED.getSize() * 4 > scrollBar.getMaximum());
        }
        try {
            document.insertString(document.getLength(), line, null);
        } catch (BadLocationException ex) {
            LOGGER.debug("A BadLocationException is caught!");
        }
        if (shouldScroll) {
            scrollBar.setValue(Integer.MAX_VALUE);
        }
    }
}
