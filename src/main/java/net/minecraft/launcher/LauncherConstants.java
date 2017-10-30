package net.minecraft.launcher;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.launcher.updater.LowerCaseEnumTypeAdapterFactory;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import static java.nio.charset.StandardCharsets.UTF_8;

public class LauncherConstants {
    // int A-Z
    public static final int BUILD_NUMBER = 2017103000;
    public static final int FORMAT_PROFILES = 1;
    public static final int FORMAT_VERSION = 18;
    public static final int VERSION_MINIMUM_SUPPORTED_BOOTSTRAP = 4;
    public static final int VERSION_SUPER_COOL_BOOTSTRAP = 100;
    // String Image A-Z
    public static final String IMAGE_FAVICON = "/images/Favicon.png";
    public static final String IMAGE_LAUNCHER_BACKGROUND = "/images/Background.png";
    public static final String IMAGE_MACOS_FAVICON = "/images/Favicon.icns";
    public static final String IMAGE_MINECRAFT_LOGO = "/images/Minecraft_Logo.png";
    // String JVM Arguments A-Z
    public static final String JVM_ARGUMENTS_DEFAULT = "-XX:InitialHeapSize=512M -XX:MaxHeapSize=2048M -XX:+UseG1GC";
    // String A-Z
    public static final String MINECRAFT_FREEDOM_LAUNCHER_WINDOW_TITLE = "Minecraft Freedom Launcher 2";
    public static final String MINECRAFT_SHORTCUT_LINK = "Microsoft\\Windows\\Start Menu\\Programs\\Minecraft\\Minecraft.lnk";
    public static final String SECURITY_CERTIFICATE = "/yggdrasil_session_pubkey.der";
    // String URL A-Z
    public static final String URL_ANALYTICS_SUBMISSION = "https://minecraftprod.rtep.msgamestudios.com/tenants/minecraftprod/routes/java/";
    public static final String URL_BOOTSTRAP_DOWNLOAD = "https://s3.amazonaws.com/Minecraft.Download/launcher/Minecraft";
    public static final String URL_BUGS = "https://bugs.mojang.com/";
    public static final String URL_DEMO_HELP = "https://help.mojang.com/customer/portal/articles/1218766-can-only-play-minecraft-demo?ref=launcher/";
    public static final String URL_FORGOT_MIGRATED_EMAIL = "https://help.mojang.com/customer/portal/articles/1205055-minecraft-launcher-error---migrated-account?ref=launcher/";
    public static final String URL_FORGOT_PASSWORD_MINECRAFT = "https://help.mojang.com/customer/portal/articles/329524-change-or-forgot-password?ref=launcher/";
    public static final String URL_FORGOT_USERNAME = "https://help.mojang.com/customer/portal/articles/1233873?ref=launcher/";
    public static final String URL_HELP = "https://help.mojang.com/?ref=launcher";
    public static final String URL_HOPPER = "https://hopper.minecraft.net/crashes/";
    public static final String URL_JAR_FALLBACK = "https://s3.amazonaws.com/Minecraft.Download/";
    public static final String URL_LAUNCHER_UPGRADE = "https://launcher.mojang.com/download/";
    public static final String URL_LEGACY_AUTHENTICATION = "https://login.minecraft.net";
    public static final String URL_LEGACY_SESSION = "https://session.minecraft.net/game/";
    public static final String URL_LIBRARY_BASE = "https://libraries.minecraft.net/";
    public static final String URL_MINECRAFT = "https://minecraft.net/en-us/";
    public static final String URL_MOJANG_AUTH_SERVER = "https://authserver.mojang.com/";
    public static final String URL_MOJANG_AUTH_SERVER_WITHOUT_HTTPS_PREFIX = "authserver.mojang.com";
    public static final String URL_MOJANG_SESSIONSERVER = "https://sessionserver.mojang.com/session/minecraft/";
    public static final String URL_PROFILES_API = "https://api.mojang.com/profiles/";
    public static final String URL_REGISTER = "https://account.mojang.com/register";
    public static final String URL_RELEASES = "https://energy0124.github.io/MCFreedomLauncher/";
    public static final String URL_RESOURCE_BASE = "https://resources.download.minecraft.net/";
    public static final String URL_SESSION_PROFILE = "https://sessionserver.mojang.com/session/minecraft/profile/";
    public static final String URL_STATUS_CHECKER = "https://status.mojang.com/check?service=";
    public static final String URL_USERS_PROFILES_API = "https://api.mojang.com/users/profiles/";
    public static final String URL_VERSION_MANIFEST = "https://launchermeta.mojang.com/mc/game/version_manifest.json";
    // String Message A-Z
    public static final String MESSAGE_CANNOT_CONNECT_ONE = "Sorry, but we couldn't connect to our servers.";
    public static final String MESSAGE_CANNOT_CONNECT_TWO = "Please make sure that you are online and that Minecraft is not blocked.";
    public static final String MESSAGE_CANNOT_LOG_BACK_IN = "We couldn't log you back in as ";
    public static final String MESSAGE_CANNOT_LOG_IN = "Sorry, but we couldn't log you in right now.";
    public static final String MESSAGE_CANNOT_LOG_IN_WITH_USERNAME = "Sorry, but we can't log you in with your username.";
    public static final String MESSAGE_CONFIRM_PROFILE_DELETION = "Are you sure you want to delete this profile?";
    public static final String MESSAGE_DEPRECATED_JVM_ARGUMENTS_USED = "At least one JVM argument used is deprecated.\nPlease remove the deprecated argument(s) and try again.";
    public static final String MESSAGE_DESCRIBE_CRASH = "Put the summary of the bug you're having here\n\n*What I expected to happen was...:*\nDescribe what you thought should happen here\n\n*What actually happened was...:*\nDescribe what happened here\n\n*Steps to Reproduce:*\n1. Put a step by step guide on how to trigger the bug here:";
    public static final String MESSAGE_GAME_CRASHED = "<html><div style='width: 100%'><p>Sorry, the game has crashed!</p><p>You can see the full report below.</p></div></html>";
    public static final String MESSAGE_INCOMPATIBLE_REASON = "This version is incompatible with your computer. Please try another one by going into Edit Profile and selecting one through the dropdown. Sorry!";
    public static final String MESSAGE_SUPPORTED_JAVA_9_UNAVAILABLE = "Please install Java 9 distributed by Oracle or OpenJDK.";
    public static final String MESSAGE_JAVAFX_UNAVAILABLE = "JavaFX can't be loaded. Please check if JavaFX is present.";
    public static final String MESSAGE_LAUNCHER_CORRUPTED = "Sorry, an unknown error occurred while starting the launcher.\nYou can obtain a new copy from the GitHub repository.";
    public static final String MESSAGE_LAUNCHER_NEWER_VERSION_USED = "It looks like you've used a newer launcher than this one! If you go back to using this one, we will need to reset your configuration. Do you still want to use this version of the launcher and have the settings reset?";
    public static final String MESSAGE_LAUNCHER_NOT_NATIVE = "This shortcut to the launcher is out of date. Please delete it and remake it to the new launcher, which we will start for you now.";
    public static final String MESSAGE_LAUNCHER_OUTDATED = "Sorry, but your launcher is outdated! Please download the newer launcher!";
    public static final String MESSAGE_MORE_THAN_ONE_INSTANCE = "You already have an instance of Minecraft running. If you launch another one in the same folder, they may clash and corrupt your saves.\nDo you want to start another instance of Minecraft despite this?\nYou may solve this issue by launching the game in a different folder (see the \"Edit Profile\" button)";
    public static final String MESSAGE_NEW_LAUNCHER_UPDATE_ONE = "<html><p>You are running an old version of the launcher. Please <a href='";
    public static final String MESSAGE_NEW_LAUNCHER_UPDATE_TWO = "'>use the new launcher</a> which will improve the performance of both launcher and game.</p></html>";
    public static final String MESSAGE_NEW_NO_JAVA_LAUNCHER_UPDATE_ONE = "<html><p>You are running on an old version of Java. Please <a href='";
    public static final String MESSAGE_NEW_NO_JAVA_LAUNCHER_UPDATE_TWO = "'>use the new launcher</a> which doesn't require Java, as it will make your game faster.</p></html>";
    public static final String MESSAGE_PUBLISH_CRASH_REPORT = "<html><p>Sorry, but it looks like the game crashed and we don't know why.</p><p>Do you want to publish this report so that Mojang can fix it?</p></html>";
    public static final String MESSAGE_RESOLUTION_UNSUPPORTED = "Sorry, your screen resolution is too small.";
    public static final String MESSAGE_SERVER_DOWN = "It looks like our servers are down right now. Sorry!";
    public static final String MESSAGE_SERVER_DOWN_FIXING = "We're already working on the problem and will have it fixed soon.";
    public static final String MESSAGE_TRY_AGAIN = "Please try again!";
    public static final String MESSAGE_TRY_AGAIN_LATER = "Please try again later!";
    public static final String MESSAGE_UNKNOWN_ERROR = "Sorry, this launcher might be corrupted.\nPlease obtain a new copy from this project's GitHub repository.";
    public static final String MESSAGE_USE_EMAIL = "You have migrated your account, please use your email address.";
    public static final String MESSAGE_USERNAME_OR_PASSWORD_INCORRECT = "Sorry, but your username or password is incorrect!";
    public static final String MESSAGE_VERSIONS_DEVELOPMENT = "Are you sure you want to enable development builds?\nThey are not guaranteed to be stable and may corrupt your world.\nYou are advised to run this in a separate directory or run regular backups.";
    public static final String MESSAGE_VERSIONS_OLD = "These versions are very out of date and may be unstable. Any bugs, crashes, missing features or\nother nasties you may find will never be fixed in these versions.\nIt is strongly recommended you play these in separate directories to avoid corruption.";
    // String A-Z
    private static final String LAUNCHER_VERSION = "2.0.0.01";

