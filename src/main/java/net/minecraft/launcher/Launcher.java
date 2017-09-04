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
import org.apache.commons.io.Charsets;
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

public class Launcher {
    private static final Logger LOGGER = LogManager.getLogger();

    static {
        Thread.currentThread().setContextClassLoader(Launcher.class.getClassLoader());
    }

    private final Gson gson = new Gson();
    private final Integer bootstrapVersion;
    private final MinecraftUserInterface userInterface;
    private final ProfileManager profileManager;
    private final GameLaunchDispatcher launchDispatcher;
    public com.mojang.launcher.Launcher launcher;
    private UUID clientToken = UUID.randomUUID();
    private String requestedUser;

    public Launcher(JFrame frame, File workingDirectory, Proxy proxy, PasswordAuthentication proxyAuth, String[] args) {
        this(frame, workingDirectory, proxy, proxyAuth, args, Integer.valueOf(0));
    }

    public Launcher(JFrame frame, File workingDirectory, Proxy proxy, PasswordAuthentication proxyAuth, String[] args, Integer bootstrapVersion) {
        setupErrorHandling();
        this.bootstrapVersion = bootstrapVersion;
        this.userInterface = selectUserInterface(frame);
        if (bootstrapVersion.intValue() < 4) {
            this.userInterface.showOutdatedNotice();
            throw new Error("Outdated bootstrap");
        }
        LOGGER.info(this.userInterface.getTitle() + " (through bootstrap " + bootstrapVersion + ") started on " + OperatingSystem.getCurrentPlatform().getName() + "...");
        LOGGER.info("Current time is " + DateFormat.getDateTimeInstance(2, 2, Locale.US).format(new Date()));
        if (!OperatingSystem.getCurrentPlatform().isSupported()) {
            LOGGER.fatal("This operating system is unknown or unsupported, we cannot guarantee that the game will launch successfully.");
        }
        LOGGER.info("System.getProperty('os.name') == '" + System.getProperty("os.name") + "'");
        LOGGER.info("System.getProperty('os.version') == '" + System.getProperty("os.version") + "'");
        LOGGER.info("System.getProperty('os.arch') == '" + System.getProperty("os.arch") + "'");
        LOGGER.info("System.getProperty('java.version') == '" + System.getProperty("java.version") + "'");
        LOGGER.info("System.getProperty('java.vendor') == '" + System.getProperty("java.vendor") + "'");
        LOGGER.info("System.getProperty('sun.arch.data.model') == '" + System.getProperty("sun.arch.data.model") + "'");

        this.launchDispatcher = new GameLaunchDispatcher(this, processArgs(args));
        this.launcher = new com.mojang.launcher.Launcher(this.userInterface, workingDirectory, proxy, proxyAuth, new MinecraftVersionManager(new LocalVersionList(workingDirectory), new RemoteVersionList("https://s3.amazonaws.com/Minecraft.Download/", proxy)), Agent.MINECRAFT, MinecraftReleaseTypeFactory.instance(), 17);
        this.profileManager = new ProfileManager(this);
        ((SwingUserInterface) this.userInterface).initializeFrame();

        refreshVersionsAndProfiles();
    }

