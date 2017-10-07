package com.mojang.launcher.versions;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

public class ReleaseTypeAdapterFactory<T extends ReleaseType> extends TypeAdapter<T> {
    private final ReleaseTypeFactory<T> factory;

    public ReleaseTypeAdapterFactory(final ReleaseTypeFactory<T> factory) {
        this.factory = factory;
    }

    @Override
    public void write(final JsonWriter out, final T value) throws IOException {
        out.value(value.getName());
    }

    @Override
    public T read(final JsonReader in) throws IOException {
        return this.factory.getTypeByName(in.nextString());
    }
}
