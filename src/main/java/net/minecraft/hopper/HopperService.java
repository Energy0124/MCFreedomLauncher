package net.minecraft.hopper;

import com.google.gson.Gson;
import net.minecraft.launcher.LauncherConstants;

import java.io.IOException;
import java.net.Proxy;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static net.minecraft.launcher.LauncherConstants.URL_HOPPER;

public final class HopperService {
    private static final URL ROUTE_SUBMIT = LauncherConstants.constantURL(URL_HOPPER + "submit_report/");
    private static final URL ROUTE_PUBLISH = LauncherConstants.constantURL(URL_HOPPER + "publish_report/");
    private static final String[] INTERESTING_SYSTEM_PROPERTY_KEYS = new String[]{"os.version", "os.name", "os.arch", "java.version", "java.vendor", "sun.arch.data.model"};
    private static final Gson GSON = new Gson();

    public static SubmitResponse submitReport(final Proxy proxy, final String report, final String product, final String version) throws IOException {
        return submitReport(proxy, report, product, version, null);
    }

    public static SubmitResponse submitReport(final Proxy proxy, final String report, final String product, final String version, final Map<String, String> env) throws IOException {
        final Map<String, String> environment = new HashMap<>();
        if (env != null) {
            environment.putAll(env);
        }
        for (final String key : HopperService.INTERESTING_SYSTEM_PROPERTY_KEYS) {
            final String value = System.getProperty(key);
            if (value != null) {
                environment.put(key, value);
            }
        }
        final SubmitRequest request = new SubmitRequest(report, product, version, environment);
        return makeRequest(proxy, HopperService.ROUTE_SUBMIT, request, SubmitResponse.class);
    }

    public static void publishReport(final Proxy proxy, final Report report) throws IOException {
        final PublishRequest request = new PublishRequest(report);
        makeRequest(proxy, HopperService.ROUTE_PUBLISH, request, PublishResponse.class);
    }

    private static <T extends Response> T makeRequest(final Proxy proxy, final URL url, final Object input, final Class<T> classOfT) throws IOException {
        final String jsonResult = Util.performPost(url, HopperService.GSON.toJson(input), proxy, "application/json", true);
        final T result = HopperService.GSON.fromJson(jsonResult, classOfT);
        if (result == null) {
            return null;
        }
        if (result.getError() != null) {
            throw new IOException(result.getError());
        }
        return result;
    }
}
