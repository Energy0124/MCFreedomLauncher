package net.minecraft.launcher;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.mojang.authlib.Agent;
import com.mojang.authlib.UserAuthentication;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.exceptions.InvalidCredentialsException;
import com.mojang.launcher.OperatingSystem;
import com.mojang.launcher.updater.DateTypeAdapter;
import com.mojang.launcher.updater.VersionSyncInfo;
import com.mojang.launcher.updater.download.assets.AssetIndex;
import com.mojang.launcher.versions.CompleteVersion;
import com.mojang.launcher.versions.Version;
import com.mojang.util.UUIDTypeAdapter;
import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import net.minecraft.launcher.game.GameLaunchDispatcher;
import net.minecraft.launcher.game.MinecraftReleaseType;
import net.minecraft.launcher.game.MinecraftReleaseTypeFactory;
import net.minecraft.launcher.profile.AuthenticationDatabase;
import net.minecraft.launcher.profile.Profile;
import net.minecraft.launcher.profile.ProfileManager;
import net.minecraft.launcher.updater.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.text.DateFormat;
import java.util.*;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.text.DateFormat.MEDIUM;
import static java.text.DateFormat.MONTH_FIELD;
import static net.minecraft.launcher.LauncherConstants.FORMAT_VERSION;
import static net.minecraft.launcher.LauncherConstants.VERSION_MINIMUM_SUPPORTED_BOOTSTRAP;

public class Launcher {
    private static final Logger LOGGER = LogManager.getLogger();

    static {
        Thread.currentThread().setContextClassLoader(Launcher.class.getClassLoader());
    }

    private final com.mojang.launcher.Launcher launcher;
    private final Integer bootstrapVersion;
    private final MinecraftUserInterface userInterface;
    private final ProfileManager profileManager;
    private final Gson gson;
    private final GameLaunchDispatcher launchDispatcher;
    private boolean winTenHack;
    private UUID clientToken;
    private String requestedUser;

    public Launcher(final JFrame frame, final File workingDirectory, final Proxy proxy, final PasswordAuthentication proxyAuth, final String[] args) {
        this(frame, workingDirectory, proxy, proxyAuth, args, 0);
    }

    public Launcher(final JFrame frame, final File workingDirectory, final Proxy proxy, final PasswordAuthentication proxyAuth, final String[] args, final Integer bootstrapVersion) {
        this.gson = new Gson();
        this.winTenHack = false;
        this.clientToken = UUID.randomUUID();
        this.setupErrorHandling();
        this.bootstrapVersion = bootstrapVersion;
        this.userInterface = this.selectUserInterface(frame);
        if (bootstrapVersion < VERSION_MINIMUM_SUPPORTED_BOOTSTRAP) {
            this.userInterface.showOutdatedNotice();
            System.exit(0);
            throw new Error("Outdated bootstrap");
        }
        Launcher.LOGGER.info(this.userInterface.getTitle() + " (through bootstrap " + bootstrapVersion + ") started on " + OperatingSystem.getCurrentPlatform().getName() + "...");
        Launcher.LOGGER.info("Current time is " + DateFormat.getDateTimeInstance(MONTH_FIELD, MEDIUM, Locale.US).format(new Date()));
        if (OperatingSystem.getCurrentPlatform().isSupported()) {
            Launcher.LOGGER.fatal("This operating system is unknown or unsupported, we cannot guarantee that the game will launch successfully.");
        }
        Launcher.LOGGER.info("System.getProperty('os.name') == '" + System.getProperty("os.name") + "'");
        Launcher.LOGGER.info("System.getProperty('os.version') == '" + System.getProperty("os.version") + "'");
        Launcher.LOGGER.info("System.getProperty('os.arch') == '" + System.getProperty("os.arch") + "'");
        Launcher.LOGGER.info("System.getProperty('java.version') == '" + System.getProperty("java.version") + "'");
        Launcher.LOGGER.info("System.getProperty('java.vendor') == '" + System.getProperty("java.vendor") + "'");
        Launcher.LOGGER.info("System.getProperty('sun.arch.data.model') == '" + System.getProperty("sun.arch.data.model") + "'");
        Launcher.LOGGER.info("proxy == " + proxy);
        this.launchDispatcher = new GameLaunchDispatcher(this, this.processArgs(args));
        this.launcher = new com.mojang.launcher.Launcher(this.userInterface, workingDirectory, proxy, proxyAuth, new MinecraftVersionManager(new LocalVersionList(workingDirectory), new RemoteVersionList(LauncherConstants.getProperties().getVersionManifest(), proxy)), Agent.MINECRAFT, MinecraftReleaseTypeFactory.instance(), FORMAT_VERSION);
        this.profileManager = new ProfileManager(this);
        ((SwingUserInterface) this.userInterface).initializeFrame();
        this.getLauncher().getVersionManager().getExecutorService().submit(() -> AnalyticsSubmission.sendStartup(proxy));
        this.refreshVersionsAndProfiles();
    }

