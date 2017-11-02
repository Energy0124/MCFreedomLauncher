package net.minecraft.launcher.updater;

import java.net.URL;

public class DownloadInfo {
    URL url;
    private String sha1;
    private int size;

    DownloadInfo() {
    }

    public DownloadInfo(final DownloadInfo other) {
        this.url = other.url;
        this.sha1 = other.sha1;
        this.size = other.size;
    }

    public URL getUrl() {
        return this.url;
    }

    public String getSha1() {
        return this.sha1;
    }

    public int getSize() {
        return this.size;
    }
}
