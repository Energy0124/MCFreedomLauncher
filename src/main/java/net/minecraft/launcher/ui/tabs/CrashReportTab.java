package net.minecraft.launcher.ui.tabs;

import com.mojang.launcher.Http;
import com.mojang.launcher.OperatingSystem;
import com.mojang.launcher.versions.CompleteVersion;
import net.minecraft.hopper.HopperService;
import net.minecraft.hopper.SubmitResponse;
import net.minecraft.launcher.Launcher;
import net.minecraft.launcher.LauncherConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import static java.awt.GridBagConstraints.EAST;
import static java.awt.GridBagConstraints.HORIZONTAL;
import static javax.swing.JOptionPane.INFORMATION_MESSAGE;
import static javax.swing.JOptionPane.YES_NO_OPTION;
import static javax.swing.JOptionPane.YES_OPTION;
import static net.minecraft.launcher.LauncherConstants.*;

public class CrashReportTab extends JPanel {
    private static final Logger LOGGER = LogManager.getLogger();

    private final Launcher minecraftLauncher;
    private final CompleteVersion version;
    private final File reportFile;
    private final String report;
    private final JEditorPane reportEditor;
    private final JScrollPane scrollPane;
    private final CrashInfoPane crashInfoPane;
    private SubmitResponse hopperServiceResponse;

    public CrashReportTab(final Launcher minecraftLauncher, final CompleteVersion version, final File reportFile, final String report) {
        super(true);
        this.reportEditor = new JEditorPane();
        this.scrollPane = new JScrollPane(this.reportEditor);
        this.hopperServiceResponse = null;
        this.minecraftLauncher = minecraftLauncher;
        this.version = version;
        this.reportFile = reportFile;
        this.report = report;
        this.crashInfoPane = new CrashInfoPane(minecraftLauncher);
        this.setLayout(new BorderLayout());
        this.createInterface();
        if (minecraftLauncher.getProfileManager().getSelectedProfile().getUseHopperCrashService()) {
            minecraftLauncher.getLauncher().getVersionManager().getExecutorService().submit(() -> {
                try {
                    final Map<String, String> environment = new HashMap<>();
                    environment.put("launcher.version", LauncherConstants.getVersionName());
                    environment.put("launcher.title", minecraftLauncher.getUserInterface().getTitle());
                    environment.put("bootstrap.version", String.valueOf(minecraftLauncher.getBootstrapVersion()));
                    CrashReportTab.this.hopperServiceResponse = HopperService.submitReport(minecraftLauncher.getLauncher().getProxy(), report, "Minecraft", version.getId(), environment);
                    CrashReportTab.LOGGER.info("Reported crash to Mojang (ID " + CrashReportTab.this.hopperServiceResponse.getReport().getId() + ")");
                    if (CrashReportTab.this.hopperServiceResponse.getProblem() != null) {
                        CrashReportTab.this.showKnownProblemPopup();
                    } else if (CrashReportTab.this.hopperServiceResponse.getReport().canBePublished()) {
                        CrashReportTab.this.showPublishReportPrompt();
                    }
                } catch (IOException e) {
                    CrashReportTab.LOGGER.error("Couldn't report crash to Mojang", e);
                }
            });
        }
    }

    private void showPublishReportPrompt() {
        try {
            final int result = JOptionPane.showConfirmDialog(this, MESSAGE_PUBLISH_CRASH_REPORT, MINECRAFT_FREEDOM_LAUNCHER_WINDOW_TITLE, YES_NO_OPTION, INFORMATION_MESSAGE, new ImageIcon(IMAGE_FAVICON));
            if (result == YES_OPTION) {
                try {
                    HopperService.publishReport(this.minecraftLauncher.getLauncher().getProxy(), this.hopperServiceResponse.getReport());
                } catch (IOException e) {
                    CrashReportTab.LOGGER.error("Couldn't publish report " + this.hopperServiceResponse.getReport().getId(), e);
                }
            }
        } catch (Exception e) {
            LOGGER.debug("An Exception is caught!");
        }
    }

