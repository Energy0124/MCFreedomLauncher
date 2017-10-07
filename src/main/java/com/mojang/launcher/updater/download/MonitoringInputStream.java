package com.mojang.launcher.updater.download;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class MonitoringInputStream extends FilterInputStream {
    private final ProgressContainer monitor;

    public MonitoringInputStream(final InputStream in, final ProgressContainer monitor) {
        super(in);
        this.monitor = monitor;
    }

    @Override
    public int read() throws IOException {
        final int result = this.in.read();
        if (result >= 0) {
            this.monitor.addProgress(1L);
        }
        return result;
    }

    @Override
    public int read(final byte[] buffer) throws IOException {
        final int size = this.in.read(buffer);
        if (size >= 0) {
            this.monitor.addProgress(size);
        }
        return size;
    }

    @Override
    public int read(final byte[] buffer, final int off, final int len) throws IOException {
        final int size = this.in.read(buffer, off, len);
        if (size > 0) {
            this.monitor.addProgress(size);
        }
        return size;
    }

    @Override
    public long skip(final long size) throws IOException {
        final long skipped = super.skip(size);
        if (skipped > 0L) {
            this.monitor.addProgress(skipped);
        }
        return skipped;
    }
}
