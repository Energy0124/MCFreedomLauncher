package com.mojang.launcher.updater.download;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;

import static java.nio.charset.StandardCharsets.UTF_8;

public class ChecksummedDownloadable extends Downloadable {
    private String localHash;
    private String expectedHash;

    private static final Logger LOGGER = LogManager.getLogger();

    public ChecksummedDownloadable(final Proxy proxy, final URL remoteFile, final File localFile, final boolean forceDownload) {
        super(proxy, remoteFile, localFile, forceDownload);
    }

    @Override
    public String download() throws IOException {
        ++this.numAttempts;
        this.ensureFileWritable(this.getTarget());
        final File target = this.getTarget();
        if (this.localHash == null && target.isFile()) {
            this.localHash = Downloadable.getDigest(target, "SHA-1", 40);
        }
        if (this.expectedHash == null) {
            try {
                final HttpURLConnection connection = this.makeConnection(new URL(this.getUrl().toString() + ".sha1"));
                final int status = connection.getResponseCode();
                if (status == 200) {
                    final InputStream inputStream = connection.getInputStream();
                    try {
                        this.expectedHash = IOUtils.toString(inputStream, UTF_8).trim();
                    } catch (IOException e2) {
                        this.expectedHash = "";
                    } finally {
                        try {
                            inputStream.close();
                        } catch (IOException e) {
                            LOGGER.warn("Unable to close input stream");
                        }
                    }
                } else {
                    this.expectedHash = "";
                }
            } catch (IOException e) {
                this.expectedHash = "";
            }
        }
        if (this.expectedHash.length() == 0 && target.isFile()) {
            return "Couldn't find a checksum so assuming our copy is good";
        }
        if (this.expectedHash.equalsIgnoreCase(this.localHash)) {
            return "Remote checksum matches local file";
        }
        try {
            final HttpURLConnection connection = this.makeConnection(this.getUrl());
            final int status = connection.getResponseCode();
            if (status == 200) {
                this.updateExpectedSize(connection);
                final InputStream inputStream = new MonitoringInputStream(connection.getInputStream(), this.getMonitor());
                final FileOutputStream outputStream = new FileOutputStream(this.getTarget());
                final String digest = Downloadable.copyAndDigest(inputStream, outputStream, "SHA", 40);
                if (this.expectedHash.length() == 0) {
                    return "Didn't have checksum so assuming the downloaded file is good";
                }
                if (this.expectedHash.equalsIgnoreCase(digest)) {
                    return "Downloaded successfully and checksum matched";
                }
                throw new RuntimeException(String.format("Checksum did not match downloaded file (Checksum was %s, downloaded %s)", this.expectedHash, digest));
            } else {
                if (this.getTarget().isFile()) {
                    return "Couldn't connect to server (responded with " + status + ") but have local file, assuming it's good";
                }
                throw new RuntimeException("Server responded with " + status);
            }
        } catch (IOException e) {
            if (this.getTarget().isFile() && (this.expectedHash == null || this.expectedHash.length() == 0)) {
                return "Couldn't connect to server (" + e.getClass().getSimpleName() + ": '" + e.getMessage() + "') but have local file, assuming it's good";
            }
            throw e;
        }
    }
}
