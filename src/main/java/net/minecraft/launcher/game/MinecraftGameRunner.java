package net.minecraft.launcher.game;

import com.google.common.base.MoreObjects;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.authlib.UserAuthentication;
import com.mojang.authlib.UserType;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.authlib.yggdrasil.YggdrasilUserAuthentication;
import com.mojang.launcher.LegacyPropertyMapSerializer;
import com.mojang.launcher.OperatingSystem;
import com.mojang.launcher.game.GameInstanceStatus;
import com.mojang.launcher.game.process.GameProcess;
import com.mojang.launcher.game.process.GameProcessBuilder;
import com.mojang.launcher.game.process.GameProcessFactory;
import com.mojang.launcher.game.process.GameProcessRunnable;
import com.mojang.launcher.game.process.direct.DirectGameProcessFactory;
import com.mojang.launcher.game.runner.AbstractGameRunner;
import com.mojang.launcher.updater.DateTypeAdapter;
import com.mojang.launcher.updater.VersionSyncInfo;
import com.mojang.launcher.updater.download.Downloadable;
import com.mojang.launcher.updater.download.assets.AssetIndex;
import com.mojang.launcher.versions.ExtractRules;
import com.mojang.util.UUIDTypeAdapter;
import net.minecraft.launcher.Launcher;
import net.minecraft.launcher.profile.LauncherVisibilityRule;
import net.minecraft.launcher.profile.Profile;
import net.minecraft.launcher.updater.CompleteMinecraftVersion;
import net.minecraft.launcher.updater.Library;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StrSubstitutor;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static java.nio.charset.StandardCharsets.UTF_8;
import static net.minecraft.launcher.LauncherConstants.IMAGE_MACOS_FAVICON;
import static net.minecraft.launcher.LauncherConstants.JVM_ARGUMENTS_DEFAULT;

public class MinecraftGameRunner extends AbstractGameRunner implements GameProcessRunnable {
    private static final String CRASH_IDENTIFIER_MAGIC = "#@!@#";
    private final Gson gson;
    private final DateTypeAdapter dateAdapter;
    private final Launcher minecraftLauncher;
    private final String[] additionalLaunchArgs;
    private final GameProcessFactory processFactory;
    private File nativeDir;
    private LauncherVisibilityRule visibilityRule;
    private UserAuthentication auth;
    private Profile selectedProfile;

    public MinecraftGameRunner(final Launcher minecraftLauncher, final String[] additionalLaunchArgs) {
        this.gson = new Gson();
        this.dateAdapter = new DateTypeAdapter();
        this.processFactory = new DirectGameProcessFactory();
        this.visibilityRule = LauncherVisibilityRule.CLOSE_LAUNCHER;
        this.minecraftLauncher = minecraftLauncher;
        this.additionalLaunchArgs = additionalLaunchArgs;
    }

    @Override
    protected void setStatus(final GameInstanceStatus status) {
        synchronized (this.lock) {
            if (this.nativeDir != null && status == GameInstanceStatus.IDLE) {
                MinecraftGameRunner.LOGGER.info("Deleting " + this.nativeDir);
                if (!this.nativeDir.isDirectory() || FileUtils.deleteQuietly(this.nativeDir)) {
                    this.nativeDir = null;
                } else {
                    MinecraftGameRunner.LOGGER.warn("Couldn't delete " + this.nativeDir + " - scheduling for deletion upon exit");
                    try {
                        FileUtils.forceDeleteOnExit(this.nativeDir);
                    } catch (Throwable t) {
                        LOGGER.debug("A throwable is caught!");
                    }
                }
            }
            super.setStatus(status);
        }
    }

    @Override
    protected com.mojang.launcher.Launcher getLauncher() {
        return this.minecraftLauncher.getLauncher();
    }

    @Override
    protected void downloadRequiredFiles(final VersionSyncInfo syncInfo) {
        this.migrateOldAssets();
        super.downloadRequiredFiles(syncInfo);
    }

