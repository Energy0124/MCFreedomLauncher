package com.mojang.launcher.game.runner;

import com.google.common.collect.Lists;
import com.mojang.launcher.Launcher;
import com.mojang.launcher.game.GameInstanceStatus;
import com.mojang.launcher.updater.DownloadProgress;
import com.mojang.launcher.updater.VersionSyncInfo;
import com.mojang.launcher.updater.download.DownloadJob;
import com.mojang.launcher.updater.download.DownloadListener;
import com.mojang.launcher.updater.download.Downloadable;
import com.mojang.launcher.versions.CompleteVersion;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static net.minecraft.launcher.LauncherConstants.MESSAGE_INCOMPATIBLE_REASON;
import static net.minecraft.launcher.LauncherConstants.URL_BUGS;

public abstract class AbstractGameRunner implements GameRunner, DownloadListener {
    protected static final Logger LOGGER = LogManager.getLogger();

    protected final Object lock;
    private final List<DownloadJob> jobs;
    private final List<GameRunnerListener> listeners;
    protected CompleteVersion version;
    private GameInstanceStatus status;

    public AbstractGameRunner() {
        this.lock = new Object();
        this.jobs = new ArrayList<>();
        this.status = GameInstanceStatus.IDLE;
        this.listeners = Lists.newArrayList();
    }

    protected abstract Launcher getLauncher();

    @Override
    public GameInstanceStatus getStatus() {
        return this.status;
    }

    protected void setStatus(final GameInstanceStatus status) {
        synchronized (this.lock) {
            this.status = status;
            for (final GameRunnerListener listener : Lists.newArrayList((Iterable<? extends GameRunnerListener>) this.listeners)) {
                listener.onGameInstanceChangedState(this, status);
            }
        }
    }

    @Override
    public void playGame(VersionSyncInfo syncInfo) {
        synchronized (this.lock) {
            if (this.getStatus() != GameInstanceStatus.IDLE) {
                AbstractGameRunner.LOGGER.warn("Tried to play game but game is already starting!");
                return;
            }
            this.setStatus(GameInstanceStatus.PREPARING);
        }
        AbstractGameRunner.LOGGER.info("Getting syncinfo for selected version");
        if (syncInfo == null) {
            AbstractGameRunner.LOGGER.warn("Tried to launch a version without a version being selected...");
            this.setStatus(GameInstanceStatus.IDLE);
            return;
        }
        synchronized (this.lock) {
            AbstractGameRunner.LOGGER.info("Queueing library & version downloads");
            try {
                this.version = this.getLauncher().getVersionManager().getLatestCompleteVersion(syncInfo);
            } catch (IOException e) {
                AbstractGameRunner.LOGGER.error("Couldn't get complete version info for " + syncInfo.getLatestVersion(), e);
                this.setStatus(GameInstanceStatus.IDLE);
                return;
            }
            if (syncInfo.getRemoteVersion() != null && syncInfo.getLatestSource() != VersionSyncInfo.VersionSource.REMOTE && !this.version.isSynced()) {
                try {
                    syncInfo = this.getLauncher().getVersionManager().syncVersion(syncInfo);
                    this.version = this.getLauncher().getVersionManager().getLatestCompleteVersion(syncInfo);
                } catch (IOException e) {
                    AbstractGameRunner.LOGGER.error("Couldn't sync local and remote versions", e);
                }
                this.version.setSynced(true);
            }
            if (!this.version.appliesToCurrentEnvironment()) {
                String reason = this.version.getIncompatibilityReason();
                if (reason == null) {
                    reason = MESSAGE_INCOMPATIBLE_REASON;
                }
                AbstractGameRunner.LOGGER.error("Version " + this.version.getId() + " is incompatible with current environment: " + reason);
                this.getLauncher().getUserInterface().gameLaunchFailure(reason);
                this.setStatus(GameInstanceStatus.IDLE);
                return;
            }
            if (!syncInfo.isUpToDate()) {
                try {
                    this.getLauncher().getVersionManager().installVersion(this.version);
                } catch (IOException e) {
                    AbstractGameRunner.LOGGER.error("Couldn't save version info to install " + syncInfo.getLatestVersion(), e);
                    this.setStatus(GameInstanceStatus.IDLE);
                    return;
                }
            }
            this.setStatus(GameInstanceStatus.DOWNLOADING);
            this.downloadRequiredFiles(syncInfo);
        }
    }

