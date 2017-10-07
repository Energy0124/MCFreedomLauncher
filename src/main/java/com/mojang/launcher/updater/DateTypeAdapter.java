package com.mojang.launcher.updater;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateTypeAdapter implements JsonSerializer<Date>, JsonDeserializer<Date> {
    private final DateFormat enUsFormat;
    private final DateFormat iso8601Format;

    public DateTypeAdapter() {
        this.enUsFormat = DateFormat.getDateTimeInstance(2, 2, Locale.US);
        this.iso8601Format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
    }

    @Override
    public Date deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) throws JsonParseException {
        if (!(json instanceof JsonPrimitive)) {
            throw new JsonParseException("The date should be a string value");
        }
        final Date date = this.deserializeToDate(json.getAsString());
        if (typeOfT == Date.class) {
            return date;
        }
        throw new IllegalArgumentException(this.getClass() + " cannot deserialize to " + typeOfT);
    }

    @Override
    public JsonElement serialize(final Date src, final Type typeOfSrc, final JsonSerializationContext context) {
        synchronized (this.enUsFormat) {
            return new JsonPrimitive(this.serializeToString(src));
        }
    }

    public Date deserializeToDate(final String string) {
        synchronized (this.enUsFormat) {
            try {
                return this.enUsFormat.parse(string);
            } catch (ParseException ex) {
                try {
                    return this.iso8601Format.parse(string);
                } catch (ParseException ex2) {
                    try {
                        String cleaned = string.replace("Z", "+00:00");
                        cleaned = cleaned.substring(0, 22) + cleaned.substring(23);
                        return this.iso8601Format.parse(cleaned);
                    } catch (Exception e) {
                        throw new JsonSyntaxException("Invalid date: " + string, e);
                    }
                }
            }
        }
    }

    public String serializeToString(final Date date) {
        synchronized (this.enUsFormat) {
            final String result = this.iso8601Format.format(date);
            return result.substring(0, 22) + ":" + result.substring(22);
        }
    }
}
