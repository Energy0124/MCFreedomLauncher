package net.minecraft.launcher.updater;

import com.mojang.launcher.OperatingSystem;
import com.mojang.launcher.versions.CompleteVersion;
import com.mojang.launcher.versions.Version;
import net.minecraft.launcher.game.MinecraftReleaseType;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.Set;

public class LocalVersionList extends FileBasedVersionList {
    private static final Logger LOGGER = LogManager.getLogger();

    private final File baseDirectory;
    private final File baseVersionsDir;

    public LocalVersionList(final File baseDirectory) {
        if (baseDirectory == null || !baseDirectory.isDirectory()) {
            throw new IllegalArgumentException("Base directory is not a folder!");
        }
        this.baseDirectory = baseDirectory;
        this.baseVersionsDir = new File(this.baseDirectory, "versions");
        if (!this.baseVersionsDir.isDirectory()) {
            this.baseVersionsDir.mkdirs();
        }
    }

    @Override
    protected InputStream getFileInputStream(final String path) throws FileNotFoundException {
        return new FileInputStream(new File(this.baseDirectory, path));
    }

    @Override
    public void refreshVersions() throws IOException {
        this.clearCache();
        final File[] files = this.baseVersionsDir.listFiles();
        if (files == null) {
            return;
        }
        for (final File directory : files) {
            final String id = directory.getName();
            final File jsonFile = new File(directory, id + ".json");
            if (directory.isDirectory() && jsonFile.exists()) {
                try {
                    final String path = "versions/" + id + "/" + id + ".json";
                    final CompleteVersion version = this.gson.fromJson(this.getContent(path), CompleteMinecraftVersion.class);
                    if (version.getType() == null) {
                        LocalVersionList.LOGGER.warn("Ignoring: " + path + "; it has an invalid version specified");
                        return;
                    }
                    if (version.getId().equals(id)) {
                        this.addVersion(version);
                    } else {
                        LocalVersionList.LOGGER.warn("Ignoring: " + path + "; it contains id: '" + version.getId() + "' expected '" + id + "'");
                    }
                } catch (RuntimeException ex) {
                    LocalVersionList.LOGGER.error("Couldn't load local version " + jsonFile.getAbsolutePath(), ex);
                }
            }
        }
        for (final Version version2 : this.getVersions()) {
            final MinecraftReleaseType type = (MinecraftReleaseType) version2.getType();
            if (this.getLatestVersion(type) == null || this.getLatestVersion(type).getUpdatedTime().before(version2.getUpdatedTime())) {
                this.setLatestVersion(version2);
            }
        }
    }

    public void saveVersion(final CompleteVersion version) throws IOException {
        final String text = this.serializeVersion(version);
        final File target = new File(this.baseVersionsDir, version.getId() + "/" + version.getId() + ".json");
        if (target.getParentFile() != null) {
            target.getParentFile().mkdirs();
        }
        final PrintWriter writer = new PrintWriter(target);
        writer.print(text);
        writer.close();
    }

    public File getBaseDirectory() {
        return this.baseDirectory;
    }

    @Override
    public boolean hasAllFiles(final CompleteMinecraftVersion version, final OperatingSystem os) {
        final Set<String> files = version.getRequiredFiles(os);
        for (final String file : files) {
            if (!new File(this.baseDirectory, file).isFile()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void uninstallVersion(final Version version) {
        super.uninstallVersion(version);
        final File dir = new File(this.baseVersionsDir, version.getId());
        if (dir.isDirectory()) {
            FileUtils.deleteQuietly(dir);
        }
    }
}
