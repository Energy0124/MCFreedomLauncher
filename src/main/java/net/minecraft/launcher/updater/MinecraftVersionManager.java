package net.minecraft.launcher.updater;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.mojang.launcher.OperatingSystem;
import com.mojang.launcher.events.RefreshedVersionsListener;
import com.mojang.launcher.updater.ExceptionalThreadPoolExecutor;
import com.mojang.launcher.updater.VersionFilter;
import com.mojang.launcher.updater.VersionManager;
import com.mojang.launcher.updater.VersionSyncInfo;
import com.mojang.launcher.updater.download.DownloadJob;
import com.mojang.launcher.updater.download.Downloadable;
import com.mojang.launcher.updater.download.EtagDownloadable;
import com.mojang.launcher.updater.download.assets.AssetDownloadable;
import com.mojang.launcher.updater.download.assets.AssetIndex;
import com.mojang.launcher.versions.CompleteVersion;
import com.mojang.launcher.versions.ReleaseType;
import com.mojang.launcher.versions.Version;
import net.minecraft.launcher.game.MinecraftReleaseType;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.Proxy;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static java.nio.charset.StandardCharsets.UTF_8;
import static net.minecraft.launcher.LauncherConstants.URL_JAR_FALLBACK;
import static net.minecraft.launcher.LauncherConstants.URL_RESOURCE_BASE;

public class MinecraftVersionManager implements VersionManager {
    private static final Logger LOGGER = LogManager.getLogger();

    private final VersionList localVersionList;
    private final VersionList remoteVersionList;
    private final ThreadPoolExecutor executorService;
    private final List<RefreshedVersionsListener> refreshedVersionsListeners;
    private final Object refreshLock;
    private final Gson gson;
    private boolean isRefreshing;

    public MinecraftVersionManager(final VersionList localVersionList, final VersionList remoteVersionList) {
        this.executorService = new ExceptionalThreadPoolExecutor(4, 8, 30L, TimeUnit.SECONDS);
        this.refreshedVersionsListeners = Collections.synchronizedList(new ArrayList<RefreshedVersionsListener>());
        this.refreshLock = new Object();
        this.gson = new Gson();
        this.localVersionList = localVersionList;
        this.remoteVersionList = remoteVersionList;
    }

    @Override
    public void refreshVersions() throws IOException {
        synchronized (this.refreshLock) {
            this.isRefreshing = true;
        }
        try {
            MinecraftVersionManager.LOGGER.info("Refreshing local version list...");
            this.localVersionList.refreshVersions();
            MinecraftVersionManager.LOGGER.info("Refreshing remote version list...");
            this.remoteVersionList.refreshVersions();
        } catch (IOException ex) {
            synchronized (this.refreshLock) {
                this.isRefreshing = false;
            }
            throw ex;
        }
        MinecraftVersionManager.LOGGER.info("Refresh complete.");
        synchronized (this.refreshLock) {
            this.isRefreshing = false;
        }
        for (final RefreshedVersionsListener listener : Lists.newArrayList(this.refreshedVersionsListeners)) {
            listener.onVersionsRefreshed(this);
        }
    }

    @Override
    public List<VersionSyncInfo> getVersions() {
        return this.getVersions(null);
    }

    @Override
    public List<VersionSyncInfo> getVersions(final VersionFilter<? extends ReleaseType> filter) {
        synchronized (this.refreshLock) {
            if (this.isRefreshing) {
                return new ArrayList<>();
            }
        }
        final List<VersionSyncInfo> result = new ArrayList<>();
        final Map<String, VersionSyncInfo> lookup = new HashMap<>();
        final Map<MinecraftReleaseType, Integer> counts = Maps.newEnumMap(MinecraftReleaseType.class);
        for (final MinecraftReleaseType type : MinecraftReleaseType.values()) {
            counts.put(type, 0);
        }
        for (final Version version : Lists.newArrayList(this.localVersionList.getVersions())) {
            if (version.getType() != null) {
                if (version.getUpdatedTime() == null) {
                    continue;
                }
                final MinecraftReleaseType type2 = (MinecraftReleaseType) version.getType();
                if (filter != null) {
                    if (!filter.getTypes().contains(type2)) {
                        continue;
                    }
                    if (counts.get(type2) >= filter.getMaxCount()) {
                        continue;
                    }
                }
                final VersionSyncInfo syncInfo = this.getVersionSyncInfo(version, this.remoteVersionList.getVersion(version.getId()));
                lookup.put(version.getId(), syncInfo);
                result.add(syncInfo);
            }
        }
        for (final Version version : this.remoteVersionList.getVersions()) {
            if (version.getType() != null) {
                if (version.getUpdatedTime() == null) {
                    continue;
                }
                final MinecraftReleaseType type2 = (MinecraftReleaseType) version.getType();
                if (lookup.containsKey(version.getId())) {
                    continue;
                }
                if (filter != null) {
                    if (!filter.getTypes().contains(type2)) {
                        continue;
                    }
                    if (counts.get(type2) >= filter.getMaxCount()) {
                        continue;
                    }
                }
                final VersionSyncInfo syncInfo = this.getVersionSyncInfo(this.localVersionList.getVersion(version.getId()), version);
                lookup.put(version.getId(), syncInfo);
                result.add(syncInfo);
                if (filter == null) {
                    continue;
                }
                counts.put(type2, counts.get(type2) + 1);
            }
        }
        if (result.isEmpty()) {
            for (final Version version : this.localVersionList.getVersions()) {
                if (version.getType() != null) {
                    if (version.getUpdatedTime() == null) {
                        continue;
                    }
                    final VersionSyncInfo syncInfo2 = this.getVersionSyncInfo(version, this.remoteVersionList.getVersion(version.getId()));
                    lookup.put(version.getId(), syncInfo2);
                    result.add(syncInfo2);
                    break;
                }
            }
        }
        result.sort((a, b) -> {
            final Version aVer = a.getLatestVersion();
            final Version bVer = b.getLatestVersion();
            if (aVer.getReleaseTime() != null && bVer.getReleaseTime() != null) {
                return bVer.getReleaseTime().compareTo(aVer.getReleaseTime());
            }
            return bVer.getUpdatedTime().compareTo(aVer.getUpdatedTime());
        });
        return result;
    }

