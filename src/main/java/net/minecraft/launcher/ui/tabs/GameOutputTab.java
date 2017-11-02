package net.minecraft.launcher.ui.tabs;

import com.mojang.launcher.events.GameOutputLogProcessor;
import com.mojang.launcher.game.process.GameProcess;
import net.minecraft.launcher.Launcher;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import java.awt.*;
import java.awt.datatransfer.StringSelection;

public class GameOutputTab extends JScrollPane implements GameOutputLogProcessor {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final Font MONOSPACED = new Font("Monospaced", Font.PLAIN, 12);
    private static final int MAX_LINE_COUNT = 1000;

    private final JTextArea console;
    private final Launcher minecraftLauncher;
    private boolean alreadyCensored;

    public GameOutputTab(final Launcher minecraftLauncher) {
        this.console = new JTextArea();
        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem copyTextButton = new JMenuItem("Copy All Text");
        this.alreadyCensored = false;
        this.minecraftLauncher = minecraftLauncher;
        popupMenu.add(copyTextButton);
        this.console.setComponentPopupMenu(popupMenu);
        copyTextButton.addActionListener(e -> {
            try {
                final StringSelection ss = new StringSelection(GameOutputTab.this.console.getText());
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss, null);
            } catch (Exception ex) {
                LOGGER.debug("An Exception is caught!");
            }
        });
        this.console.setFont(GameOutputTab.MONOSPACED);
        this.console.setEditable(false);
        this.console.setMargin(null);
        this.setViewportView(this.console);
        this.console.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(final DocumentEvent e) {
                SwingUtilities.invokeLater(() -> {
                    final Document document = GameOutputTab.this.console.getDocument();
                    final Element root = document.getDefaultRootElement();
                    while (root.getElementCount() > (MAX_LINE_COUNT + 1)) {
                        try {
                            document.remove(0, root.getElement(0).getEndOffset());
                        } catch (BadLocationException ex) {
                            LOGGER.debug("A BadLocationException is caught!");
                        }
                    }
                });
            }

            @Override
            public void removeUpdate(final DocumentEvent e) {
            }

            @Override
            public void changedUpdate(final DocumentEvent e) {
            }
        });
    }

    public Launcher getMinecraftLauncher() {
        return this.minecraftLauncher;
    }

    private void print(final String line) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> GameOutputTab.this.print(line));
            return;
        }
        final Document document = this.console.getDocument();
        final JScrollBar scrollBar = this.getVerticalScrollBar();
        boolean shouldScroll = false;
        if (this.getViewport().getView() == this.console) {
            shouldScroll = (scrollBar.getValue() + scrollBar.getSize().getHeight() + GameOutputTab.MONOSPACED.getSize() * 4 > scrollBar.getMaximum());
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

    @Override
    public void onGameOutput(final GameProcess process, String logLine) {
        if (!this.alreadyCensored) {
            final int index = logLine.indexOf("(Session ID is");
            if (index > 0) {
                this.alreadyCensored = true;
                logLine = logLine.substring(0, index) + "(Session ID is <censored>)";
            }
        }
        this.print(logLine + "\n");
    }
}
