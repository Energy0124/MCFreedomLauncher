package net.minecraft.launcher.updater;

import com.mojang.launcher.Http;
import com.mojang.launcher.OperatingSystem;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;

public class RemoteVersionList
        extends VersionList {
    private final String baseUrl;
    private final Proxy proxy;

    public RemoteVersionList(String baseUrl, Proxy proxy) {
        this.baseUrl = baseUrl;
        this.proxy = proxy;
    }

    public boolean hasAllFiles(CompleteMinecraftVersion version, OperatingSystem os) {
        return true;
    }

    public String getContent(String path)
            throws IOException {
        return Http.performGet(getUrl(path), this.proxy);
    }

    public URL getUrl(String file)
            throws MalformedURLException {
        return new URL(this.baseUrl + file);
    }

    public Proxy getProxy() {
        return this.proxy;
    }
}