    protected void downloadRequiredFiles(final VersionSyncInfo syncInfo) {
        try {
            final DownloadJob librariesJob = new DownloadJob("Version & Libraries", false, this);
            this.addJob(librariesJob);
            this.getLauncher().getVersionManager().downloadVersion(syncInfo, librariesJob);
            librariesJob.startDownloading(this.getLauncher().getDownloaderExecutorService());
            final DownloadJob resourceJob = new DownloadJob("Resources", true, this);
            this.addJob(resourceJob);
            this.getLauncher().getVersionManager().downloadResources(resourceJob, this.version);
            resourceJob.startDownloading(this.getLauncher().getDownloaderExecutorService());
        } catch (IOException e) {
            AbstractGameRunner.LOGGER.error("Couldn't get version info for " + syncInfo.getLatestVersion(), e);
            this.setStatus(GameInstanceStatus.IDLE);
        }
    }

    protected void updateProgressBar() {
        synchronized (this.lock) {
            if (this.hasRemainingJobs()) {
                long total = 0L;
                long current = 0L;
                Downloadable longestRunning = null;
                for (final DownloadJob job : this.jobs) {
                    for (final Downloadable file : job.getAllFiles()) {
                        total += file.getMonitor().getTotal();
                        current += file.getMonitor().getCurrent();
                        if (longestRunning == null || longestRunning.getEndTime() > 0L || (file.getStartTime() < longestRunning.getStartTime() && file.getEndTime() == 0L)) {
                            longestRunning = file;
                        }
                    }
                }
                this.getLauncher().getUserInterface().setDownloadProgress(new DownloadProgress(current, total, (longestRunning == null) ? null : longestRunning.getStatus()));
            } else {
                this.jobs.clear();
                this.getLauncher().getUserInterface().hideDownloadProgress();
            }
        }
    }

    @Override
    public boolean hasRemainingJobs() {
        synchronized (this.lock) {
            for (final DownloadJob job : this.jobs) {
                if (!job.isComplete()) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void addJob(final DownloadJob job) {
        synchronized (this.lock) {
            this.jobs.add(job);
        }
    }

    @Override
    public void onDownloadJobFinished(final DownloadJob job) {
        this.updateProgressBar();
        synchronized (this.lock) {
            if (job.getFailures() > 0) {
                AbstractGameRunner.LOGGER.error("Job '" + job.getName() + "' finished with " + job.getFailures() + " failure(s)! (took " + job.getStopWatch().toString() + ")");
                this.setStatus(GameInstanceStatus.IDLE);
            } else {
                AbstractGameRunner.LOGGER.info("Job '" + job.getName() + "' finished successfully (took " + job.getStopWatch().toString() + ")");
                if (this.getStatus() != GameInstanceStatus.IDLE && !this.hasRemainingJobs()) {
                    try {
                        this.setStatus(GameInstanceStatus.LAUNCHING);
                        this.launchGame();
                    } catch (Throwable ex) {
                        AbstractGameRunner.LOGGER.fatal("Fatal error launching game. Report this to " + URL_BUGS + " please!", ex);
                    }
                }
            }
        }
    }

    protected abstract void launchGame() throws IOException;

    @Override
    public void onDownloadJobProgressChanged(final DownloadJob job) {
        this.updateProgressBar();
    }

    public void addListener(final GameRunnerListener listener) {
        synchronized (this.lock) {
            this.listeners.add(listener);
        }
    }
}
