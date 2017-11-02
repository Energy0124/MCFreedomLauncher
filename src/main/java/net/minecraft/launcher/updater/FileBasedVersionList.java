package net.minecraft.launcher.updater;

import com.mojang.launcher.versions.CompleteVersion;
import com.mojang.launcher.versions.Version;
import net.minecraft.launcher.game.MinecraftReleaseType;
import org.apache.commons.io.IOUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import static java.nio.charset.StandardCharsets.UTF_8;

public abstract class FileBasedVersionList extends VersionList {
    String getContent(final String path) throws IOException {
        return IOUtils.toString(this.getFileInputStream(path), UTF_8).replaceAll("\\r\\n", "\r").replaceAll("\\r", "\n");
    }

    protected abstract InputStream getFileInputStream(final String p0) throws FileNotFoundException;

    @Override
    public CompleteMinecraftVersion getCompleteVersion(final Version version) throws IOException {
        if (version instanceof CompleteVersion) {
            return (CompleteMinecraftVersion) version;
        }
        if (!(version instanceof PartialVersion)) {
            throw new IllegalArgumentException("Version must be a partial");
        }
        final PartialVersion partial = (PartialVersion) version;
        final CompleteMinecraftVersion complete = this.gson.fromJson(this.getContent("versions/" + version.getId() + "/" + version.getId() + ".json"), CompleteMinecraftVersion.class);
        final MinecraftReleaseType type = (MinecraftReleaseType) version.getType();
        this.replacePartialWithFull(partial, complete);
        return complete;
    }
}
