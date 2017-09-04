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

public class LocalVersionList
        extends FileBasedVersionList {
    private static final Logger LOGGER;

    static {
        LOGGER = LogManager.getLogger();
    }

    private final File baseDirectory;
    private final File baseVersionsDir;

    public LocalVersionList(File baseDirectory) {
        if ((baseDirectory == null) || (!baseDirectory.isDirectory())) {
            throw new IllegalArgumentException("Base directory is not a folder!");
        }
        this.baseDirectory = baseDirectory;
        this.baseVersionsDir = new File(this.baseDirectory, "versions");
        if (!this.baseVersionsDir.isDirectory()) {
            this.baseVersionsDir.mkdirs();
        }
    }

    protected InputStream getFileInputStream(String path)
            throws FileNotFoundException {
        return new FileInputStream(new File(this.baseDirectory, path));
    }

    public void refreshVersions()
            throws IOException {
        clearCache();

        File[] files = this.baseVersionsDir.listFiles();
        if (files == null) {
            return;
        }
        for (File directory : files) {
            String id = directory.getName();
            File jsonFile = new File(directory, id + ".json");
            if ((directory.isDirectory()) && (jsonFile.exists())) {
                try {
                    String path = "versions/" + id + "/" + id + ".json";
                    CompleteVersion version = (CompleteVersion) this.gson.fromJson(getContent(path), CompleteMinecraftVersion.class);
                    if (version.getType() == null) {
                        LOGGER.warn("Ignoring: " + path + "; it has an invalid version specified");
                        return;
                    }
                    if (version.getId().equals(id)) {
                        addVersion(version);
                    } else {
                        LOGGER.warn("Ignoring: " + path + "; it contains id: '" + version.getId() + "' expected '" + id + "'");
                    }
                } catch (RuntimeException ex) {
                    LOGGER.error("Couldn't load local version " + jsonFile.getAbsolutePath(), ex);
                }
            }
        }
        for (Version version : getVersions()) {
            MinecraftReleaseType type = (MinecraftReleaseType) version.getType();
            if ((getLatestVersion(type) == null) || (getLatestVersion(type).getUpdatedTime().before(version.getUpdatedTime()))) {
                setLatestVersion(version);
            }
        }
    }

    public void saveVersionList()
            throws IOException {
        String text = serializeVersionList();
        PrintWriter writer = new PrintWriter(new File(this.baseVersionsDir, "versions.json"));
        writer.print(text);
        writer.close();
    }

    public void saveVersion(CompleteVersion version)
            throws IOException {
        String text = serializeVersion(version);
        File target = new File(this.baseVersionsDir, version.getId() + "/" + version.getId() + ".json");
        if (target.getParentFile() != null) {
            target.getParentFile().mkdirs();
        }
        PrintWriter writer = new PrintWriter(target);
        writer.print(text);
        writer.close();
    }

    public File getBaseDirectory() {
        return this.baseDirectory;
    }

    public boolean hasAllFiles(CompleteMinecraftVersion version, OperatingSystem os) {
        Set<String> files = version.getRequiredFiles(os);
        for (String file : files) {
            if (!new File(this.baseDirectory, file).isFile()) {
                return false;
            }
        }
        return true;
    }

    public void uninstallVersion(Version version) {
        super.uninstallVersion(version);

        File dir = new File(this.baseVersionsDir, version.getId());
        if (dir.isDirectory()) {
            FileUtils.deleteQuietly(dir);
        }
    }
}