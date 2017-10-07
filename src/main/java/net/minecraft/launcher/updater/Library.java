package net.minecraft.launcher.updater;

import com.mojang.launcher.OperatingSystem;
import com.mojang.launcher.updater.download.ChecksummedDownloadable;
import com.mojang.launcher.updater.download.Downloadable;
import com.mojang.launcher.versions.CompatibilityRule;
import com.mojang.launcher.versions.ExtractRules;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StrSubstitutor;

import java.io.File;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.util.*;

import static net.minecraft.launcher.LauncherConstants.URL_LIBRARY_BASE;

public class Library {
    private static final StrSubstitutor SUBSTITUTOR = new StrSubstitutor(new HashMap<String, String>() {
        {
            this.put("arch", System.getProperty("os.arch").contains("64") ? "64" : "32");
        }
    });

    private String name;
    private List<CompatibilityRule> rules;
    private Map<OperatingSystem, String> natives;
    private ExtractRules extract;
    private String url;
    private LibraryDownloadInfo downloads;

    public Library() {
    }

    public Library(final String name) {
        if (name == null || name.length() == 0) {
            throw new IllegalArgumentException("Library name cannot be null or empty");
        }
        this.name = name;
    }

    public Library(final Library library) {
        this.name = library.name;
        this.url = library.url;
        if (library.extract != null) {
            this.extract = new ExtractRules(library.extract);
        }
        if (library.rules != null) {
            this.rules = new ArrayList<>();
            for (final CompatibilityRule compatibilityRule : library.rules) {
                this.rules.add(new CompatibilityRule(compatibilityRule));
            }
        }
        if (library.natives != null) {
            this.natives = new LinkedHashMap<>();
            for (final Map.Entry<OperatingSystem, String> entry : library.getNatives().entrySet()) {
                this.natives.put(entry.getKey(), entry.getValue());
            }
        }
        if (library.downloads != null) {
            this.downloads = new LibraryDownloadInfo(library.downloads);
        }
    }

    public String getName() {
        return this.name;
    }

    public Library addNative(final OperatingSystem operatingSystem, final String name) {
        if (operatingSystem == null || operatingSystem.isSupported()) {
            throw new IllegalArgumentException("Cannot add native for unsupported OS");
        }
        if (name == null || name.length() == 0) {
            throw new IllegalArgumentException("Cannot add native for null or empty name");
        }
        if (this.natives == null) {
            this.natives = new EnumMap<>(OperatingSystem.class);
        }
        this.natives.put(operatingSystem, name);
        return this;
    }

    public List<CompatibilityRule> getCompatibilityRules() {
        return this.rules;
    }

    public boolean appliesToCurrentEnvironment() {
        if (this.rules == null) {
            return true;
        }
        CompatibilityRule.Action lastAction = CompatibilityRule.Action.DISALLOW;
        for (final CompatibilityRule compatibilityRule : this.rules) {
            final CompatibilityRule.Action action = compatibilityRule.getAppliedAction();
            if (action != null) {
                lastAction = action;
            }
        }
        return lastAction == CompatibilityRule.Action.ALLOW;
    }

    public Map<OperatingSystem, String> getNatives() {
        return this.natives;
    }

    public ExtractRules getExtractRules() {
        return this.extract;
    }

    public Library setExtractRules(final ExtractRules rules) {
        this.extract = rules;
        return this;
    }

    public String getArtifactBaseDir() {
        if (this.name == null) {
            throw new IllegalStateException("Cannot get artifact dir of empty/blank artifact");
        }
        final String[] parts = this.name.split(":", 3);
        return String.format("%s/%s/%s", parts[0].replaceAll("\\.", "/"), parts[1], parts[2]);
    }

    public String getArtifactPath() {
        return this.getArtifactPath(null);
    }

    public String getArtifactPath(final String classifier) {
        if (this.name == null) {
            throw new IllegalStateException("Cannot get artifact path of empty/blank artifact");
        }
        return String.format("%s/%s", this.getArtifactBaseDir(), this.getArtifactFilename(classifier));
    }

    public String getArtifactFilename(final String classifier) {
        if (this.name == null) {
            throw new IllegalStateException("Cannot get artifact filename of empty/blank artifact");
        }
        final String[] parts = this.name.split(":", 3);
        final String result = String.format("%s-%s%s.jar", parts[1], parts[2], StringUtils.isEmpty(classifier) ? "" : ("-" + classifier));
        return Library.SUBSTITUTOR.replace(result);
    }

    @Override
    public String toString() {
        return "Library{name='" + this.name + '\'' + ", rules=" + this.rules + ", natives=" + this.natives + ", extract=" + this.extract + '}';
    }

    public Downloadable createDownload(final Proxy proxy, final String path, final File local, final boolean ignoreLocalFiles, final String classifier) throws MalformedURLException {
        if (this.url != null) {
            final URL url = new URL(this.url + path);
            return new ChecksummedDownloadable(proxy, url, local, ignoreLocalFiles);
        }
        if (this.downloads == null) {
            final URL url = new URL(URL_LIBRARY_BASE + path);
            return new ChecksummedDownloadable(proxy, url, local, ignoreLocalFiles);
        }
        final DownloadInfo info = this.downloads.getDownloadInfo(Library.SUBSTITUTOR.replace(classifier));
        if (info != null) {
            return new PreHashedDownloadable(proxy, info.getUrl(), local, ignoreLocalFiles, info.getSha1());
        }
        return null;
    }
}
