package com.mojang.launcher.updater.download;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

public class DownloadJob {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final int MAX_ATTEMPTS_PER_FILE = 5;
    private static final long ASSUMED_AVERAGE_FILE_SIZE = 5242880L;

    private final Queue<Downloadable> remainingFiles;
    private final List<Downloadable> allFiles;
    private final List<Downloadable> failures;
    private final List<Downloadable> successful;
    private final DownloadListener listener;
    private final String name;
    private final boolean ignoreFailures;
    private final AtomicInteger remainingThreads;
    private final StopWatch stopWatch;
    private boolean started;

    public DownloadJob(final String name, final boolean ignoreFailures, final DownloadListener listener, final Collection<Downloadable> files) {
        this.remainingFiles = new ConcurrentLinkedQueue<>();
        this.allFiles = Collections.synchronizedList(new ArrayList<Downloadable>());
        this.failures = Collections.synchronizedList(new ArrayList<Downloadable>());
        this.successful = Collections.synchronizedList(new ArrayList<Downloadable>());
        this.remainingThreads = new AtomicInteger();
        this.stopWatch = new StopWatch();
        this.name = name;
        this.ignoreFailures = ignoreFailures;
        this.listener = listener;
        if (files != null) {
            this.addDownloadables(files);
        }
    }

    public DownloadJob(final String name, final boolean ignoreFailures, final DownloadListener listener) {
        this(name, ignoreFailures, listener, null);
    }

    public void addDownloadables(final Collection<Downloadable> downloadables) {
        if (this.started) {
            throw new IllegalStateException("Cannot add to download job that has already started");
        }
        this.allFiles.addAll(downloadables);
        this.remainingFiles.addAll(downloadables);
        for (final Downloadable downloadable : downloadables) {
            if (downloadable.getExpectedSize() == 0L) {
                downloadable.getMonitor().setTotal(ASSUMED_AVERAGE_FILE_SIZE);
            } else {
                downloadable.getMonitor().setTotal(downloadable.getExpectedSize());
            }
            downloadable.getMonitor().setJob(this);
        }
    }

    public void addDownloadables(final Downloadable... downloadables) {
        if (this.started) {
            throw new IllegalStateException("Cannot add to download job that has already started");
        }
        for (final Downloadable downloadable : downloadables) {
            this.allFiles.add(downloadable);
            this.remainingFiles.add(downloadable);
            if (downloadable.getExpectedSize() == 0L) {
                downloadable.getMonitor().setTotal(ASSUMED_AVERAGE_FILE_SIZE);
            } else {
                downloadable.getMonitor().setTotal(downloadable.getExpectedSize());
            }
            downloadable.getMonitor().setJob(this);
        }
    }

    public void startDownloading(final ThreadPoolExecutor executorService) {
        if (this.started) {
            throw new IllegalStateException("Cannot start download job that has already started");
        }
        this.started = true;
        this.stopWatch.start();
        if (this.allFiles.isEmpty()) {
            DownloadJob.LOGGER.info("Download job '" + this.name + "' skipped as there are no files to download");
            this.listener.onDownloadJobFinished(this);
        } else {
            final int threads = executorService.getMaximumPoolSize();
            this.remainingThreads.set(threads);
            DownloadJob.LOGGER.info("Download job '" + this.name + "' started (" + threads + " threads, " + this.allFiles.size() + " files)");
            for (int i = 0; i < threads; ++i) {
                executorService.submit(DownloadJob.this::popAndDownload);
            }
        }
    }

    private void popAndDownload() {
        Downloadable downloadable;
        while ((downloadable = this.remainingFiles.poll()) != null) {
            if (downloadable.getStartTime() == 0L) {
                downloadable.setStartTime(System.currentTimeMillis());
            }
            if (downloadable.getNumAttempts() > MAX_ATTEMPTS_PER_FILE) {
                if (!this.ignoreFailures) {
                    this.failures.add(downloadable);
                }
                DownloadJob.LOGGER.error("Gave up trying to download " + downloadable.getUrl() + " for job '" + this.name + "'");
            } else {
                try {
                    DownloadJob.LOGGER.info("Attempting to download " + downloadable.getTarget() + " for job '" + this.name + "'... (try " + downloadable.getNumAttempts() + ")");
                    final String result = downloadable.download();
                    this.successful.add(downloadable);
                    downloadable.setEndTime(System.currentTimeMillis());
                    downloadable.getMonitor().setCurrent(downloadable.getMonitor().getTotal());
                    DownloadJob.LOGGER.info("Finished downloading " + downloadable.getTarget() + " for job '" + this.name + "'" + ": " + result);
                } catch (Throwable t) {
                    DownloadJob.LOGGER.warn("Couldn't download " + downloadable.getUrl() + " for job '" + this.name + "'", t);
                    downloadable.getMonitor().setCurrent(downloadable.getMonitor().getTotal());
                    this.remainingFiles.add(downloadable);
                }
            }
        }
        if (this.remainingThreads.decrementAndGet() <= 0) {
            this.listener.onDownloadJobFinished(this);
        }
    }

    public boolean shouldIgnoreFailures() {
        return this.ignoreFailures;
    }

    public boolean isStarted() {
        return this.started;
    }

    public boolean isComplete() {
        return this.started && this.remainingFiles.isEmpty() && this.remainingThreads.get() == 0;
    }

    public int getFailures() {
        return this.failures.size();
    }

    public int getSuccessful() {
        return this.successful.size();
    }

    public String getName() {
        return this.name;
    }

    public void updateProgress() {
        this.listener.onDownloadJobProgressChanged(this);
    }

    public List<Downloadable> getAllFiles() {
        return this.allFiles;
    }

    public StopWatch getStopWatch() {
        return this.stopWatch;
    }
}
