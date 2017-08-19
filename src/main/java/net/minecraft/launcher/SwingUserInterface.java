package net.minecraft.launcher;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.SettableFuture;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.UserAuthentication;
import com.mojang.launcher.OperatingSystem;
import com.mojang.launcher.events.GameOutputLogProcessor;
import com.mojang.launcher.updater.DownloadProgress;
import com.mojang.launcher.updater.VersionManager;
import com.mojang.launcher.versions.CompleteVersion;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import net.minecraft.launcher.game.MinecraftGameRunner;
import net.minecraft.launcher.profile.AuthenticationDatabase;
import net.minecraft.launcher.profile.Profile;
import net.minecraft.launcher.profile.ProfileManager;
import net.minecraft.launcher.ui.BottomBarPanel;
import net.minecraft.launcher.ui.LauncherPanel;
import net.minecraft.launcher.ui.bottombar.PlayButtonPanel;
import net.minecraft.launcher.ui.popups.login.LogInPopup;
import net.minecraft.launcher.ui.popups.login.LogInPopup.Callback;
import net.minecraft.launcher.ui.tabs.CrashReportTab;
import net.minecraft.launcher.ui.tabs.GameOutputTab;
import net.minecraft.launcher.ui.tabs.LauncherTabPanel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SwingUserInterface
  implements MinecraftUserInterface
{
  private static final Logger LOGGER;
  private final Launcher minecraftLauncher;
  private LauncherPanel launcherPanel;
  private final JFrame frame;
  
  public SwingUserInterface(Launcher minecraftLauncher, JFrame frame)
  {
    this.minecraftLauncher = minecraftLauncher;
    this.frame = frame;
    
    setLookAndFeel();
  }
  
  private static void setLookAndFeel()
  {
    JFrame frame = new JFrame();
    try
    {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    }
    catch (Throwable ignored)
    {
      try
      {
        LOGGER.error("Your java failed to provide normal look and feel, trying the old fallback now");
        UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
      }
      catch (Throwable t)
      {
        LOGGER.error("Unexpected exception setting look and feel", t);
      }
    }
    JPanel panel = new JPanel();
    panel.setBorder(BorderFactory.createTitledBorder("test"));
    frame.add(panel);
    try
    {
      frame.pack();
    }
    catch (Throwable ignored)
    {
      LOGGER.error("Custom (broken) theme detected, falling back onto x-platform theme");
      try
      {
        UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
      }
      catch (Throwable ex)
      {
        LOGGER.error("Unexpected exception setting look and feel", ex);
      }
    }
    frame.dispose();
  }
  
  public void showLoginPrompt(final Launcher minecraftLauncher, final LogInPopup.Callback callback)
  {
    SwingUtilities.invokeLater(new Runnable()
    {
      public void run()
      {
        LogInPopup popup = new LogInPopup(minecraftLauncher, callback);
        SwingUserInterface.this.launcherPanel.setCard("login", popup);
      }
    });
  }
  
  public void initializeFrame()
  {
    this.frame.getContentPane().removeAll();
    this.frame.setTitle("Minecraft Freedom Launcher 1.5.2");
    this.frame.setPreferredSize(new Dimension(900, 580));
    this.frame.setDefaultCloseOperation(2);
    this.frame.pack();
    this.frame.setLocationRelativeTo(null);
    this.frame.setVisible(true);
    
    this.frame.addWindowListener(new WindowAdapter()
    {
      public void windowClosing(WindowEvent e)
      {
        SwingUserInterface.this.frame.setVisible(false);
        SwingUserInterface.this.frame.dispose();
        SwingUserInterface.this.minecraftLauncher.getLauncher().getVersionManager().getExecutorService().shutdown();
      }
    });
    try
    {
      InputStream in = Launcher.class.getResourceAsStream("/favicon.png");
      if (in != null) {
        this.frame.setIconImage(ImageIO.read(in));
      }
    }
    catch (IOException localIOException) {}
    this.launcherPanel = new LauncherPanel(this.minecraftLauncher);
    
    SwingUtilities.invokeLater(new Runnable()
    {
      public void run()
      {
        SwingUserInterface.this.frame.add(SwingUserInterface.this.launcherPanel);
        SwingUserInterface.this.frame.pack();
        SwingUserInterface.this.frame.setVisible(true);
      }
    });
  }
  
  public void showOutdatedNotice()
  {
    String error = "Sorry, but your launcher is outdated! Please redownload it at https://mojang.com/2013/06/minecraft-1-6-pre-release/";
    
    this.frame.getContentPane().removeAll();
    
    int result = JOptionPane.showOptionDialog(this.frame, error, "Outdated launcher", 0, 0, null, LauncherConstants.BOOTSTRAP_OUT_OF_DATE_BUTTONS, LauncherConstants.BOOTSTRAP_OUT_OF_DATE_BUTTONS[0]);
    if (result == 0) {
      try
      {
        OperatingSystem.openLink(new URI("https://mojang.com/2013/06/minecraft-1-6-pre-release/"));
      }
      catch (URISyntaxException e)
      {
        LOGGER.error("Couldn't open bootstrap download link. Please visit https://mojang.com/2013/06/minecraft-1-6-pre-release/ manually.", e);
      }
    }
    this.minecraftLauncher.getLauncher().shutdownLauncher();
  }
  
  public void showLoginPrompt()
  {
    final ProfileManager profileManager = this.minecraftLauncher.getProfileManager();
    try
    {
      profileManager.saveProfiles();
    }
    catch (IOException e)
    {
      LOGGER.error("Couldn't save profiles before logging in!", e);
    }
    final Profile selectedProfile = profileManager.getSelectedProfile();
    showLoginPrompt(this.minecraftLauncher, new LogInPopup.Callback()
    {
      public void onLogIn(String uuid)
      {
        UserAuthentication auth = profileManager.getAuthDatabase().getByUUID(uuid);
        profileManager.setSelectedUser(uuid);
        if ((selectedProfile.getName().equals("(Default)")) && (auth.getSelectedProfile() != null))
        {
          String playerName = auth.getSelectedProfile().getName();
          String profileName = auth.getSelectedProfile().getName();
          int count = 1;
          while (profileManager.getProfiles().containsKey(profileName)) {
            profileName = playerName + " " + ++count;
          }
          Profile newProfile = new Profile(selectedProfile);
          newProfile.setName(profileName);
          profileManager.getProfiles().put(profileName, newProfile);
          profileManager.getProfiles().remove("(Default)");
          profileManager.setSelectedProfile(profileName);
        }
        try
        {
          profileManager.saveProfiles();
        }
        catch (IOException e)
        {
          SwingUserInterface.LOGGER.error("Couldn't save profiles after logging in!", e);
        }
        if (uuid == null) {
          SwingUserInterface.this.minecraftLauncher.getLauncher().shutdownLauncher();
        } else {
          profileManager.fireRefreshEvent();
        }
        SwingUserInterface.this.launcherPanel.setCard("launcher", null);
      }
    });
  }
  
  public void setVisible(final boolean visible)
  {
    SwingUtilities.invokeLater(new Runnable()
    {
      public void run()
      {
        SwingUserInterface.this.frame.setVisible(visible);
      }
    });
  }
  
  public void shutdownLauncher()
  {
    this.frame.dispatchEvent(new WindowEvent(this.frame, 201));
  }
  
  public void setDownloadProgress(final DownloadProgress downloadProgress)
  {
    SwingUtilities.invokeLater(new Runnable()
    {
      public void run()
      {
        SwingUserInterface.this.launcherPanel.getProgressBar().setVisible(true);
        SwingUserInterface.this.launcherPanel.getProgressBar().setValue((int)(downloadProgress.getPercent() * 100.0F));
        SwingUserInterface.this.launcherPanel.getProgressBar().setString(downloadProgress.getStatus());
      }
    });
  }
  
  public void hideDownloadProgress()
  {
    SwingUtilities.invokeLater(new Runnable()
    {
      public void run()
      {
        SwingUserInterface.this.launcherPanel.getProgressBar().setVisible(false);
      }
    });
  }
  
  public void showCrashReport(final CompleteVersion version, final File crashReportFile, final String crashReport)
  {
    SwingUtilities.invokeLater(new Runnable()
    {
      public void run()
      {
        SwingUserInterface.this.launcherPanel.getTabPanel().setCrashReport(new CrashReportTab(SwingUserInterface.this.minecraftLauncher, version, crashReportFile, crashReport));
      }
    });
  }
  
  public void gameLaunchFailure(final String reason)
  {
    SwingUtilities.invokeLater(new Runnable()
    {
      public void run()
      {
        JOptionPane.showMessageDialog(SwingUserInterface.this.frame, reason, "Cannot play game", 0);
      }
    });
  }
  
  public void updatePlayState()
  {
    SwingUtilities.invokeLater(new Runnable()
    {
      public void run()
      {
        SwingUserInterface.this.launcherPanel.getBottomBar().getPlayButtonPanel().checkState();
      }
    });
  }
  
  public GameOutputLogProcessor showGameOutputTab(final MinecraftGameRunner gameRunner)
  {
    final SettableFuture<GameOutputLogProcessor> future = SettableFuture.create();
    
    SwingUtilities.invokeLater(new Runnable()
    {
      public void run()
      {
        GameOutputTab tab = new GameOutputTab(SwingUserInterface.this.minecraftLauncher);
        future.set(tab);
        UserAuthentication auth = gameRunner.getAuth();
        String name = auth.getSelectedProfile() == null ? "Demo" : auth.getSelectedProfile().getName();
        SwingUserInterface.this.launcherPanel.getTabPanel().removeTab("Game Output (" + name + ")");
        SwingUserInterface.this.launcherPanel.getTabPanel().addTab("Game Output (" + name + ")", tab);
        SwingUserInterface.this.launcherPanel.getTabPanel().setSelectedComponent(tab);
      }
    });
    return (GameOutputLogProcessor)Futures.getUnchecked(future);
  }
  
  public String getTitle()
  {
    return "Minecraft Launcher 1.5.2";
  }
  
  public JFrame getFrame()
  {
    return this.frame;
  }
  static {
      LOGGER = LogManager.getLogger();
  }
}
