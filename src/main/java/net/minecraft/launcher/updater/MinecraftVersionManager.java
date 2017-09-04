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

public class MinecraftVersionManager
        implements VersionManager {
    private static final Logger LOGGER;

    static {
        LOGGER = LogManager.getLogger();
    }

    private final VersionList localVersionList;
    private final VersionList remoteVersionList;
    private final ThreadPoolExecutor executorService = new ExceptionalThreadPoolExecutor(4, 8, 30L, TimeUnit.SECONDS);
    private final List<RefreshedVersionsListener> refreshedVersionsListeners = Collections.synchronizedList(new ArrayList());
    private final Object refreshLock = new Object();
    private final Gson gson = new Gson();
    private boolean isRefreshing;

    public MinecraftVersionManager(VersionList localVersionList, VersionList remoteVersionList) {
        this.localVersionList = localVersionList;
        this.remoteVersionList = remoteVersionList;
    }

    public void refreshVersions()
            throws IOException {
        synchronized (this.refreshLock) {
            this.isRefreshing = true;
        }
        try {
            LOGGER.info("Refreshing local version list...");
            this.localVersionList.refreshVersions();
            LOGGER.info("Refreshing remote version list...");
            this.remoteVersionList.refreshVersions();
        } catch (IOException ex) {
            synchronized (this.refreshLock) {
                this.isRefreshing = false;
            }
            throw ex;
        }
        LOGGER.info("Refresh complete.");
        synchronized (this.refreshLock) {
            this.isRefreshing = false;
        }
        for (RefreshedVersionsListener listener : Lists.newArrayList(this.refreshedVersionsListeners)) {
            listener.onVersionsRefreshed(this);
        }
    }

    public List<VersionSyncInfo> getVersions() {
        return getVersions(null);
    }

    public List<VersionSyncInfo> getVersions(VersionFilter<? extends ReleaseType> filter) {
        synchronized (this.refreshLock) {
            if (this.isRefreshing) {
                return new ArrayList();
            }
        }
        List<VersionSyncInfo> result = new ArrayList();
        Object lookup = new HashMap();
        Map<MinecraftReleaseType, Integer> counts = Maps.newEnumMap(MinecraftReleaseType.class);
        for (MinecraftReleaseType type : MinecraftReleaseType.values()) {
            counts.put(type, Integer.valueOf(0));
        }
        for (Version version : Lists.newArrayList(this.localVersionList.getVersions())) {
            if ((version.getType() != null) && (version.getUpdatedTime() != null)) {
                MinecraftReleaseType type = (MinecraftReleaseType) version.getType();
                if ((filter == null) || ((filter.getTypes().contains(type)) && (((Integer) counts.get(type)).intValue() < filter.getMaxCount()))) {
                    VersionSyncInfo syncInfo = getVersionSyncInfo(version, this.remoteVersionList.getVersion(version.getId()));
                    ((Map) lookup).put(version.getId(), syncInfo);
                    result.add(syncInfo);
                }
            }
        }
        for (Version version : this.remoteVersionList.getVersions()) {
            if ((version.getType() != null) && (version.getUpdatedTime() != null)) {
                MinecraftReleaseType type = (MinecraftReleaseType) version.getType();
                if ((!((Map) lookup).containsKey(version.getId())) && (
                        (filter == null) || ((filter.getTypes().contains(type)) && (((Integer) counts.get(type)).intValue() < filter.getMaxCount())))) {
                    VersionSyncInfo syncInfo = getVersionSyncInfo(this.localVersionList.getVersion(version.getId()), version);
                    ((Map) lookup).put(version.getId(), syncInfo);
                    result.add(syncInfo);
                    if (filter != null) {
                        counts.put(type, Integer.valueOf(((Integer) counts.get(type)).intValue() + 1));
                    }
                }
            }
        }
        if (result.isEmpty()) {
            for (Version version : this.localVersionList.getVersions()) {
                if ((version.getType() != null) && (version.getUpdatedTime() != null)) {
                    VersionSyncInfo syncInfo = getVersionSyncInfo(version, this.remoteVersionList.getVersion(version.getId()));
                    ((Map) lookup).put(version.getId(), syncInfo);
                    result.add(syncInfo);
                }
            }
        }
        Collections.sort(result, new Comparator<VersionSyncInfo>() {
            public int compare(VersionSyncInfo a, VersionSyncInfo b) {
                Version aVer = a.getLatestVersion();
                Version bVer = b.getLatestVersion();
                if ((aVer.getReleaseTime() != null) && (bVer.getReleaseTime() != null)) {
                    return bVer.getReleaseTime().compareTo(aVer.getReleaseTime());
                }
                return bVer.getUpdatedTime().compareTo(aVer.getUpdatedTime());
            }
        });
        return result;
    }

    public VersionSyncInfo getVersionSyncInfo(Version version) {
        return getVersionSyncInfo(version.getId());
    }

    public VersionSyncInfo getVersionSyncInfo(String name) {
        return getVersionSyncInfo(this.localVersionList.getVersion(name), this.remoteVersionList.getVersion(name));
    }

    public VersionSyncInfo getVersionSyncInfo(Version localVersion, Version remoteVersion) {
        boolean installed = localVersion != null;
        boolean upToDate = installed;
        CompleteMinecraftVersion resolved = null;
        if ((installed) && (remoteVersion != null)) {
            upToDate = !remoteVersion.getUpdatedTime().after(localVersion.getUpdatedTime());
        }
        if ((localVersion instanceof CompleteVersion)) {
            try {
                resolved = ((CompleteMinecraftVersion) localVersion).resolve(this);
            } catch (IOException ex) {
                LOGGER.error("Couldn't resolve version " + localVersion.getId(), ex);
                resolved = (CompleteMinecraftVersion) localVersion;
            }
            upToDate &= this.localVersionList.hasAllFiles(resolved, OperatingSystem.getCurrentPlatform());
        }
        return new VersionSyncInfo(resolved, remoteVersion, installed, upToDate);
    }

    public List<VersionSyncInfo> getInstalledVersions() {
        List<VersionSyncInfo> result = new ArrayList();
        for (Version version : this.localVersionList.getVersions()) {
            if ((version.getType() != null) && (version.getUpdatedTime() != null)) {
                VersionSyncInfo syncInfo = getVersionSyncInfo(version, this.remoteVersionList.getVersion(version.getId()));
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

    public CompleteMinecraftVersion getLatestCompleteVersion(VersionSyncInfo syncInfo)
            throws IOException {
        if (syncInfo.getLatestSource() == VersionSyncInfo.VersionSource.REMOTE) {
            CompleteMinecraftVersion result = null;
            IOException exception = null;
            try {
                result = this.remoteVersionList.getCompleteVersion(syncInfo.getLatestVersion());
            } catch (IOException e) {
                exception = e;
                try {
                    result = this.localVersionList.getCompleteVersion(syncInfo.getLatestVersion());
                } catch (IOException localIOException1) {
                }
            }
            if (result != null) {
                return result;
            }
            throw exception;
        }
        return this.localVersionList.getCompleteVersion(syncInfo.getLatestVersion());
    }

    public DownloadJob downloadVersion(VersionSyncInfo syncInfo, DownloadJob job)
            throws IOException {
        if (!(this.localVersionList instanceof LocalVersionList)) {
            throw new IllegalArgumentException("Cannot download if local repo isn't a LocalVersionList");
        }
        if (!(this.remoteVersionList instanceof RemoteVersionList)) {
            throw new IllegalArgumentException("Cannot download if local repo isn't a RemoteVersionList");
        }
        CompleteMinecraftVersion version = getLatestCompleteVersion(syncInfo);
        File baseDirectory = ((LocalVersionList) this.localVersionList).getBaseDirectory();
        Proxy proxy = ((RemoteVersionList) this.remoteVersionList).getProxy();

        job.addDownloadables(version.getRequiredDownloadables(OperatingSystem.getCurrentPlatform(), proxy, baseDirectory, false));
        String jarFile = "versions/" + version.getJar() + "/" + version.getJar() + ".jar";
        job.addDownloadables(new Downloadable[]{new EtagDownloadable(proxy, this.remoteVersionList.getUrl(jarFile), new File(baseDirectory, jarFile), false)});

        return job;
    }

    public DownloadJob downloadResources(DownloadJob job, CompleteVersion version)
            throws IOException {
        File baseDirectory = ((LocalVersionList) this.localVersionList).getBaseDirectory();

        job.addDownloadables(getResourceFiles(((RemoteVersionList) this.remoteVersionList).getProxy(), baseDirectory, (CompleteMinecraftVersion) version));

        return job;
    }

    private Set<Downloadable> getResourceFiles(Proxy proxy, File baseDirectory, CompleteMinecraftVersion version) {
        Set<Downloadable> result = new HashSet();
        InputStream inputStream = null;
        File assets = new File(baseDirectory, "assets");
        File objectsFolder = new File(assets, "objects");
        File indexesFolder = new File(assets, "indexes");
        String indexName = version.getAssets();
        long start = System.nanoTime();
        if (indexName == null) {
            indexName = "legacy";
        }
        File indexFile = new File(indexesFolder, indexName + ".json");
        try {
            URL indexUrl = this.remoteVersionList.getUrl("indexes/" + indexName + ".json");
            inputStream = indexUrl.openConnection(proxy).getInputStream();
            String json = IOUtils.toString(inputStream);
            FileUtils.writeStringToFile(indexFile, json);
            AssetIndex index = (AssetIndex) this.gson.fromJson(json, AssetIndex.class);
            for (Map.Entry<AssetIndex.AssetObject, String> entry : index.getUniqueObjects().entrySet()) {
                AssetIndex.AssetObject object = (AssetIndex.AssetObject) entry.getKey();
                String filename = object.getHash().substring(0, 2) + "/" + object.getHash();
                File file = new File(objectsFolder, filename);
                if ((!file.isFile()) || (FileUtils.sizeOf(file) != object.getSize())) {
                    Downloadable downloadable = new AssetDownloadable(proxy, (String) entry.getValue(), object, "http://resources.download.minecraft.net/", objectsFolder);
                    downloadable.setExpectedSize(object.getSize());
                    result.add(downloadable);
                }
            }
            long end = System.nanoTime();
            long delta = end - start;
            LOGGER.debug("Delta time to compare resources: " + delta / 1000000L + " ms ");
        } catch (Exception ex) {
            LOGGER.error("Couldn't download resources", ex);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
        return result;
    }

    public ThreadPoolExecutor getExecutorService() {
        return this.executorService;
    }

    public void addRefreshedVersionsListener(RefreshedVersionsListener listener) {
        this.refreshedVersionsListeners.add(listener);
    }

    public void removeRefreshedVersionsListener(RefreshedVersionsListener listener) {
        this.refreshedVersionsListeners.remove(listener);
    }

    public VersionSyncInfo syncVersion(VersionSyncInfo syncInfo)
            throws IOException {
        CompleteVersion remoteVersion = getRemoteVersionList().getCompleteVersion(syncInfo.getRemoteVersion());
        getLocalVersionList().removeVersion(syncInfo.getLocalVersion());
        getLocalVersionList().addVersion(remoteVersion);
        ((LocalVersionList) getLocalVersionList()).saveVersion(((CompleteMinecraftVersion) remoteVersion).getSavableVersion());
        return getVersionSyncInfo(remoteVersion);
    }

    public void installVersion(CompleteVersion version)
            throws IOException {
        if ((version instanceof CompleteMinecraftVersion)) {
            version = ((CompleteMinecraftVersion) version).getSavableVersion();
        }
        VersionList localVersionList = getLocalVersionList();
        if (localVersionList.getVersion(version.getId()) != null) {
            localVersionList.removeVersion(version.getId());
        }
        localVersionList.addVersion(version);
        if ((localVersionList instanceof LocalVersionList)) {
            ((LocalVersionList) localVersionList).saveVersion(version);
        }
        LOGGER.info("Installed " + version);
    }

    public void uninstallVersion(CompleteVersion version)
            throws IOException {
        VersionList localVersionList = getLocalVersionList();
        if ((localVersionList instanceof LocalVersionList)) {
            localVersionList.uninstallVersion(version);
            LOGGER.info("Uninstalled " + version);
        }
    }
}