    @Override
    protected void launchGame() throws IOException {
        MinecraftGameRunner.LOGGER.info("Launching game");
        this.selectedProfile = this.minecraftLauncher.getProfileManager().getSelectedProfile();
        this.auth = this.minecraftLauncher.getProfileManager().getAuthDatabase().getByUUID(this.minecraftLauncher.getProfileManager().getSelectedUser());
        if (this.getVersion() == null) {
            MinecraftGameRunner.LOGGER.error("Aborting launch; version is null?");
            return;
        }
        this.nativeDir = new File(this.getLauncher().getWorkingDirectory(), "versions/" + this.getVersion().getId() + "/" + this.getVersion().getId() + "-natives-" + System.nanoTime());
        if (!this.nativeDir.isDirectory()) {
            boolean success = this.nativeDir.mkdirs();
            if (!success) {
                LOGGER.error("Unable to create directories");
            }
        }
        MinecraftGameRunner.LOGGER.info("Unpacking natives to " + this.nativeDir);
        try {
            this.unpackNatives(this.nativeDir);
        } catch (IOException e) {
            MinecraftGameRunner.LOGGER.error("Couldn't unpack natives!", e);
            return;
        }
        File assetsDir;
        try {
            assetsDir = this.reconstructAssets();
        } catch (IOException e2) {
            MinecraftGameRunner.LOGGER.error("Couldn't unpack natives!", e2);
            return;
        }
        final File gameDirectory = (this.selectedProfile.getGameDir() == null) ? this.getLauncher().getWorkingDirectory() : this.selectedProfile.getGameDir();
        MinecraftGameRunner.LOGGER.info("Launching in " + gameDirectory);
        if (!gameDirectory.exists()) {
            if (!gameDirectory.mkdirs()) {
                MinecraftGameRunner.LOGGER.error("Aborting launch; couldn't create game directory");
                return;
            }
        } else if (!gameDirectory.isDirectory()) {
            MinecraftGameRunner.LOGGER.error("Aborting launch; game directory is not actually a directory");
            return;
        }
        final File serverResourcePacksDir = new File(gameDirectory, "server-resource-packs");
        if (!serverResourcePacksDir.exists()) {
            boolean success = serverResourcePacksDir.mkdirs();
            if (!success) {
                LOGGER.error("Unable to create directories");
            }
        }
        final GameProcessBuilder processBuilder = new GameProcessBuilder(MoreObjects.firstNonNull(this.selectedProfile.getJavaPath(), OperatingSystem.getCurrentPlatform().getJavaDir()));
        processBuilder.withSysOutFilter(input -> input.contains(CRASH_IDENTIFIER_MAGIC));
        processBuilder.directory(gameDirectory);
        processBuilder.withLogProcessor(this.minecraftLauncher.getUserInterface().showGameOutputTab(this));
        final OperatingSystem os = OperatingSystem.getCurrentPlatform();
        if (os.equals(OperatingSystem.MACOS)) {
            processBuilder.withArguments("-Xdock:icon=" + this.getAssetObject().getAbsolutePath(), "-Xdock:name=Minecraft");
        }
        final String profileArgs = this.selectedProfile.getJavaArgs();
        if (profileArgs != null) {
            processBuilder.withArguments(profileArgs.split(" "));
        } else {
            processBuilder.withArguments(JVM_ARGUMENTS_DEFAULT.split(" "));
        }
        if (this.minecraftLauncher.usesWinTenHack()) {
            processBuilder.withArguments("-Dos.name=Windows 10");
            processBuilder.withArguments("-Dos.version=10.0");
        }
        processBuilder.withArguments("-Djava.library.path=" + this.nativeDir.getAbsolutePath());
        processBuilder.withArguments("-cp", this.constructClassPath(this.getVersion()));
        processBuilder.withArguments(this.getVersion().getMainClass());
        MinecraftGameRunner.LOGGER.info("Half command: " + StringUtils.join(processBuilder.getFullCommands(), " "));
        final String[] args = this.getMinecraftArguments(this.getVersion(), this.selectedProfile, gameDirectory, assetsDir, this.auth);
        if (args == null) {
            return;
        }
        processBuilder.withArguments(args);
        final Proxy proxy = this.getLauncher().getProxy();
        final PasswordAuthentication proxyAuth = this.getLauncher().getProxyAuth();
        if (!proxy.equals(Proxy.NO_PROXY)) {
            final InetSocketAddress address = (InetSocketAddress) proxy.address();
            processBuilder.withArguments("--proxyHost", address.getHostName());
            processBuilder.withArguments("--proxyPort", Integer.toString(address.getPort()));
            if (proxyAuth != null) {
                processBuilder.withArguments("--proxyUser", proxyAuth.getUserName());
                processBuilder.withArguments("--proxyPass", new String(proxyAuth.getPassword()));
            }
        }
        processBuilder.withArguments(this.additionalLaunchArgs);
        if (this.auth == null || this.auth.getSelectedProfile() == null) {
            processBuilder.withArguments("--demo");
        }
        if (this.selectedProfile.getResolution() != null) {
            processBuilder.withArguments("--width", String.valueOf(this.selectedProfile.getResolution().getWidth()));
            processBuilder.withArguments("--height", String.valueOf(this.selectedProfile.getResolution().getHeight()));
        }
        try {
            MinecraftGameRunner.LOGGER.debug("Running " + StringUtils.join(processBuilder.getFullCommands(), " "));
            final GameProcess process = this.processFactory.startGame(processBuilder);
            process.setExitRunnable(this);
            this.setStatus(GameInstanceStatus.PLAYING);
            if (this.visibilityRule != LauncherVisibilityRule.DO_NOTHING) {
                this.minecraftLauncher.getUserInterface().setVisible(false);
            }
        } catch (IOException e3) {
            MinecraftGameRunner.LOGGER.error("Couldn't launch game", e3);
            this.setStatus(GameInstanceStatus.IDLE);
            return;
        }
        this.minecraftLauncher.performCleanups();
    }

