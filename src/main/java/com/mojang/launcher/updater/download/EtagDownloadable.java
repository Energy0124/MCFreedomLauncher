package com.mojang.launcher.updater.download;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;

public class EtagDownloadable
        extends Downloadable {
    public EtagDownloadable(Proxy proxy, URL remoteFile, File localFile, boolean forceDownload) {
        super(proxy, remoteFile, localFile, forceDownload);
    }

    public static String getEtag(String etag) {
        if (etag == null) {
            etag = "-";
        } else if ((etag.startsWith("\"")) && (etag.endsWith("\""))) {
            etag = etag.substring(1, etag.length() - 1);
        }
        return etag;
    }

    public String download()
            throws IOException {
        this.numAttempts += 1;
        ensureFileWritable(getTarget());
        try {
            HttpURLConnection connection = makeConnection(getUrl());
            int status = connection.getResponseCode();
            if (status == 304) {
                return "Used own copy as it matched etag";
            }
            if (status / 100 == 2) {
                updateExpectedSize(connection);

                InputStream inputStream = new MonitoringInputStream(connection.getInputStream(), getMonitor());
                FileOutputStream outputStream = new FileOutputStream(getTarget());
                String md5 = copyAndDigest(inputStream, outputStream, "MD5", 32);
                String etag = getEtag(connection.getHeaderField("ETag"));
                if (etag.contains("-")) {
                    return "Didn't have etag so assuming our copy is good";
                }
                if (etag.equalsIgnoreCase(md5)) {
                    return "Downloaded successfully and etag matched";
                }
                throw new RuntimeException(String.format("E-tag did not match downloaded MD5 (ETag was %s, downloaded %s)", etag, md5));
            }
            if (getTarget().isFile()) {
                return "Couldn't connect to server (responded with " + status + ") but have local file, assuming it's good";
            }
            throw new RuntimeException("Server responded with " + status);
        } catch (IOException e) {
            if (getTarget().isFile()) {
                return "Couldn't connect to server (" + e.getClass().getSimpleName() + ": '" + e.getMessage() + "') but have local file, assuming it's good";
            }
            throw e;
        }
    }

    protected HttpURLConnection makeConnection(URL url)
            throws IOException {
        HttpURLConnection connection = super.makeConnection(url);
        if ((!shouldIgnoreLocal()) && (getTarget().isFile())) {
            connection.setRequestProperty("If-None-Match", getDigest(getTarget(), "MD5", 32));
        }
        return connection;
    }
}