    @Override
    public VersionSyncInfo getVersionSyncInfo(final Version version) {
        return this.getVersionSyncInfo(version.getId());
    }

    @Override
    public VersionSyncInfo getVersionSyncInfo(final String name) {
        return this.getVersionSyncInfo(this.localVersionList.getVersion(name), this.remoteVersionList.getVersion(name));
    }

    @Override
    public VersionSyncInfo getVersionSyncInfo(final Version localVersion, final Version remoteVersion) {
        boolean upToDate;
        final boolean installed = upToDate = (localVersion != null);
        CompleteMinecraftVersion resolved = null;
        if (installed && remoteVersion != null) {
            upToDate = !remoteVersion.getUpdatedTime().after(localVersion.getUpdatedTime());
        }
        if (localVersion instanceof CompleteVersion) {
            try {
                resolved = ((CompleteMinecraftVersion) localVersion).resolve(this);
            } catch (IOException ex) {
                MinecraftVersionManager.LOGGER.error("Couldn't resolve version " + localVersion.getId(), ex);
                resolved = (CompleteMinecraftVersion) localVersion;
            }
            upToDate &= this.localVersionList.hasAllFiles(resolved, OperatingSystem.getCurrentPlatform());
        }
        return new VersionSyncInfo(resolved, remoteVersion, installed, upToDate);
    }

    @Override
    public List<VersionSyncInfo> getInstalledVersions() {
        final List<VersionSyncInfo> result = new ArrayList<>();
        final Collection<Version> versions = Lists.newArrayList(this.localVersionList.getVersions());
        for (final Version version : versions) {
            if (version.getType() != null) {
                if (version.getUpdatedTime() == null) {
                    continue;
                }
                final VersionSyncInfo syncInfo = this.getVersionSyncInfo(version, this.remoteVersionList.getVersion(version.getId()));
                result.add(syncInfo);
            }
        }
        return result;
    }

    public VersionList getRemoteVersionList() {
        return this.remoteVersionList;
    }

    public VersionList getLocalVersionList() {
        return this.localVersionList;
    }

    @Override
    public CompleteMinecraftVersion getLatestCompleteVersion(final VersionSyncInfo syncInfo) throws IOException {
        if (syncInfo.getLatestSource() != VersionSyncInfo.VersionSource.REMOTE) {
            return this.localVersionList.getCompleteVersion(syncInfo.getLatestVersion());
        }
        CompleteMinecraftVersion result = null;
        IOException exception = null;
        try {
            result = this.remoteVersionList.getCompleteVersion(syncInfo.getLatestVersion());
        } catch (IOException e) {
            exception = e;
            try {
                result = this.localVersionList.getCompleteVersion(syncInfo.getLatestVersion());
            } catch (IOException ex) {
                LOGGER.debug("An IOException is caught!");
            }
        }
        if (result != null) {
            return result;
        }
        throw exception;

    }

    @Override
    public void downloadVersion(final VersionSyncInfo syncInfo, final DownloadJob job) throws IOException {
        if (!(this.localVersionList instanceof LocalVersionList)) {
            throw new IllegalArgumentException("Cannot download if local repo isn't a LocalVersionList");
        }
        if (!(this.remoteVersionList instanceof RemoteVersionList)) {
            throw new IllegalArgumentException("Cannot download if local repo isn't a RemoteVersionList");
        }
        final CompleteMinecraftVersion version = this.getLatestCompleteVersion(syncInfo);
        final File baseDirectory = ((LocalVersionList) this.localVersionList).getBaseDirectory();
        final Proxy proxy = ((RemoteVersionList) this.remoteVersionList).getProxy();
        job.addDownloadables(version.getRequiredDownloadables(OperatingSystem.getCurrentPlatform(), proxy, baseDirectory, false));
        final String jarFile = "versions/" + version.getJar() + "/" + version.getJar() + ".jar";
        final DownloadInfo clientInfo = version.getDownloadURL(DownloadType.CLIENT);
        if (clientInfo == null) {
            job.addDownloadables(new EtagDownloadable(proxy, new URL(URL_JAR_FALLBACK + jarFile), new File(baseDirectory, jarFile), false));
        } else {
            job.addDownloadables(new PreHashedDownloadable(proxy, clientInfo.getUrl(), new File(baseDirectory, jarFile), false, clientInfo.getSha1()));
        }
    }