    private static Collection<File> listEmptyDirectories(final File directory) {
        final List<File> result = Lists.newArrayList();
        final File[] files = directory.listFiles();
        if (files != null) {
            for (final File file : files) {
                if (file.isDirectory()) {
                    final File[] subFiles = file.listFiles();
                    if (subFiles == null || subFiles.length == 0) {
                        result.add(file);
                    } else {
                        result.addAll(listEmptyDirectories(file));
                    }
                }
            }
        }
        return result;
    }

    private static void deleteEmptyDirectories(final File directory) {
        while (true) {
            final Collection<File> files = listEmptyDirectories(directory);
            if (files.isEmpty()) {
                return;
            }
            for (final File file : files) {
                if (!FileUtils.deleteQuietly(file)) {
                    return;
                }
                Launcher.LOGGER.info("Deleted empty directory {}", file);
            }
        }
    }

    private void setupErrorHandling() {
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> Launcher.LOGGER.fatal("Unhandled exception in thread " + t, e));
    }

    private String[] processArgs(final String[] args) {
        final OptionParser optionParser = new OptionParser();
        optionParser.allowsUnrecognizedOptions();
        final OptionSpec<String> userOption = optionParser.accepts("user").withRequiredArg().ofType(String.class);
        final OptionSpec<String> nonOptions = optionParser.nonOptions();
        OptionSet optionSet;
        try {
            optionSet = optionParser.parse(args);
        } catch (OptionException e) {
            return args;
        }
        if (optionSet.has(userOption)) {
            this.requestedUser = optionSet.valueOf(userOption);
        }
        final List<String> remainingOptions = optionSet.valuesOf(nonOptions);
        return remainingOptions.toArray(new String[remainingOptions.size()]);
    }

    public void refreshVersionsAndProfiles() {
        this.getLauncher().getVersionManager().getExecutorService().submit(() -> {
            try {
                Launcher.this.getLauncher().getVersionManager().refreshVersions();
            } catch (Throwable e) {
                Launcher.LOGGER.error("Unexpected exception refreshing version list", e);
            }
            try {
                Launcher.this.profileManager.loadProfiles();
                Launcher.LOGGER.info("Loaded " + Launcher.this.profileManager.getProfiles().size() + " profile(s); selected '" + Launcher.this.profileManager.getSelectedProfile().getName() + "'");
            } catch (Throwable e) {
                Launcher.LOGGER.error("Unexpected exception refreshing profile list", e);
            }
            if (Launcher.this.requestedUser != null) {
                final AuthenticationDatabase authDatabase = Launcher.this.profileManager.getAuthDatabase();
                boolean loggedIn = false;
                try {
                    final String uuid = UUIDTypeAdapter.fromUUID(UUIDTypeAdapter.fromString(Launcher.this.requestedUser));
                    final UserAuthentication auth = authDatabase.getByUUID(uuid);
                    if (auth != null) {
                        Launcher.this.profileManager.setSelectedUser(uuid);
                        loggedIn = true;
                    }
                } catch (RuntimeException ex) {
                    LOGGER.debug("A RuntimeException is caught!");
                }
                if (!loggedIn && authDatabase.getByName(Launcher.this.requestedUser) != null) {
                    final UserAuthentication auth2 = authDatabase.getByName(Launcher.this.requestedUser);
                    if (auth2.getSelectedProfile() != null) {
                        Launcher.this.profileManager.setSelectedUser(UUIDTypeAdapter.fromUUID(auth2.getSelectedProfile().getId()));
                    } else {
                        Launcher.this.profileManager.setSelectedUser("demo-" + auth2.getUserID());
                    }
                }
            }
            Launcher.this.ensureLoggedIn();
        });
    }

    private MinecraftUserInterface selectUserInterface(final JFrame frame) {
        return new SwingUserInterface(this, frame);
    }

    public com.mojang.launcher.Launcher getLauncher() {
        return this.launcher;
    }

    public MinecraftUserInterface getUserInterface() {
        return this.userInterface;
    }

    public Integer getBootstrapVersion() {
        return this.bootstrapVersion;
    }

    public void ensureLoggedIn() {
        final UserAuthentication auth = this.profileManager.getAuthDatabase().getByUUID(this.profileManager.getSelectedUser());
        if (auth == null) {
            this.getUserInterface().showLoginPrompt();
        } else if (!auth.isLoggedIn()) {
            if (auth.canLogIn()) {
                try {
                    auth.logIn();
                    try {
                        this.profileManager.saveProfiles();
                    } catch (IOException e) {
                        Launcher.LOGGER.error("Couldn't save profiles after refreshing auth!", e);
                    }
                    this.profileManager.fireRefreshEvent();
                } catch (AuthenticationException e2) {
                    Launcher.LOGGER.error("Exception whilst logging into profile", e2);
                    this.getUserInterface().showLoginPrompt();
                }
            } else {
                this.getUserInterface().showLoginPrompt();
            }
        } else if (!auth.canPlayOnline()) {
            try {
                Launcher.LOGGER.info("Refreshing auth...");
                auth.logIn();
                try {
                    this.profileManager.saveProfiles();
                } catch (IOException e) {
                    Launcher.LOGGER.error("Couldn't save profiles after refreshing auth!", e);
                }
                this.profileManager.fireRefreshEvent();
            } catch (InvalidCredentialsException e3) {
                Launcher.LOGGER.error("Exception whilst logging into profile", e3);
                this.getUserInterface().showLoginPrompt();
            } catch (AuthenticationException e2) {
                Launcher.LOGGER.error("Exception whilst logging into profile", e2);
            }
        }
    }

    public UUID getClientToken() {
        return this.clientToken;
    }

    public void setClientToken(final UUID clientToken) {
        this.clientToken = clientToken;
    }

    public void cleanupOrphanedAssets() throws IOException {
        final File assetsDir = new File(this.getLauncher().getWorkingDirectory(), "assets");
        final File indexDir = new File(assetsDir, "indexes");
        final File objectsDir = new File(assetsDir, "objects");
        final Set<String> referencedObjects = Sets.newHashSet();
        if (!objectsDir.isDirectory()) {
            return;
        }
        for (final VersionSyncInfo syncInfo : this.getLauncher().getVersionManager().getInstalledVersions()) {
            if (syncInfo.getLocalVersion() instanceof CompleteMinecraftVersion) {
                final CompleteMinecraftVersion version = (CompleteMinecraftVersion) syncInfo.getLocalVersion();
                final String assetVersion = version.getAssetIndex().getId();
                final File indexFile = new File(indexDir, assetVersion + ".json");
                final AssetIndex index = this.gson.fromJson(FileUtils.readFileToString(indexFile, UTF_8), AssetIndex.class);
                for (final AssetIndex.AssetObject object : index.getUniqueObjects().keySet()) {
                    referencedObjects.add(object.getHash().toLowerCase());
                }
            }
        }
        final File[] directories = objectsDir.listFiles((FileFilter) DirectoryFileFilter.DIRECTORY);
        if (directories != null) {
            for (final File directory : directories) {
                final File[] files = directory.listFiles((FileFilter) FileFileFilter.FILE);
                if (files != null) {
                    for (final File file : files) {
                        if (!referencedObjects.contains(file.getName().toLowerCase())) {
                            Launcher.LOGGER.info("Cleaning up orphaned object {}", file.getName());
                            FileUtils.deleteQuietly(file);
                        }
                    }
                }
            }
        }
        deleteEmptyDirectories(objectsDir);
    }

    public void cleanupOrphanedLibraries() {
        final File librariesDir = new File(this.getLauncher().getWorkingDirectory(), "libraries");
        final Set<File> referencedLibraries = Sets.newHashSet();
        if (!librariesDir.isDirectory()) {
            return;
        }
        for (final VersionSyncInfo syncInfo : this.getLauncher().getVersionManager().getInstalledVersions()) {
            if (syncInfo.getLocalVersion() instanceof CompleteMinecraftVersion) {
                final CompleteMinecraftVersion version = (CompleteMinecraftVersion) syncInfo.getLocalVersion();
                for (final Library library : version.getRelevantLibraries()) {
                    String file = null;
                    if (library.getNatives() != null) {
                        final String natives = library.getNatives().get(OperatingSystem.getCurrentPlatform());
                        if (natives != null) {
                            file = library.getArtifactPath(natives);
                        }
                    } else {
                        file = library.getArtifactPath();
                    }
                    if (file != null) {
                        referencedLibraries.add(new File(librariesDir, file));
                        referencedLibraries.add(new File(librariesDir, file + ".sha"));
                    }
                }
            }
        }
        final Collection<File> libraries = FileUtils.listFiles(librariesDir, TrueFileFilter.TRUE, TrueFileFilter.TRUE);
        if (libraries != null) {
            for (final File file2 : libraries) {
                if (!referencedLibraries.contains(file2)) {
                    Launcher.LOGGER.info("Cleaning up orphaned library {}", file2);
                    FileUtils.deleteQuietly(file2);
                }
            }
        }
        deleteEmptyDirectories(librariesDir);
    }

    public void cleanupOldSkins() {
        final File assetsDir = new File(this.getLauncher().getWorkingDirectory(), "assets");
        final File skinsDir = new File(assetsDir, "skins");
        if (!skinsDir.isDirectory()) {
            return;
        }
        final Collection<File> files = FileUtils.listFiles(skinsDir, new AgeFileFilter(System.currentTimeMillis() - 604800000L), TrueFileFilter.TRUE);
        if (files != null) {
            for (final File file : files) {
                Launcher.LOGGER.info("Cleaning up old skin {}", file.getName());
                FileUtils.deleteQuietly(file);
            }
        }
        deleteEmptyDirectories(skinsDir);
    }

    public void cleanupOldVirtuals() throws IOException {
        final File assetsDir = new File(this.getLauncher().getWorkingDirectory(), "assets");
        final File virtualsDir = new File(assetsDir, "virtual");
        final DateTypeAdapter dateAdapter = new DateTypeAdapter();
        final Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -5);
        final Date cutoff = calendar.getTime();
        if (!virtualsDir.isDirectory()) {
            return;
        }
        final File[] directories = virtualsDir.listFiles((FileFilter) DirectoryFileFilter.DIRECTORY);
        if (directories != null) {
            for (final File directory : directories) {
                final File lastUsedFile = new File(directory, ".lastused");
                if (lastUsedFile.isFile()) {
                    final Date lastUsed = dateAdapter.deserializeToDate(FileUtils.readFileToString(lastUsedFile, UTF_8));
                    if (cutoff.after(lastUsed)) {
                        Launcher.LOGGER.info("Cleaning up old virtual directory {}", directory);
                        FileUtils.deleteQuietly(directory);
                    }
                } else {
                    Launcher.LOGGER.info("Cleaning up strange virtual directory {}", directory);
                    FileUtils.deleteQuietly(directory);
                }
            }
        }
        deleteEmptyDirectories(virtualsDir);
    }

    public void cleanupOldNatives() {
        final File root = new File(this.launcher.getWorkingDirectory(), "versions/");
        Launcher.LOGGER.info("Looking for old natives & assets to clean up...");
        final IOFileFilter ageFilter = new AgeFileFilter(System.currentTimeMillis() - 3600000L);
        if (!root.isDirectory()) {
            return;
        }
        final File[] versions = root.listFiles((FileFilter) DirectoryFileFilter.DIRECTORY);
        if (versions != null) {
            for (final File version : versions) {
                final File[] files = version.listFiles((FileFilter) FileFilterUtils.and(new PrefixFileFilter(version.getName() + "-natives-"), ageFilter));
                if (files != null) {
                    for (final File folder : files) {
                        Launcher.LOGGER.debug("Deleting " + folder);
                        FileUtils.deleteQuietly(folder);
                    }
                }
            }
        }
    }

    public void cleanupOrphanedVersions() {
        Launcher.LOGGER.info("Looking for orphaned versions to clean up...");
        final Set<String> referencedVersions = Sets.newHashSet();
        for (final Profile profile : this.getProfileManager().getProfiles().values()) {
            final String lastVersionId = profile.getLastVersionId();
            VersionSyncInfo syncInfo = null;
            if (lastVersionId != null) {
                syncInfo = this.getLauncher().getVersionManager().getVersionSyncInfo(lastVersionId);
            }
            if (syncInfo == null || syncInfo.getLatestVersion() == null) {
                syncInfo = this.getLauncher().getVersionManager().getVersions(profile.getVersionFilter()).get(0);
            }
            if (syncInfo != null) {
                final Version version = syncInfo.getLatestVersion();
                referencedVersions.add(version.getId());
                if (!(version instanceof CompleteMinecraftVersion)) {
                    continue;
                }
                final CompleteMinecraftVersion completeMinecraftVersion = (CompleteMinecraftVersion) version;
                referencedVersions.add(completeMinecraftVersion.getInheritsFrom());
                referencedVersions.add(completeMinecraftVersion.getJar());
            }
        }
        final Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -7);
        final Date cutoff = calendar.getTime();
        for (final VersionSyncInfo versionSyncInfo : this.getLauncher().getVersionManager().getInstalledVersions()) {
            if (versionSyncInfo.getLocalVersion() instanceof CompleteMinecraftVersion) {
                final CompleteVersion version2 = (CompleteVersion) versionSyncInfo.getLocalVersion();
                if (referencedVersions.contains(version2.getId()) || version2.getType() != MinecraftReleaseType.SNAPSHOT) {
                    continue;
                }
                if (versionSyncInfo.isOnRemote()) {
                    Launcher.LOGGER.info("Deleting orphaned version {} because it's a snapshot available on remote", version2.getId());
                    try {
                        this.getLauncher().getVersionManager().uninstallVersion(version2);
                    } catch (Exception e) {
                        Launcher.LOGGER.warn("Couldn't uninstall version " + version2.getId(), e);
                    }
                } else {
                    if (!version2.getUpdatedTime().before(cutoff)) {
                        continue;
                    }
                    Launcher.LOGGER.info("Deleting orphaned version {} because it's an unsupported old snapshot", version2.getId());
                    try {
                        this.getLauncher().getVersionManager().uninstallVersion(version2);
                    } catch (Exception e) {
                        Launcher.LOGGER.warn("Couldn't uninstall version " + version2.getId(), e);
                    }
                }
            }
        }
    }

    public void performCleanups() throws IOException {
        this.cleanupOrphanedVersions();
        this.cleanupOrphanedAssets();
        this.cleanupOldSkins();
        this.cleanupOldNatives();
        this.cleanupOldVirtuals();
    }

    public ProfileManager getProfileManager() {
        return this.profileManager;
    }

    public GameLaunchDispatcher getLaunchDispatcher() {
        return this.launchDispatcher;
    }

    public boolean usesWinTenHack() {
        return this.winTenHack;
    }

    public void setWinTenHack() {
        this.winTenHack = true;
    }
}
