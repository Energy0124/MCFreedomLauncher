package com.mojang.launcher.updater.download;

public interface DownloadListener {
    void onDownloadJobFinished(final DownloadJob p0);

    void onDownloadJobProgressChanged(final DownloadJob p0);
}
