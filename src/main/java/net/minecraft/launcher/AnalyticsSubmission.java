package net.minecraft.launcher;

import com.google.gson.*;
import com.mojang.launcher.OperatingSystem;
import net.minecraft.hopper.Util;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.Proxy;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import static net.minecraft.launcher.LauncherConstants.*;

class AnalyticsSubmission {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String SESSION_ID = UUID.randomUUID().toString();
    private static final DateFormat DATE_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
    private static final String CONTENT_TYPE = "application/ms-maelstrom.v3+json;type=eventbatch";
    private static final Gson GSON = new GsonBuilder().create();

    static void sendStartup(final Proxy proxy) {
        final JsonObject object = new JsonObject();
        final JsonArray events = new JsonArray();
        events.add(createStartupEvent());
        object.add("events", events);
        final String json = AnalyticsSubmission.GSON.toJson(object);
        try {
            Util.performPost(Util.constantURL(URL_ANALYTICS_SUBMISSION + AnalyticsSubmission.SESSION_ID), json, proxy, AnalyticsSubmission.CONTENT_TYPE, false);
        } catch (IOException ex) {
            LOGGER.debug("An IOException is caught!");
        }
    }

    private static JsonObject createStartupEvent() {
        final JsonObject result = new JsonObject();
        final JsonObject body = new JsonObject();
        final JsonObject properties = new JsonObject();
        properties.add("oldToken", JsonNull.INSTANCE);
        properties.addProperty("newToken", AnalyticsSubmission.SESSION_ID);
        properties.addProperty("osName", OperatingSystem.getCurrentPlatform().getName());
        properties.addProperty("osVersion", System.getProperty("os.version"));
        properties.addProperty("osArch", System.getProperty("os.arch"));
        properties.addProperty("versionFormat", FORMAT_VERSION);
        properties.addProperty("profilesFormat", FORMAT_PROFILES);
        properties.addProperty("launcherVersion", LauncherConstants.getVersionName());
        body.addProperty("eventName", "startup");
        body.add("properties", properties);
        result.addProperty("sequenceId", 0);
        result.addProperty("timestampUtc", formatTime());
        result.addProperty("typeId", 0);
        result.add("body", body);
        return result;
    }

    private static String formatTime() {
        final String result = AnalyticsSubmission.DATE_TIME_FORMAT.format(new Date());
        return result.substring(0, 22) + ":" + result.substring(22);
    }
}