    private CompleteMinecraftVersion getVersion() {
        return (CompleteMinecraftVersion) this.version;
    }

    private File getAssetObject() throws IOException {
        final File assetsDir = new File(this.getLauncher().getWorkingDirectory(), "assets");
        final File indexDir = new File(assetsDir, "indexes");
        final File objectsDir = new File(assetsDir, "objects");
        final String assetVersion = this.getVersion().getAssetIndex().getId();
        final File indexFile = new File(indexDir, assetVersion + ".json");
        final AssetIndex index = this.gson.fromJson(FileUtils.readFileToString(indexFile, UTF_8), AssetIndex.class);
        final String hash = index.getFileMap().get(IMAGE_MACOS_FAVICON).getHash();
        return new File(objectsDir, hash.substring(0, 2) + "/" + hash);
    }

    private File reconstructAssets() throws IOException {
        final File assetsDir = new File(this.getLauncher().getWorkingDirectory(), "assets");
        final File indexDir = new File(assetsDir, "indexes");
        final File objectDir = new File(assetsDir, "objects");
        final String assetVersion = this.getVersion().getAssetIndex().getId();
        final File indexFile = new File(indexDir, assetVersion + ".json");
        final File virtualRoot = new File(new File(assetsDir, "virtual"), assetVersion);
        if (!indexFile.isFile()) {
            MinecraftGameRunner.LOGGER.warn("No assets index file " + virtualRoot + "; can't reconstruct assets");
            return virtualRoot;
        }
        final AssetIndex index = this.gson.fromJson(FileUtils.readFileToString(indexFile, UTF_8), AssetIndex.class);
        if (index.isVirtual()) {
            MinecraftGameRunner.LOGGER.info("Reconstructing virtual assets folder at " + virtualRoot);
            for (final Map.Entry<String, AssetIndex.AssetObject> entry : index.getFileMap().entrySet()) {
                final File target = new File(virtualRoot, entry.getKey());
                final File original = new File(new File(objectDir, entry.getValue().getHash().substring(0, 2)), entry.getValue().getHash());
                if (!target.isFile()) {
                    FileUtils.copyFile(original, target, false);
                }
            }
            FileUtils.writeStringToFile(new File(virtualRoot, ".lastused"), this.dateAdapter.serializeToString(new Date()), UTF_8);
        }
        return virtualRoot;
    }

