package com.mojang.launcher.game.process;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import com.mojang.launcher.OperatingSystem;
import com.mojang.launcher.events.GameOutputLogProcessor;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

public class GameProcessBuilder {
    private final String processPath;
    private final List<String> arguments;
    private Predicate<String> sysOutFilter;
    private GameOutputLogProcessor logProcessor;
    private File directory;

    public GameProcessBuilder(String processPath) {
        this.arguments = Lists.newArrayList();
        this.sysOutFilter = s -> true;
        this.logProcessor = (process, logLine) -> {
        };
        if (processPath == null) {
            processPath = OperatingSystem.getCurrentPlatform().getJavaDir();
        }
        this.processPath = processPath;
    }

    public List<String> getFullCommands() {
        final List<String> result = new ArrayList<>(this.arguments);
        result.add(0, this.getProcessPath());
        return result;
    }

    public void withArguments(final String... commands) {
        this.arguments.addAll(Arrays.asList(commands));
    }

    public List<String> getArguments() {
        return this.arguments;
    }

    public void directory(final File directory) {
        this.directory = directory;
    }

    public File getDirectory() {
        return this.directory;
    }

    public void withSysOutFilter(final Predicate<String> predicate) {
        this.sysOutFilter = predicate;
    }

    public void withLogProcessor(final GameOutputLogProcessor logProcessor) {
        this.logProcessor = logProcessor;
    }

    public Predicate<String> getSysOutFilter() {
        return this.sysOutFilter;
    }

    protected String getProcessPath() {
        return this.processPath;
    }

    public GameOutputLogProcessor getLogProcessor() {
        return this.logProcessor;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("processPath", this.processPath).add("arguments", this.arguments).add("sysOutFilter", this.sysOutFilter).add("directory", this.directory).add("logProcessor", this.logProcessor).toString();
    }
}
