package com.mojang.launcher.game.process.direct;

import com.google.common.base.MoreObjects;
import com.google.common.collect.EvictingQueue;
import com.mojang.launcher.events.GameOutputLogProcessor;
import com.mojang.launcher.game.process.AbstractGameProcess;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

public class DirectGameProcess extends AbstractGameProcess {
    private static final int MAX_SYSOUT_LINES = 5;
    private final DirectProcessInputMonitor monitor;
    private final Process process;
    private final Collection<String> sysOutLines;

    public DirectGameProcess(final List<String> commands, final Process process, final Predicate<String> sysOutFilter, final GameOutputLogProcessor logProcessor) {
        super(commands, sysOutFilter);
        this.sysOutLines = EvictingQueue.create(MAX_SYSOUT_LINES);
        this.process = process;
        (this.monitor = new DirectProcessInputMonitor(this, logProcessor)).start();
    }

    public Process getRawProcess() {
        return this.process;
    }

    @Override
    public Collection<String> getSysOutLines() {
        return this.sysOutLines;
    }

    @Override
    public boolean isRunning() {
        try {
            this.process.exitValue();
        } catch (IllegalThreadStateException ex) {
            return true;
        }
        return false;
    }

    @Override
    public int getExitCode() {
        try {
            return this.process.exitValue();
        } catch (IllegalThreadStateException ex) {
            ex.fillInStackTrace();
            throw ex;
        }
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("process", this.process).add("monitor", this.monitor).toString();
    }

    @Override
    public void stop() {
        this.process.destroy();
    }
}
