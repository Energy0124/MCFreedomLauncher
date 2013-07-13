package net.minecraft.launcher.profile;

import net.minecraft.launcher.authentication.AuthenticationService;
import net.minecraft.launcher.authentication.SPAuthenticationService;
import net.minecraft.launcher.authentication.yggdrasil.YggdrasilAuthenticationService;
import net.minecraft.launcher.updater.VersionFilter;
import net.minecraft.launcher.versions.ReleaseType;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class Profile {
    public static final String DEFAULT_JRE_ARGUMENTS_64BIT = "-Xmx1G";
    public static final String DEFAULT_JRE_ARGUMENTS_32BIT = "-Xmx512M";
    public static final Resolution DEFAULT_RESOLUTION = new Resolution(854, 480);
    public static final Set<ReleaseType> DEFAULT_RELEASE_TYPES = new HashSet(Arrays.asList(new ReleaseType[]{ReleaseType.RELEASE}));
    private static boolean SPMode = true;
    private AuthenticationService authentication = SPMode ? new SPAuthenticationService() : new YggdrasilAuthenticationService();
    private String name;
    private File gameDir;
    private String lastVersionId;
    private String javaDir;
    private String javaArgs;
    private Resolution resolution;
    private Set<ReleaseType> allowedReleaseTypes;
    private Boolean useHopperCrashService;

    private Locale locale;

    public Profile() {
    }



    public Profile(Profile copy) {
        this.name = copy.name;
        this.gameDir = copy.gameDir;
        this.authentication = copy.authentication;
        this.lastVersionId = copy.lastVersionId;
        this.javaDir = copy.javaDir;
        this.javaArgs = copy.javaArgs;
        //this.resolution = copy.resolution;
        this.resolution = (copy.resolution == null ? null : new Resolution(copy.resolution));
        this.allowedReleaseTypes = (copy.allowedReleaseTypes == null ? null : new HashSet(copy.allowedReleaseTypes));
        this.useHopperCrashService = copy.useHopperCrashService;

        this.locale=copy.locale;
        this.SPMode=copy.SPMode;
    }

    public Locale getLocale() {
        return locale;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }
    public Profile(String name) {
        this.name = name;
    }

    public String getJavaDir() {
        return javaDir;
    }

    public void setAuthentication(AuthenticationService authentication) {
        this.authentication = authentication;
    }

    public static boolean isSPMode() {
        return SPMode;
    }

    public static void setSPMode(boolean SPMode) {
        Profile.SPMode = SPMode;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public File getGameDir() {
        return this.gameDir;
    }

    public void setGameDir(File gameDir) {
        this.gameDir = gameDir;
    }

    public void setJavaDir(String javaDir) {
        this.javaDir = javaDir;
    }

    public String getLastVersionId() {
        return this.lastVersionId;
    }

    public void setLastVersionId(String lastVersionId) {
        this.lastVersionId = lastVersionId;
    }

    public String getJavaArgs() {
        return this.javaArgs;
    }

    public void setJavaArgs(String javaArgs) {
        this.javaArgs = javaArgs;
    }

    public String getJavaPath() {
        return this.javaDir;
    }

    public Resolution getResolution() {
        return this.resolution;
    }

    public void setResolution(Resolution resolution) {
        this.resolution = resolution;
    }

    public AuthenticationService getAuthentication() {
        return this.authentication;
    }

    public Set<ReleaseType> getAllowedReleaseTypes() {
        return this.allowedReleaseTypes;
    }

    public void setAllowedReleaseTypes(Set<ReleaseType> allowedReleaseTypes) {
        this.allowedReleaseTypes = allowedReleaseTypes;
    }

    public boolean getUseHopperCrashService() {
        return this.useHopperCrashService == null;
    }

    public void setUseHopperCrashService(boolean useHopperCrashService) {
        this.useHopperCrashService = (useHopperCrashService ? null : Boolean.valueOf(false));
    }

    public VersionFilter getVersionFilter() {
        VersionFilter filter = new VersionFilter().setMaxCount(2147483647);

        if (this.allowedReleaseTypes == null)
            filter.onlyForTypes((ReleaseType[]) DEFAULT_RELEASE_TYPES.toArray(new ReleaseType[DEFAULT_RELEASE_TYPES.size()]));
        else {
            filter.onlyForTypes((ReleaseType[]) this.allowedReleaseTypes.toArray(new ReleaseType[this.allowedReleaseTypes.size()]));
        }

        return filter;
    }

    public static class Resolution {
        private int width;
        private int height;

        public Resolution() {
        }

        public Resolution(Resolution resolution) {
            this(resolution.getWidth(), resolution.getHeight());
        }

        public Resolution(int width, int height) {
            this.width = width;
            this.height = height;
        }

        public int getWidth() {
            return this.width;
        }

        public int getHeight() {
            return this.height;
        }
    }
}