    public static URI constantURI(final String input) {
        try {
            return new URI(input);
        } catch (URISyntaxException e) {
            throw new Error(e);
        }
    }

    public static URL constantURL(final String input) {
        try {
            return new URL(input);
        } catch (MalformedURLException e) {
            throw new Error(e);
        }
    }

    public static String getVersionName() {
        return LAUNCHER_VERSION;
    }

    protected static LauncherProperties getProperties() {
        final Gson gson = new GsonBuilder().registerTypeAdapterFactory(new LowerCaseEnumTypeAdapterFactory()).create();
        final InputStream stream = LauncherConstants.class.getResourceAsStream("/launcher_properties.json");
        if (stream != null) {
            try {
                return gson.fromJson(IOUtils.toString(stream, UTF_8), LauncherProperties.class);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                IOUtils.closeQuietly(stream);
            }
        }
        return new LauncherProperties();
    }

    public enum LauncherEnvironment {
        PRODUCTION(""),
        STAGING(" STAGING VERSION, NOT FINAL"),
        DEV(" DEV VERSION, NOT FINAL");

        private final String title;

        LauncherEnvironment(final String title) {
            this.title = title;
        }

        public String getTitle() {
            return this.title;
        }
    }

    public static class LauncherProperties {
        private final LauncherEnvironment environment;
        private final URL versionManifest;

        public LauncherProperties() {
            this.environment = LauncherEnvironment.PRODUCTION;
            this.versionManifest = constantURL(URL_VERSION_MANIFEST);
        }

        public LauncherEnvironment getEnvironment() {
            return this.environment;
        }

        public URL getVersionManifest() {
            return this.versionManifest;
        }
    }
}
