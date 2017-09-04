package com.mojang.launcher.game.process.direct;

import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.common.collect.EvictingQueue;
import com.mojang.launcher.events.GameOutputLogProcessor;
import com.mojang.launcher.game.process.AbstractGameProcess;

import java.util.Collection;
import java.util.List;

public class DirectGameProcess
        extends AbstractGameProcess {
    private static final int MAX_SYSOUT_LINES = 5;
    protected final DirectProcessInputMonitor monitor;
    private final Process process;
    private final Collection<String> sysOutLines = EvictingQueue.create(5);

    public DirectGameProcess(List<String> commands, Process process, Predicate<String> sysOutFilter, GameOutputLogProcessor logProcessor) {
        super(commands, sysOutFilter);
        this.process = process;
        this.monitor = new DirectProcessInputMonitor(this, logProcessor);

        this.monitor.start();
    }

    public Process getRawProcess() {
        return this.process;
    }

    public Collection<String> getSysOutLines() {
        return this.sysOutLines;
    }

    public boolean isRunning() {
        try {
            this.process.exitValue();
        } catch (IllegalThreadStateException ex) {
            return true;
        }
        return false;
    }

    public int getExitCode() {
        try {
            return this.process.exitValue();
        } catch (IllegalThreadStateException ex) {
            ex.fillInStackTrace();
            throw ex;
        }
    }

    public String toString() {
        return Objects.toStringHelper(this).add("process", this.process).add("monitor", this.monitor).toString();
    }

    public void stop() {
        this.process.destroy();
    }
}