    private void showKnownProblemPopup() {
        if (this.hopperServiceResponse.getProblem().getUrl() == null) {
            try {
                JOptionPane.showMessageDialog(this, this.hopperServiceResponse.getProblem().getDescription(), MINECRAFT_FREEDOM_LAUNCHER_WINDOW_TITLE, INFORMATION_MESSAGE, new ImageIcon(IMAGE_FAVICON));
            } catch (Exception e) {
                LOGGER.debug("An Exception is caught!");
            }
        } else {
            try {
                final int result = JOptionPane.showConfirmDialog(this, this.hopperServiceResponse.getProblem().getDescription() + "\nDo you want to fix the problem?", MINECRAFT_FREEDOM_LAUNCHER_WINDOW_TITLE, YES_NO_OPTION, INFORMATION_MESSAGE, new ImageIcon(IMAGE_FAVICON));
                if (result == YES_OPTION) {
                    try {
                        OperatingSystem.openLink(new URI(this.hopperServiceResponse.getProblem().getUrl()));
                    } catch (URISyntaxException e) {
                        CrashReportTab.LOGGER.error("Couldn't open help page ( " + this.hopperServiceResponse.getProblem().getUrl() + "  ) for crash", e);
                    }
                }
            } catch (Exception e) {
                LOGGER.debug("An Exception is caught!");
            }
        }
    }

    protected void createInterface() {
        this.add(this.crashInfoPane, "North");
        this.add(this.scrollPane, "Center");
        this.reportEditor.setText(this.report);
        this.crashInfoPane.createInterface();
    }

    private class CrashInfoPane extends JPanel implements ActionListener {
        public static final String INFO = MESSAGE_GAME_CRASHED;
        private final JButton submitButton;
        private final JButton openFileButton;

        protected CrashInfoPane(final Launcher minecraftLauncher) {
            this.submitButton = new JButton("Report to Mojang");
            this.openFileButton = new JButton("Open report file");
            this.submitButton.addActionListener(this);
            this.openFileButton.addActionListener(this);
        }

        protected void createInterface() {
            this.setLayout(new GridBagLayout());
            final GridBagConstraints constraints = new GridBagConstraints();
            constraints.anchor = EAST;
            constraints.fill = HORIZONTAL;
            constraints.insets = new Insets(2, 2, 2, 2);
            constraints.gridx = 1;
            this.add(this.submitButton, constraints);
            constraints.gridy = 1;
            this.add(this.openFileButton, constraints);
            constraints.gridx = 0;
            constraints.gridy = 0;
            constraints.weightx = 1.0;
            constraints.weighty = 1.0;
            constraints.gridheight = 2;
            this.add(new JLabel(INFO), constraints);
        }

        @Override
        public void actionPerformed(final ActionEvent e) {
            if (e.getSource() == this.submitButton) {
                if (CrashReportTab.this.hopperServiceResponse != null) {
                    if (CrashReportTab.this.hopperServiceResponse.getProblem() != null) {
                        CrashReportTab.this.showKnownProblemPopup();
                    } else if (CrashReportTab.this.hopperServiceResponse.getReport().canBePublished()) {
                        CrashReportTab.this.showPublishReportPrompt();
                    }
                } else {
                    try {
                        final Map<String, Object> args = new HashMap<>();
                        args.put("pid", 10400);
                        args.put("issuetype", 1);
                        args.put("description", MESSAGE_DESCRIBE_CRASH);
                        args.put("environment", this.buildEnvironmentInfo());
                        OperatingSystem.openLink(URI.create(URL_BUGS + "secure/CreateIssueDetails!init.jspa?" + Http.buildQuery(args)));
                    } catch (Throwable ex) {
                        CrashReportTab.LOGGER.error("Couldn't open bugtracker", ex);
                    }
                }
            } else if (e.getSource() == this.openFileButton) {
                OperatingSystem.openLink(CrashReportTab.this.reportFile.toURI());
            }
        }

        private String buildEnvironmentInfo() {
            return "OS: " + System.getProperty("os.name") + " (ver " + System.getProperty("os.version") + ", arch " + System.getProperty("os.arch") + ")\nJava: " + System.getProperty("java.version") + " (by " + System.getProperty("java.vendor") + ")\nLauncher: " + CrashReportTab.this.minecraftLauncher.getUserInterface().getTitle() + " (bootstrap " + CrashReportTab.this.minecraftLauncher.getBootstrapVersion() + ")\nMinecraft: " + CrashReportTab.this.version.getId() + " (updated " + CrashReportTab.this.version.getUpdatedTime() + ")";
        }
    }
}