    public static void setLookAndFeel() {
        final JFrame frame = new JFrame();
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Throwable ignored) {
            try {
                Launcher.LOGGER.error("Your java failed to provide normal look and feel, trying the old fallback now");
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Throwable t) {
                Launcher.LOGGER.error("Unexpected exception setting look and feel", t);
            }
        }
        final JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder("Test"));
        frame.add(panel);
        try {
            frame.pack();
        } catch (Throwable ignored2) {
            Launcher.LOGGER.error("Custom (broken) theme detected, falling back onto x-platform theme");
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Throwable ex) {
                Launcher.LOGGER.error("Unexpected exception setting look and feel", ex);
            }
        }
        frame.dispose();
    }

    private static Collection<File> listEmptyDirectories(File directory) {
        List<File> result = Lists.newArrayList();
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    File[] subFiles = file.listFiles();
                    if ((subFiles == null) || (subFiles.length == 0)) {
                        result.add(file);
                    } else {
                        result.addAll(listEmptyDirectories(file));
                    }
                }
            }
        }
        return result;
    }

    private static void deleteEmptyDirectories(File directory) {
        for (; ; ) {
            Collection<File> files = listEmptyDirectories(directory);
            if (files.isEmpty()) {
                return;
            }
            for (File file : files) {
                if (FileUtils.deleteQuietly(file)) {
                    LOGGER.info("Deleted empty directory {}", file);
                } else {
                    return;
                }
            }
        }
    }

    private void setupErrorHandling() {
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            public void uncaughtException(Thread t, Throwable e) {
                Launcher.LOGGER.fatal("Unhandled exception in thread " + t, e);
            }
        });
    }

    private String[] processArgs(String[] args) {
        OptionParser optionParser = new OptionParser();
        optionParser.allowsUnrecognizedOptions();

        OptionSpec<String> userOption = optionParser.accepts("user").withRequiredArg().ofType(String.class);
        OptionSpec<String> nonOptions = optionParser.nonOptions();
        OptionSet optionSet;
        try {
            optionSet = optionParser.parse(args);
        } catch (OptionException e) {
            return args;
        }
        if (optionSet.has(userOption)) {
            this.requestedUser = ((String) optionSet.valueOf(userOption));
        }
        List<String> remainingOptions = optionSet.valuesOf(nonOptions);
        return (String[]) remainingOptions.toArray(new String[remainingOptions.size()]);
    }

    public void refreshVersionsAndProfiles() {
        getLauncher().getVersionManager().getExecutorService().submit(new Runnable() {
            public void run() {
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
                    AuthenticationDatabase authDatabase = Launcher.this.profileManager.getAuthDatabase();
                    boolean loggedIn = false;
                    try {
                        String uuid = UUIDTypeAdapter.fromUUID(UUIDTypeAdapter.fromString(Launcher.this.requestedUser));
                        UserAuthentication auth = authDatabase.getByUUID(uuid);
                        if (auth != null) {
                            Launcher.this.profileManager.setSelectedUser(uuid);
                            loggedIn = true;
                        }
                    } catch (RuntimeException localRuntimeException) {
                    }
                    if ((!loggedIn) && (authDatabase.getByName(Launcher.this.requestedUser) != null)) {
                        UserAuthentication auth = authDatabase.getByName(Launcher.this.requestedUser);
                        if (auth.getSelectedProfile() != null) {
                            Launcher.this.profileManager.setSelectedUser(UUIDTypeAdapter.fromUUID(auth.getSelectedProfile().getId()));
                        } else {
                            Launcher.this.profileManager.setSelectedUser("demo-" + auth.getUserID());
                        }
                    }
                }
                Launcher.this.ensureLoggedIn();
            }
        });
    }

    private MinecraftUserInterface selectUserInterface(JFrame frame) {
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
        UserAuthentication auth = this.profileManager.getAuthDatabase().getByUUID(this.profileManager.getSelectedUser());
        if (auth == null) {
            getUserInterface().showLoginPrompt();
        } else if (!auth.isLoggedIn()) {
            if (auth.canLogIn()) {
                try {
                    auth.logIn();
                    try {
                        this.profileManager.saveProfiles();
                    } catch (IOException e) {
                        LOGGER.error("Couldn't save profiles after refreshing auth!", e);
                    }
                    this.profileManager.fireRefreshEvent();
                } catch (AuthenticationException e) {
                    LOGGER.error("Exception whilst logging into profile", e);
                    getUserInterface().showLoginPrompt();
                }
            } else {
                getUserInterface().showLoginPrompt();
            }
        } else if (!auth.canPlayOnline()) {
            try {
                LOGGER.info("Refreshing auth...");
                auth.logIn();
                try {
                    this.profileManager.saveProfiles();
                } catch (IOException e) {
                    LOGGER.error("Couldn't save profiles after refreshing auth!", e);
                }
                this.profileManager.fireRefreshEvent();
            } catch (InvalidCredentialsException e) {
                LOGGER.error("Exception whilst logging into profile", e);
                getUserInterface().showLoginPrompt();
            } catch (AuthenticationException e) {
                LOGGER.error("Exception whilst logging into profile", e);
            }
        }
    }

    public UUID getClientToken() {
        return this.clientToken;
    }

    public void setClientToken(UUID clientToken) {
        this.clientToken = clientToken;
    }

    public void cleanupOrphanedAssets()
            throws IOException {
        File assetsDir = new File(getLauncher().getWorkingDirectory(), "assets");
        File indexDir = new File(assetsDir, "indexes");
        File objectsDir = new File(assetsDir, "objects");
        Set<String> referencedObjects = Sets.newHashSet();
        if (!objectsDir.isDirectory()) {
            return;
        }
        for (VersionSyncInfo syncInfo : getLauncher().getVersionManager().getInstalledVersions()) {
            if ((syncInfo.getLocalVersion() instanceof CompleteMinecraftVersion)) {
                CompleteMinecraftVersion version = (CompleteMinecraftVersion) syncInfo.getLocalVersion();
                String assetVersion = version.getAssets() == null ? "legacy" : version.getAssets();
                File indexFile = new File(indexDir, assetVersion + ".json");
                AssetIndex index = (AssetIndex) this.gson.fromJson(FileUtils.readFileToString(indexFile, Charsets.UTF_8), AssetIndex.class);
                for (AssetIndex.AssetObject object : index.getUniqueObjects().keySet()) {
                    referencedObjects.add(object.getHash().toLowerCase());
                }
            }
        }
        File[] directories = objectsDir.listFiles((FileFilter) DirectoryFileFilter.DIRECTORY);
        if (directories != null) {
            for (File directory : directories) {
                File[] files = directory.listFiles((FileFilter) FileFileFilter.FILE);
                if (files != null) {
                    for (File file : files) {
                        if (!referencedObjects.contains(file.getName().toLowerCase())) {
                            LOGGER.info("Cleaning up orphaned object {}", file.getName());
                            FileUtils.deleteQuietly(file);
                        }
                    }
                }
            }
        }
        deleteEmptyDirectories(objectsDir);
    }

    public void cleanupOrphanedLibraries()
            throws IOException {
        File librariesDir = new File(getLauncher().getWorkingDirectory(), "libraries");
        Set<File> referencedLibraries = Sets.newHashSet();
        if (!librariesDir.isDirectory()) {
            return;
        }
        for (VersionSyncInfo syncInfo : getLauncher().getVersionManager().getInstalledVersions()) {
            if ((syncInfo.getLocalVersion() instanceof CompleteMinecraftVersion)) {
                CompleteMinecraftVersion version = (CompleteMinecraftVersion) syncInfo.getLocalVersion();
                for (Library library : version.getRelevantLibraries()) {
                    String file = null;
                    if (library.getNatives() != null) {
                        String natives = (String) library.getNatives().get(OperatingSystem.getCurrentPlatform());
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
        Collection<File> libraries = FileUtils.listFiles(librariesDir, TrueFileFilter.TRUE, TrueFileFilter.TRUE);
        if (libraries != null) {
            for (File file : libraries) {
                if (!referencedLibraries.contains(file)) {
                    LOGGER.info("Cleaning up orphaned library {}", file);
                    FileUtils.deleteQuietly(file);
                }
            }
        }
        deleteEmptyDirectories(librariesDir);
    }

    public void cleanupOldSkins() {
        File assetsDir = new File(getLauncher().getWorkingDirectory(), "assets");
        File skinsDir = new File(assetsDir, "skins");
        if (!skinsDir.isDirectory()) {
            return;
        }
        Collection<File> files = FileUtils.listFiles(skinsDir, new AgeFileFilter(System.currentTimeMillis() - 604800000L), TrueFileFilter.TRUE);
        if (files != null) {
            for (File file : files) {
                LOGGER.info("Cleaning up old skin {}", file.getName());
                FileUtils.deleteQuietly(file);
            }
        }
        deleteEmptyDirectories(skinsDir);
    }

    public void cleanupOldVirtuals()
            throws IOException {
        File assetsDir = new File(getLauncher().getWorkingDirectory(), "assets");
        File virtualsDir = new File(assetsDir, "virtual");
        DateTypeAdapter dateAdapter = new DateTypeAdapter();
        Calendar calendar = Calendar.getInstance();
        calendar.add(5, -5);
        Date cutoff = calendar.getTime();
        if (!virtualsDir.isDirectory()) {
            return;
        }
        File[] directories = virtualsDir.listFiles((FileFilter) DirectoryFileFilter.DIRECTORY);
        if (directories != null) {
            for (File directory : directories) {
                File lastUsedFile = new File(directory, ".lastused");
                if (lastUsedFile.isFile()) {
                    Date lastUsed = dateAdapter.deserializeToDate(FileUtils.readFileToString(lastUsedFile));
                    if (cutoff.after(lastUsed)) {
                        LOGGER.info("Cleaning up old virtual directory {}", directory);
                        FileUtils.deleteQuietly(directory);
                    }
                } else {
                    LOGGER.info("Cleaning up strange virtual directory {}", directory);
                    FileUtils.deleteQuietly(directory);
                }
            }
        }
        deleteEmptyDirectories(virtualsDir);
    }

    public void cleanupOldNatives() {
        File root = new File(this.launcher.getWorkingDirectory(), "versions/");
        LOGGER.info("Looking for old natives & assets to clean up...");
        IOFileFilter ageFilter = new AgeFileFilter(System.currentTimeMillis() - 3600000L);
        if (!root.isDirectory()) {
            return;
        }
        File[] versions = root.listFiles((FileFilter) DirectoryFileFilter.DIRECTORY);
        if (versions != null) {
            for (File version : versions) {
                File[] files = version.listFiles((FileFilter) FileFilterUtils.and(new IOFileFilter[]{new PrefixFileFilter(version.getName() + "-natives-"), ageFilter}));
                if (files != null) {
                    for (File folder : files) {
                        LOGGER.debug("Deleting " + folder);

                        FileUtils.deleteQuietly(folder);
                    }
                }
            }
        }
    }

    public void cleanupOrphanedVersions() {
        LOGGER.info("Looking for orphaned versions to clean up...");
        Set<String> referencedVersions = Sets.newHashSet();
        for (Profile profile : getProfileManager().getProfiles().values()) {
            String lastVersionId = profile.getLastVersionId();
            VersionSyncInfo syncInfo = null;
            if (lastVersionId != null) {
                syncInfo = getLauncher().getVersionManager().getVersionSyncInfo(lastVersionId);
            }
            if ((syncInfo == null) || (syncInfo.getLatestVersion() == null)) {
                syncInfo = (VersionSyncInfo) getLauncher().getVersionManager().getVersions(profile.getVersionFilter()).get(0);
            }
            if (syncInfo != null) {
                Version version = syncInfo.getLatestVersion();
                referencedVersions.add(version.getId());
                if ((version instanceof CompleteMinecraftVersion)) {
                    CompleteMinecraftVersion completeMinecraftVersion = (CompleteMinecraftVersion) version;
                    referencedVersions.add(completeMinecraftVersion.getInheritsFrom());
                    referencedVersions.add(completeMinecraftVersion.getJar());
                }
            }
        }
        Calendar calendar = Calendar.getInstance();
        calendar.add(5, -7);
        Date cutoff = calendar.getTime();
        for (VersionSyncInfo versionSyncInfo : getLauncher().getVersionManager().getInstalledVersions()) {
            if ((versionSyncInfo.getLocalVersion() instanceof CompleteMinecraftVersion)) {
                CompleteVersion version = (CompleteVersion) versionSyncInfo.getLocalVersion();
                if ((!referencedVersions.contains(version.getId())) && (version.getType() == MinecraftReleaseType.SNAPSHOT)) {
                    if (versionSyncInfo.isOnRemote()) {
                        LOGGER.info("Deleting orphaned version {} because it's a snapshot available on remote", version.getId());
                        try {
                            getLauncher().getVersionManager().uninstallVersion(version);
                        } catch (IOException e) {
                            LOGGER.warn("Couldn't uninstall version " + version.getId(), e);
                        }
                    } else if (version.getUpdatedTime().before(cutoff)) {
                        LOGGER.info("Deleting orphaned version {} because it's an unsupported old snapshot", version.getId());
                        try {
                            getLauncher().getVersionManager().uninstallVersion(version);
                        } catch (IOException e) {
                            LOGGER.warn("Couldn't uninstall version " + version.getId(), e);
                        }
                    }
                }
            }
        }
    }

    public void performCleanups()
            throws IOException {
        cleanupOrphanedVersions();
        cleanupOrphanedAssets();
        cleanupOldSkins();
        cleanupOldNatives();
        cleanupOldVirtuals();
    }

    public ProfileManager getProfileManager() {
        return this.profileManager;
    }

    public GameLaunchDispatcher getLaunchDispatcher() {
        return this.launchDispatcher;
    }

}
