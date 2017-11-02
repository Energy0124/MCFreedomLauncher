package com.mojang.launcher.updater.download;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public abstract class Downloadable {
    private static final Logger LOGGER = LogManager.getLogger();

    private final URL url;
    private final File target;
    private final boolean forceDownload;
    private final Proxy proxy;
    private final ProgressContainer monitor;
    protected int numAttempts;
    private long startTime;
    private long expectedSize;
    private long endTime;

    protected Downloadable(final Proxy proxy, final URL remoteFile, final File localFile, final boolean forceDownload) {
        this.proxy = proxy;
        this.url = remoteFile;
        this.target = localFile;
        this.forceDownload = forceDownload;
        this.monitor = new ProgressContainer();
    }

    public static String getDigest(final File file, final String algorithm, final int hashLength) {
        DigestInputStream stream = null;
        try {
            stream = new DigestInputStream(new FileInputStream(file), MessageDigest.getInstance(algorithm));
            final byte[] buffer = new byte[65536];
            int read;
            do {
                read = stream.read(buffer);
            } while (read > 0);
        } catch (Exception ignored) {
            return null;
        } finally {
            closeSilently(stream);
        }
        return String.format("%1$0" + hashLength + "x", new BigInteger(1, stream.getMessageDigest().digest()));
    }

    public static void closeSilently(final Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException ex) {
                LOGGER.debug("An IOException is caught!");
            }
        }
    }

    public static String copyAndDigest(final InputStream inputStream, final OutputStream outputStream, final String algorithm, final int hashLength) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            closeSilently(inputStream);
            closeSilently(outputStream);
            throw new RuntimeException("Missing Digest." + algorithm, e);
        }
        final byte[] buffer = new byte[65536];
        try {
            for (int read = inputStream.read(buffer); read >= 1; read = inputStream.read(buffer)) {
                digest.update(buffer, 0, read);
                outputStream.write(buffer, 0, read);
            }
        } finally {
            closeSilently(inputStream);
            closeSilently(outputStream);
        }
        return String.format("%1$0" + hashLength + "x", new BigInteger(1, digest.digest()));
    }

    public ProgressContainer getMonitor() {
        return this.monitor;
    }

    public long getExpectedSize() {
        return this.expectedSize;
    }

    public void setExpectedSize(final long expectedSize) {
        this.expectedSize = expectedSize;
    }

    public abstract String download() throws IOException;

    protected void updateExpectedSize(final HttpURLConnection connection) {
        if (this.expectedSize == 0L) {
            this.monitor.setTotal(connection.getContentLength());
            this.setExpectedSize(connection.getContentLength());
        } else {
            this.monitor.setTotal(this.expectedSize);
        }
    }

    protected HttpURLConnection makeConnection(final URL url) throws IOException {
        final HttpURLConnection connection = (HttpURLConnection) url.openConnection(this.proxy);
        connection.setUseCaches(false);
        connection.setDefaultUseCaches(false);
        connection.setRequestProperty("Cache-Control", "no-store,max-age=0,no-cache");
        connection.setRequestProperty("Expires", "0");
        connection.setRequestProperty("Pragma", "no-cache");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(30000);
        return connection;
    }

    public URL getUrl() {
        return this.url;
    }

    public File getTarget() {
        return this.target;
    }

    boolean shouldIgnoreLocal() {
        return this.forceDownload;
    }

    public int getNumAttempts() {
        return this.numAttempts;
    }

    public Proxy getProxy() {
        return this.proxy;
    }

    protected void ensureFileWritable(final File target) {
        if (target.getParentFile() != null && !target.getParentFile().isDirectory()) {
            Downloadable.LOGGER.info("Making directory " + target.getParentFile());
            if (!target.getParentFile().mkdirs() && !target.getParentFile().isDirectory()) {
                throw new RuntimeException("Could not create directory " + target.getParentFile());
            }
        }
        if (target.isFile() && !target.canWrite()) {
            throw new RuntimeException("Do not have write permissions for " + target + " - aborting!");
        }
    }

    public long getStartTime() {
        return this.startTime;
    }

    public void setStartTime(final long startTime) {
        this.startTime = startTime;
    }

    public String getStatus() {
        return "Downloading " + this.getTarget().getName();
    }

    public long getEndTime() {
        return this.endTime;
    }

    public void setEndTime(final long endTime) {
        this.endTime = endTime;
    }
}