    private String[] getMinecraftArguments(final CompleteMinecraftVersion version, final Profile selectedProfile, final File gameDirectory, final File assetsDirectory, final UserAuthentication authentication) {
        if (version.getMinecraftArguments() == null) {
            MinecraftGameRunner.LOGGER.error("Can't run version, missing minecraftArguments");
            this.setStatus(GameInstanceStatus.IDLE);
            return null;
        }
        final Map<String, String> map = new HashMap<>();
        final StrSubstitutor substitutor = new StrSubstitutor(map);
        final String[] split = version.getMinecraftArguments().split(" ");
        map.put("auth_access_token", authentication.getAuthenticatedToken());
        map.put("user_properties", new GsonBuilder().registerTypeAdapter(PropertyMap.class, new LegacyPropertyMapSerializer()).create().toJson(authentication.getUserProperties()));
        map.put("user_property_map", new GsonBuilder().registerTypeAdapter(PropertyMap.class, new PropertyMap.Serializer()).create().toJson(authentication.getUserProperties()));
        if (authentication.isLoggedIn() && authentication.canPlayOnline()) {
            if (authentication instanceof YggdrasilUserAuthentication) {
                map.put("auth_session", String.format("token:%s:%s", authentication.getAuthenticatedToken(), UUIDTypeAdapter.fromUUID(authentication.getSelectedProfile().getId())));
            } else {
                map.put("auth_session", authentication.getAuthenticatedToken());
            }
        } else {
            map.put("auth_session", "-");
        }
        if (authentication.getSelectedProfile() != null) {
            map.put("auth_player_name", authentication.getSelectedProfile().getName());
            map.put("auth_uuid", UUIDTypeAdapter.fromUUID(authentication.getSelectedProfile().getId()));
            map.put("user_type", authentication.getUserType().getName());
        } else {
            map.put("auth_player_name", "Player");
            map.put("auth_uuid", new UUID(0L, 0L).toString());
            map.put("user_type", UserType.LEGACY.getName());
        }
        map.put("profile_name", selectedProfile.getName());
        map.put("version_name", version.getId());
        map.put("game_directory", gameDirectory.getAbsolutePath());
        map.put("game_assets", assetsDirectory.getAbsolutePath());
        map.put("assets_root", new File(this.getLauncher().getWorkingDirectory(), "assets").getAbsolutePath());
        map.put("assets_index_name", this.getVersion().getAssetIndex().getId());
        map.put("version_type", this.getVersion().getType().getName());
        for (int i = 0; i < split.length; ++i) {
            split[i] = substitutor.replace(split[i]);
        }
        return split;
    }

    private void migrateOldAssets() {
        final File sourceDir = new File(this.getLauncher().getWorkingDirectory(), "assets");
        final File objectsDir = new File(sourceDir, "objects");
        if (!sourceDir.isDirectory()) {
            return;
        }
        final IOFileFilter migratableFilter = FileFilterUtils.notFileFilter(FileFilterUtils.or(FileFilterUtils.nameFileFilter("indexes"), FileFilterUtils.nameFileFilter("objects"), FileFilterUtils.nameFileFilter("virtual"), FileFilterUtils.nameFileFilter("skins")));
        for (final File file : (TreeSet<File>) new TreeSet(FileUtils.listFiles(sourceDir, TrueFileFilter.TRUE, migratableFilter))) {
            final String hash = Downloadable.getDigest(file, "SHA-1", 40);
            if (hash != null) {
                final File destinationFile = new File(objectsDir, hash.substring(0, 2) + "/" + hash);
                if (!destinationFile.exists()) {
                    MinecraftGameRunner.LOGGER.info("Migrated old asset {} into {}", file, destinationFile);
                    try {
                        FileUtils.copyFile(file, destinationFile);
                    } catch (IOException e) {
                        MinecraftGameRunner.LOGGER.error("Couldn't migrate old asset", e);
                    }
                }
            }
            FileUtils.deleteQuietly(file);
        }
        final File[] assets = sourceDir.listFiles();
        if (assets != null) {
            for (final File file2 : assets) {
                if (!file2.getName().equals("indexes") && !file2.getName().equals("objects") && !file2.getName().equals("virtual") && !file2.getName().equals("skins")) {
                    MinecraftGameRunner.LOGGER.info("Cleaning up old assets directory {} after migration", file2);
                    FileUtils.deleteQuietly(file2);
                }
            }
        }
    }

