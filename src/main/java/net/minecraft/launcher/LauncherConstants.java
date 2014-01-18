package net.minecraft.launcher;

import java.net.URISyntaxException;
import java.net.URI;

public class LauncherConstants
{
    public static final String VERSION_NAME = "1.3.7";
    public static final int VERSION_NUMERIC = 13;
    public static final URI URL_REGISTER;
    public static final String URL_DOWNLOAD_BASE = "https://s3.amazonaws.com/Minecraft.Download/";
    public static final String URL_RESOURCE_BASE = "http://resources.download.minecraft.net/";
    public static final String URL_LIBRARY_BASE = "https://libraries.minecraft.net/";
    public static final String URL_BLOG = "http://mcupdate.tumblr.com";
    public static final String URL_SUPPORT = "http://help.mojang.com/?ref=launcher";
    public static final String URL_STATUS_CHECKER = "http://status.mojang.com/check";
    public static final int UNVERSIONED_BOOTSTRAP_VERSION = 0;
    public static final int MINIMUM_BOOTSTRAP_SUPPORTED = 4;
    public static final String URL_BOOTSTRAP_DOWNLOAD = "https://mojang.com/2013/06/minecraft-1-6-pre-release/";
    public static final String[] BOOTSTRAP_OUT_OF_DATE_BUTTONS;
    public static final String[] CONFIRM_PROFILE_DELETION_OPTIONS;
    public static final URI URL_FORGOT_USERNAME;
    public static final URI URL_FORGOT_PASSWORD_MINECRAFT;
    public static final URI URL_FORGOT_MIGRATED_EMAIL;
    public static final int MAX_NATIVES_LIFE_IN_SECONDS = 3600;
    public static final String DEFAULT_VERSION_INCOMPATIBILITY_REASON = "This version is incompatible with your computer. Please try another one by going into Edit Profile and selecting one through the dropdown. Sorry!";
    
    public static URI constantURI(final String input) {
        try {
            return new URI(input);
        }
        catch (URISyntaxException e) {
            throw new Error(e);
        }
    }
    
    static {
        URL_REGISTER = constantURI("https://account.mojang.com/register");
        BOOTSTRAP_OUT_OF_DATE_BUTTONS = new String[] { "Go to URL", "Close" };
        CONFIRM_PROFILE_DELETION_OPTIONS = new String[] { "Delete profile", "Cancel" };
        URL_FORGOT_USERNAME = constantURI("http://help.mojang.com/customer/portal/articles/1233873?ref=launcher");
        URL_FORGOT_PASSWORD_MINECRAFT = constantURI("http://help.mojang.com/customer/portal/articles/329524-change-or-forgot-password?ref=launcher");
        URL_FORGOT_MIGRATED_EMAIL = constantURI("http://help.mojang.com/customer/portal/articles/1205055-minecraft-launcher-error---migrated-account?ref=launcher");
    }
}
