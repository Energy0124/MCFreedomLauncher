package com.mojang.launcher;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;

import static org.apache.commons.codec.CharEncoding.UTF_8;

public class Http {
    private static final Logger LOGGER = LogManager.getLogger();

    public static String buildQuery(final Map<String, Object> query) {
        final StringBuilder builder = new StringBuilder();
        for (final Map.Entry<String, Object> entry : query.entrySet()) {
            if (builder.length() > 0) {
                builder.append('&');
            }
            try {
                builder.append(URLEncoder.encode(entry.getKey(), UTF_8));
            } catch (UnsupportedEncodingException e) {
                Http.LOGGER.error("Unexpected exception building query", e);
            }
            if (entry.getValue() != null) {
                builder.append('=');
                try {
                    builder.append(URLEncoder.encode(entry.getValue().toString(), UTF_8));
                } catch (UnsupportedEncodingException e) {
                    Http.LOGGER.error("Unexpected exception building query", e);
                }
            }
        }
        return builder.toString();
    }

    public static String performGet(final URL url, final Proxy proxy) throws IOException {
        final HttpURLConnection connection = (HttpURLConnection) url.openConnection(proxy);
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(60000);
        connection.setRequestMethod("GET");
        final InputStream inputStream = connection.getInputStream();
        try {
            return IOUtils.toString(inputStream, UTF_8);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }
}