    @Override
    public void downloadResources(final DownloadJob job, final CompleteVersion version) {
        final File baseDirectory = ((LocalVersionList) this.localVersionList).getBaseDirectory();
        job.addDownloadables(this.getResourceFiles(((RemoteVersionList) this.remoteVersionList).getProxy(), baseDirectory, (CompleteMinecraftVersion) version));
    }

    private Set<Downloadable> getResourceFiles(final Proxy proxy, final File baseDirectory, final CompleteMinecraftVersion version) {
        final Set<Downloadable> result = new HashSet<>();
        InputStream inputStream = null;
        final File assets = new File(baseDirectory, "assets");
        final File objectsFolder = new File(assets, "objects");
        final File indexesFolder = new File(assets, "indexes");
        final long start = System.nanoTime();
        final AssetIndexInfo indexInfo = version.getAssetIndex();
        final File indexFile = new File(indexesFolder, indexInfo.getId() + ".json");
        try {
            final URL indexUrl = indexInfo.getUrl();
            inputStream = indexUrl.openConnection(proxy).getInputStream();
            final String json = IOUtils.toString(inputStream, UTF_8);
            FileUtils.writeStringToFile(indexFile, json, UTF_8);
            final AssetIndex index = this.gson.fromJson(json, AssetIndex.class);
            for (final Map.Entry<AssetIndex.AssetObject, String> entry : index.getUniqueObjects().entrySet()) {
                final AssetIndex.AssetObject object = entry.getKey();
                final String filename = object.getHash().substring(0, 2) + "/" + object.getHash();
                final File file = new File(objectsFolder, filename);
                if (!file.isFile() || FileUtils.sizeOf(file) != object.getSize()) {
                    final Downloadable downloadable = new AssetDownloadable(proxy, entry.getValue(), object, URL_RESOURCE_BASE, objectsFolder);
                    downloadable.setExpectedSize(object.getSize());
                    result.add(downloadable);
                }
            }
            final long end = System.nanoTime();
            final long delta = end - start;
            MinecraftVersionManager.LOGGER.debug("Delta time to compare resources: " + delta / 1000000L + " ms ");
        } catch (Exception ex) {
            MinecraftVersionManager.LOGGER.error("Couldn't download resources", ex);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
        return result;
    }

    @Override
    public ThreadPoolExecutor getExecutorService() {
        return this.executorService;
    }

    @Override
    public void addRefreshedVersionsListener(final RefreshedVersionsListener listener) {
        this.refreshedVersionsListeners.add(listener);
    }

    @Override
    public void removeRefreshedVersionsListener(final RefreshedVersionsListener listener) {
        this.refreshedVersionsListeners.remove(listener);
    }

    @Override
    public VersionSyncInfo syncVersion(final VersionSyncInfo syncInfo) throws IOException {
        final CompleteVersion remoteVersion = this.getRemoteVersionList().getCompleteVersion(syncInfo.getRemoteVersion());
        this.getLocalVersionList().removeVersion(syncInfo.getLocalVersion());
        this.getLocalVersionList().addVersion(remoteVersion);
        ((LocalVersionList) this.getLocalVersionList()).saveVersion(((CompleteMinecraftVersion) remoteVersion).getSavableVersion());
        return this.getVersionSyncInfo(remoteVersion);
    }

    @Override
    public void installVersion(CompleteVersion version) throws IOException {
        if (version instanceof CompleteMinecraftVersion) {
            version = ((CompleteMinecraftVersion) version).getSavableVersion();
        }
        final VersionList localVersionList = this.getLocalVersionList();
        if (localVersionList.getVersion(version.getId()) != null) {
            localVersionList.removeVersion(version.getId());
        }
        localVersionList.addVersion(version);
        if (localVersionList instanceof LocalVersionList) {
            ((LocalVersionList) localVersionList).saveVersion(version);
        }
        MinecraftVersionManager.LOGGER.info("Installed " + version);
    }

    @Override
    public void uninstallVersion(final CompleteVersion version) {
        final VersionList localVersionList = this.getLocalVersionList();
        if (localVersionList instanceof LocalVersionList) {
            localVersionList.uninstallVersion(version);
            MinecraftVersionManager.LOGGER.info("Uninstalled " + version);
        }
    }
}
