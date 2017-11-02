package com.mojang.launcher.updater.download.assets;

import com.mojang.launcher.updater.download.Downloadable;
import com.mojang.launcher.updater.download.MonitoringInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.util.zip.GZIPInputStream;

public class AssetDownloadable extends Downloadable {
    private static final Logger LOGGER = LogManager.getLogger();

    private final String name;
    private final AssetIndex.AssetObject asset;
    private final String urlBase;
    private final File destination;
    private Status status;

    public AssetDownloadable(final Proxy proxy, final String name, final AssetIndex.AssetObject asset, final String urlBase, final File destination) throws MalformedURLException {
        super(proxy, new URL(urlBase + createPathFromHash(asset.getHash())), new File(destination, createPathFromHash(asset.getHash())), false);
        this.status = Status.DOWNLOADING;
        this.name = name;
        this.asset = asset;
        this.urlBase = urlBase;
        this.destination = destination;
    }

    private static String createPathFromHash(final String hash) {
        return hash.substring(0, 2) + "/" + hash;
    }

    @Override
    public String download() throws IOException {
        this.status = Status.DOWNLOADING;
        ++this.numAttempts;
        final File localAsset = this.getTarget();
        final File localCompressed = this.asset.hasCompressedAlternative() ? new File(this.destination, createPathFromHash(this.asset.getCompressedHash())) : null;
        final URL remoteAsset = this.getUrl();
        final URL remoteCompressed = this.asset.hasCompressedAlternative() ? new URL(this.urlBase + createPathFromHash(this.asset.getCompressedHash())) : null;
        this.ensureFileWritable(localAsset);
        if (localCompressed != null) {
            this.ensureFileWritable(localCompressed);
        }
        if (localAsset.isFile()) {
            if (FileUtils.sizeOf(localAsset) == this.asset.getSize()) {
                return "Have local file and it's the same size; assuming it's okay!";
            }
            AssetDownloadable.LOGGER.warn("Had local file but it was the wrong size... had {} but expected {}", FileUtils.sizeOf(localAsset), this.asset.getSize());
            FileUtils.deleteQuietly(localAsset);
            this.status = Status.DOWNLOADING;
        }
        if (localCompressed != null && localCompressed.isFile()) {
            final String localCompressedHash = Downloadable.getDigest(localCompressed, "SHA", 40);
            if (localCompressedHash != null) {
                if (localCompressedHash.equalsIgnoreCase(this.asset.getCompressedHash())) {
                    return this.decompressAsset(localAsset, localCompressed);
                }
            }
            AssetDownloadable.LOGGER.warn("Had local compressed but it was the wrong hash... expected {} but had {}", this.asset.getCompressedHash(), localCompressedHash);
            FileUtils.deleteQuietly(localCompressed);
        }
        if (remoteCompressed != null && localCompressed != null) {
            final HttpURLConnection connection = this.makeConnection(remoteCompressed);
            final int status = connection.getResponseCode();
            if (status != 200) {
                throw new RuntimeException("Server responded with " + status);
            }
            this.updateExpectedSize(connection);
            final InputStream inputStream = new MonitoringInputStream(connection.getInputStream(), this.getMonitor());
            final FileOutputStream outputStream = new FileOutputStream(localCompressed);
            final String hash = Downloadable.copyAndDigest(inputStream, outputStream, "SHA", 40);
            if (hash.equalsIgnoreCase(this.asset.getCompressedHash())) {
                return this.decompressAsset(localAsset, localCompressed);
            }
            FileUtils.deleteQuietly(localCompressed);
            throw new RuntimeException(String.format("Hash did not match downloaded compressed asset (Expected %s, downloaded %s)", this.asset.getCompressedHash(), hash));
        } else {
            final HttpURLConnection connection = this.makeConnection(remoteAsset);
            final int status = connection.getResponseCode();
            if (status != 200) {
                throw new RuntimeException("Server responded with " + status);
            }
            this.updateExpectedSize(connection);
            final InputStream inputStream = new MonitoringInputStream(connection.getInputStream(), this.getMonitor());
            final FileOutputStream outputStream = new FileOutputStream(localAsset);
            final String hash = Downloadable.copyAndDigest(inputStream, outputStream, "SHA", 40);
            if (hash.equalsIgnoreCase(this.asset.getHash())) {
                return "Downloaded asset and hash matched successfully";
            }
            FileUtils.deleteQuietly(localAsset);
            throw new RuntimeException(String.format("Hash did not match downloaded asset (Expected %s, downloaded %s)", this.asset.getHash(), hash));
        }
    }

    @Override
    public String getStatus() {
        return this.status.name + " " + this.name;
    }

    private String decompressAsset(final File localAsset, final File localCompressed) throws IOException {
        this.status = Status.EXTRACTING;
        final OutputStream outputStream = FileUtils.openOutputStream(localAsset);
        final InputStream inputStream = new GZIPInputStream(FileUtils.openInputStream(localCompressed));
        String hash;
        try {
            hash = Downloadable.copyAndDigest(inputStream, outputStream, "SHA", 40);
        } finally {
            try {
                outputStream.close();
                inputStream.close();
            } catch (IOException e) {
                LOGGER.warn("Unable to close input or output stream");
            }
        }
        this.status = Status.DOWNLOADING;
        if (hash.equalsIgnoreCase(this.asset.getHash())) {
            return "Had local compressed asset, unpacked successfully and hash matched";
        }
        FileUtils.deleteQuietly(localAsset);
        throw new RuntimeException("Had local compressed asset but unpacked hash did not match (expected " + this.asset.getHash() + " but had " + hash + ")");
    }

    private enum Status {
        DOWNLOADING("Downloading"),
        EXTRACTING("Extracting");

        private final String name;

        Status(final String name) {
            this.name = name;
        }
    }
}
