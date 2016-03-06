package com.mojang.launcher.game.runner;

import com.google.common.collect.Lists;
import com.mojang.launcher.Launcher;
import com.mojang.launcher.UserInterface;
import com.mojang.launcher.game.GameInstanceStatus;
import com.mojang.launcher.updater.DownloadProgress;
import com.mojang.launcher.updater.VersionManager;
import com.mojang.launcher.updater.VersionSyncInfo;
import com.mojang.launcher.updater.VersionSyncInfo.VersionSource;
import com.mojang.launcher.updater.download.DownloadJob;
import com.mojang.launcher.updater.download.DownloadListener;
import com.mojang.launcher.updater.download.Downloadable;
import com.mojang.launcher.updater.download.ProgressContainer;
import com.mojang.launcher.versions.CompleteVersion;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class AbstractGameRunner
  implements GameRunner, DownloadListener
{
  protected static final Logger LOGGER;
  protected final Object lock = new Object();
  private final List<DownloadJob> jobs = new ArrayList();
  protected CompleteVersion version;
  private GameInstanceStatus status = GameInstanceStatus.IDLE;
  private final List<GameRunnerListener> listeners = Lists.newArrayList();
  
  protected void setStatus(GameInstanceStatus status)
  {
    synchronized (this.lock)
    {
      this.status = status;
      for (GameRunnerListener listener : Lists.newArrayList(this.listeners)) {
        listener.onGameInstanceChangedState(this, status);
      }
    }
  }
  
  protected abstract Launcher getLauncher();
  
  public GameInstanceStatus getStatus()
  {
    return this.status;
  }
  
  public void playGame(VersionSyncInfo syncInfo)
  {
    synchronized (this.lock)
    {
      if (getStatus() != GameInstanceStatus.IDLE)
      {
        LOGGER.warn("Tried to play game but game is already starting!");
        return;
      }
      setStatus(GameInstanceStatus.PREPARING);
    }
    LOGGER.info("Getting syncinfo for selected version");
    if (syncInfo == null)
    {
      LOGGER.warn("Tried to launch a version without a version being selected...");
      setStatus(GameInstanceStatus.IDLE);
      return;
    }
    synchronized (this.lock)
    {
      LOGGER.info("Queueing library & version downloads");
      try
      {
        this.version = getLauncher().getVersionManager().getLatestCompleteVersion(syncInfo);
      }
      catch (IOException e)
      {
        LOGGER.error("Couldn't get complete version info for " + syncInfo.getLatestVersion(), e);
        setStatus(GameInstanceStatus.IDLE);
        return;
      }
      if ((syncInfo.getRemoteVersion() != null) && (syncInfo.getLatestSource() != VersionSyncInfo.VersionSource.REMOTE) && (!this.version.isSynced()))
      {
        try
        {
          syncInfo = getLauncher().getVersionManager().syncVersion(syncInfo);
          this.version = getLauncher().getVersionManager().getLatestCompleteVersion(syncInfo);
        }
        catch (IOException e)
        {
          LOGGER.error("Couldn't sync local and remote versions", e);
        }
        this.version.setSynced(true);
      }
      if (!this.version.appliesToCurrentEnvironment())
      {
        String reason = this.version.getIncompatibilityReason();
        if (reason == null) {
          reason = "This version is incompatible with your computer. Please try another one by going into Edit Profile and selecting one through the dropdown. Sorry!";
        }
        LOGGER.error("Version " + this.version.getId() + " is incompatible with current environment: " + reason);
        getLauncher().getUserInterface().gameLaunchFailure(reason);
        setStatus(GameInstanceStatus.IDLE);
        return;
      }
      /*if (this.version.getMinimumLauncherVersion() > getLauncher().getLauncherFormatVersion())
      {
        LOGGER.error("An update to your launcher is available and is required to play " + this.version.getId() + ". Please restart your launcher.");
        setStatus(GameInstanceStatus.IDLE);
        return;
      }*/
      if (!syncInfo.isUpToDate()) {
        try
        {
          getLauncher().getVersionManager().installVersion(this.version);
        }
        catch (IOException e)
        {
          LOGGER.error("Couldn't save version info to install " + syncInfo.getLatestVersion(), e);
          setStatus(GameInstanceStatus.IDLE);
          return;
        }
      }
      setStatus(GameInstanceStatus.DOWNLOADING);
      downloadRequiredFiles(syncInfo);
    }
  }
  
  protected void downloadRequiredFiles(VersionSyncInfo syncInfo)
  {
    try
    {
      DownloadJob librariesJob = new DownloadJob("Version & Libraries", false, this);
      addJob(librariesJob);
      getLauncher().getVersionManager().downloadVersion(syncInfo, librariesJob);
      librariesJob.startDownloading(getLauncher().getDownloaderExecutorService());
      
      DownloadJob resourceJob = new DownloadJob("Resources", true, this);
      addJob(resourceJob);
      getLauncher().getVersionManager().downloadResources(resourceJob, this.version);
      resourceJob.startDownloading(getLauncher().getDownloaderExecutorService());
    }
    catch (IOException e)
    {
      LOGGER.error("Couldn't get version info for " + syncInfo.getLatestVersion(), e);
      setStatus(GameInstanceStatus.IDLE);
    }
  }
  
  protected void updateProgressBar()
  {
    if (hasRemainingJobs()) {
      synchronized (this.lock)
      {
        long total = 0L;
        long current = 0L;
        Downloadable longestRunning = null;
        for (DownloadJob job : this.jobs) {
          for (Downloadable file : job.getAllFiles())
          {
            total += file.getMonitor().getTotal();
            current += file.getMonitor().getCurrent();
            if ((longestRunning == null) || (longestRunning.getEndTime() > 0L) || ((file.getStartTime() < longestRunning.getStartTime()) && (file.getEndTime() == 0L))) {
              longestRunning = file;
            }
          }
        }
        getLauncher().getUserInterface().setDownloadProgress(new DownloadProgress(current, total, longestRunning == null ? null : longestRunning.getStatus()));
      }
    } else {
      getLauncher().getUserInterface().hideDownloadProgress();
    }
  }
  
  public boolean hasRemainingJobs()
  {
    synchronized (this.lock)
    {
      for (DownloadJob job : this.jobs) {
        if (!job.isComplete()) {
          return true;
        }
      }
    }
    return false;
  }
  
  public void addJob(DownloadJob job)
  {
    synchronized (this.lock)
    {
      this.jobs.add(job);
    }
  }
  
  public void onDownloadJobFinished(DownloadJob job)
  {
    updateProgressBar();
    synchronized (this.lock)
    {
      if (job.getFailures() > 0)
      {
        LOGGER.error("Job '" + job.getName() + "' finished with " + job.getFailures() + " failure(s)! (took " + job.getStopWatch().toString() + ")");
        setStatus(GameInstanceStatus.IDLE);
      }
      else
      {
        LOGGER.info("Job '" + job.getName() + "' finished successfully (took " + job.getStopWatch().toString() + ")");
        if ((getStatus() != GameInstanceStatus.IDLE) && (!hasRemainingJobs())) {
          try
          {
            setStatus(GameInstanceStatus.LAUNCHING);
            launchGame();
          }
          catch (Throwable ex)
          {
            LOGGER.fatal("Fatal error launching game. Report this to http://bugs.mojang.com please!", ex);
          }
        }
      }
    }
  }
  
  protected abstract void launchGame()
    throws IOException;
  
  public void onDownloadJobProgressChanged(DownloadJob job)
  {
    updateProgressBar();
  }
  
  public void addListener(GameRunnerListener listener)
  {
    synchronized (this.lock)
    {
      this.listeners.add(listener);
    }
  }
  static {
      LOGGER = LogManager.getLogger();
  }
}
