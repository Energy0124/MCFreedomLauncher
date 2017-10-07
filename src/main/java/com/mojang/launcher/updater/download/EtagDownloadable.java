package com.mojang.launcher.updater.download;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;

public class EtagDownloadable extends Downloadable {
    public EtagDownloadable(final Proxy proxy, final URL remoteFile, final File localFile, final boolean forceDownload) {
        super(proxy, remoteFile, localFile, forceDownload);
    }

    public static String getEtag(String etag) {
        if (etag == null) {
            etag = "-";
        } else if (etag.startsWith("\"") && etag.endsWith("\"")) {
            etag = etag.substring(1, etag.length() - 1);
        }
        return etag;
    }

    @Override
    public String download() throws IOException {
        ++this.numAttempts;
        this.ensureFileWritable(this.getTarget());
        try {
            final HttpURLConnection connection = this.makeConnection(this.getUrl());
            final int status = connection.getResponseCode();
            if (status == 304) {
                return "Used own copy as it matched etag";
            }
            if (status == 200) {
                this.updateExpectedSize(connection);
                final InputStream inputStream = new MonitoringInputStream(connection.getInputStream(), this.getMonitor());
                final FileOutputStream outputStream = new FileOutputStream(this.getTarget());
                final String md5 = Downloadable.copyAndDigest(inputStream, outputStream, "MD5", 32);
                final String etag = getEtag(connection.getHeaderField("ETag"));
                if (etag.contains("-")) {
                    return "Didn't have etag so assuming our copy is good";
                }
                if (etag.equalsIgnoreCase(md5)) {
                    return "Downloaded successfully and etag matched";
                }
                throw new RuntimeException(String.format("E-tag did not match downloaded MD5 (ETag was %s, downloaded %s)", etag, md5));
            } else {
                if (this.getTarget().isFile()) {
                    return "Couldn't connect to server (responded with " + status + ") but have local file, assuming it's good";
                }
                throw new RuntimeException("Server responded with " + status);
            }
        } catch (IOException e) {
            if (this.getTarget().isFile()) {
                return "Couldn't connect to server (" + e.getClass().getSimpleName() + ": '" + e.getMessage() + "') but have local file, assuming it's good";
            }
            throw e;
        }
    }

    @Override
    protected HttpURLConnection makeConnection(final URL url) throws IOException {
        final HttpURLConnection connection = super.makeConnection(url);
        if (!this.shouldIgnoreLocal() && this.getTarget().isFile()) {
            connection.setRequestProperty("If-None-Match", Downloadable.getDigest(this.getTarget(), "MD5", 32));
        }
        return connection;
    }
}