    private void unpackNatives(final File targetDir) throws IOException {
        final OperatingSystem os = OperatingSystem.getCurrentPlatform();
        final Collection<Library> libraries = this.getVersion().getRelevantLibraries();
        for (final Library library : libraries) {
            final Map<OperatingSystem, String> nativesPerOs = library.getNatives();
            if (nativesPerOs != null && nativesPerOs.get(os) != null) {
                final File file = new File(this.getLauncher().getWorkingDirectory(), "libraries/" + library.getArtifactPath(nativesPerOs.get(os)));
                final ExtractRules extractRules = library.getExtractRules();
                try (ZipFile zip = new ZipFile(file)) {
                    final Enumeration<? extends ZipEntry> entries = zip.entries();
                    while (entries.hasMoreElements()) {
                        final ZipEntry entry = entries.nextElement();
                        if (extractRules != null && !extractRules.shouldExtract(entry.getName())) {
                            continue;
                        }
                        final File targetFile = new File(targetDir, entry.getName());
                        if (targetFile.getParentFile() != null) {
                            boolean success = targetFile.getParentFile().mkdirs();
                            if (!success) {
                                LOGGER.error("Unable to create directories");
                            }
                        }
                        if (entry.isDirectory()) {
                            continue;
                        }
                        final BufferedInputStream inputStream = new BufferedInputStream(zip.getInputStream(entry));
                        final byte[] buffer = new byte[2048];
                        final FileOutputStream outputStream = new FileOutputStream(targetFile);
                        final BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);
                        try {
                            int length;
                            while ((length = inputStream.read(buffer, 0, buffer.length)) != -1) {
                                bufferedOutputStream.write(buffer, 0, length);
                            }
                        } finally {
                            Downloadable.closeSilently(bufferedOutputStream);
                            Downloadable.closeSilently(outputStream);
                            Downloadable.closeSilently(inputStream);
                        }
                    }
                }
            }
        }
    }

    private String constructClassPath(final CompleteMinecraftVersion version) {
        final StringBuilder result = new StringBuilder();
        final Collection<File> classPath = version.getClassPath(OperatingSystem.getCurrentPlatform(), this.getLauncher().getWorkingDirectory());
        final String separator = System.getProperty("path.separator");
        for (final File file : classPath) {
            if (!file.isFile()) {
                throw new RuntimeException("Classpath file not found: " + file);
            }
            if (result.length() > 0) {
                result.append(separator);
            }
            result.append(file.getAbsolutePath());
        }
        return result.toString();
    }

    @Override
    public void onGameProcessEnded(final GameProcess process) {
        final int exitCode = process.getExitCode();
        if (exitCode == 0) {
            MinecraftGameRunner.LOGGER.info("Game ended with no troubles detected (exit code " + exitCode + ")");
            if (this.visibilityRule == LauncherVisibilityRule.CLOSE_LAUNCHER) {
                MinecraftGameRunner.LOGGER.info("Following visibility rule and exiting launcher as the game has ended");
                this.getLauncher().shutdownLauncher();
            } else if (this.visibilityRule == LauncherVisibilityRule.HIDE_LAUNCHER) {
                MinecraftGameRunner.LOGGER.info("Following visibility rule and showing launcher as the game has ended");
                this.minecraftLauncher.getUserInterface().setVisible(true);
            }
        } else {
            MinecraftGameRunner.LOGGER.error("Game ended with bad state (exit code " + exitCode + ")");
            MinecraftGameRunner.LOGGER.info("Ignoring visibility rule and showing launcher due to a game crash");
            this.minecraftLauncher.getUserInterface().setVisible(true);
            String errorText = null;
            final Collection<String> sysOutLines = process.getSysOutLines();
            final String[] sysOut = sysOutLines.toArray(new String[sysOutLines.size()]);
            for (int i = sysOut.length - 1; i >= 0; --i) {
                final String line = sysOut[i];
                final int pos = line.lastIndexOf(CRASH_IDENTIFIER_MAGIC);
                if (pos >= 0 && pos < line.length() - CRASH_IDENTIFIER_MAGIC.length() - 1) {
                    errorText = line.substring(pos + CRASH_IDENTIFIER_MAGIC.length()).trim();
                    break;
                }
            }
            if (errorText != null) {
                final File file = new File(errorText);
                if (file.isFile()) {
                    MinecraftGameRunner.LOGGER.info("Crash report detected, opening: " + errorText);
                    InputStream inputStream = null;
                    try {
                        inputStream = new FileInputStream(file);
                        final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                        final StringBuilder result = new StringBuilder();
                        String line2;
                        while ((line2 = reader.readLine()) != null) {
                            if (result.length() > 0) {
                                result.append("\n");
                            }
                            result.append(line2);
                        }
                        reader.close();
                        this.minecraftLauncher.getUserInterface().showCrashReport(this.getVersion(), file, result.toString());
                    } catch (IOException e) {
                        MinecraftGameRunner.LOGGER.error("Couldn't open crash report", e);
                    } finally {
                        Downloadable.closeSilently(inputStream);
                    }
                } else {
                    MinecraftGameRunner.LOGGER.error("Crash report detected, but unknown format: " + errorText);
                }
            }
        }
        this.setStatus(GameInstanceStatus.IDLE);
    }

    public void setVisibility(final LauncherVisibilityRule visibility) {
        this.visibilityRule = visibility;
    }

    public UserAuthentication getAuth() {
        return this.auth;
    }

    public Profile getSelectedProfile() {
        return this.selectedProfile;
    }
}
