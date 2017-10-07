package com.mojang.launcher.updater;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.File;
import java.io.IOException;

public class FileTypeAdapter extends TypeAdapter<File> {
    @Override
    public void write(final JsonWriter out, final File value) throws IOException {
        if (value == null) {
            out.nullValue();
        } else {
            out.value(value.getAbsolutePath());
        }
    }

    @Override
    public File read(final JsonReader in) throws IOException {
        if (in.hasNext()) {
            final String name = in.nextString();
            return (name != null) ? new File(name) : null;
        }
        return null;
    }
}
