package net.minecraft.hopper;

import org.apache.commons.io.IOUtils;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;

import static java.nio.charset.StandardCharsets.UTF_8;

public class Util {
    public static String performPost(final URL url, final String parameters, final Proxy proxy, final String contentType, final boolean returnErrorPage) throws IOException {
        final HttpURLConnection connection = (HttpURLConnection) url.openConnection(proxy);
        final byte[] paramAsBytes = parameters.getBytes(UTF_8);
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(15000);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", contentType + "; charset=utf-8");
        connection.setRequestProperty("Content-Length", "" + paramAsBytes.length);
        connection.setRequestProperty("Content-Language", "en-US");
        connection.setUseCaches(false);
        connection.setDoInput(true);
        connection.setDoOutput(true);
        final DataOutputStream writer = new DataOutputStream(connection.getOutputStream());
        writer.write(paramAsBytes);
        writer.flush();
        writer.close();
        InputStream stream;
        try {
            stream = connection.getInputStream();
        } catch (IOException e) {
            if (!returnErrorPage) {
                throw e;
            }
            stream = connection.getErrorStream();
            if (stream == null) {
                throw e;
            }
        }
        return IOUtils.toString(stream, UTF_8);
    }

    public static URL constantURL(final String input) {
        try {
            return new URL(input);
        } catch (MalformedURLException e) {
            throw new Error(e);
        }
    }
}
