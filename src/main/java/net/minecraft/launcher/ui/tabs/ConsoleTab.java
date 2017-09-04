package net.minecraft.launcher.ui.tabs;

import com.mojang.util.QueueLogAppender;
import net.minecraft.launcher.Launcher;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ConsoleTab
        extends JScrollPane {
    private static final Font MONOSPACED = new Font("Monospaced", 0, 12);
    private final JTextArea console = new JTextArea();
    private final JPopupMenu popupMenu = new JPopupMenu();
    private final JMenuItem copyTextButton = new JMenuItem("Copy All Text");
    private final Launcher minecraftLauncher;

    public ConsoleTab(Launcher minecraftLauncher) {
        this.minecraftLauncher = minecraftLauncher;

        this.popupMenu.add(this.copyTextButton);
        this.console.setComponentPopupMenu(this.popupMenu);

        this.copyTextButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    StringSelection ss = new StringSelection(ConsoleTab.this.console.getText());
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss, null);
                } catch (Exception localException) {
                }
            }
        });
        this.console.setFont(MONOSPACED);
        this.console.setEditable(false);
        this.console.setMargin(null);

        setViewportView(this.console);

        Thread thread = new Thread(new Runnable() {
            public void run() {
                String line;
                while ((line = QueueLogAppender.getNextLogEvent("DevelopmentConsole")) != null) {
                    ConsoleTab.this.print(line);
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    public Launcher getMinecraftLauncher() {
        return this.minecraftLauncher;
    }

    public void print(final String line) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    ConsoleTab.this.print(line);
                }
            });
            return;
        }
        Document document = this.console.getDocument();
        JScrollBar scrollBar = getVerticalScrollBar();
        boolean shouldScroll = false;
        if (getViewport().getView() == this.console) {
            shouldScroll = scrollBar.getValue() + scrollBar.getSize().getHeight() + MONOSPACED.getSize() * 4 > scrollBar.getMaximum();
        }
        try {
            document.insertString(document.getLength(), line, null);
        } catch (BadLocationException localBadLocationException) {
        }
        if (shouldScroll) {
            scrollBar.setValue(2147483647);
        }
    }
}