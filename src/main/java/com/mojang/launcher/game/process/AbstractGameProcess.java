package com.mojang.launcher.game.process;

import com.google.common.base.Predicate;

import java.util.List;

public abstract class AbstractGameProcess
        implements GameProcess {
    protected final List<String> arguments;
    protected final Predicate<String> sysOutFilter;
    private GameProcessRunnable onExit;

    public AbstractGameProcess(List<String> arguments, Predicate<String> sysOutFilter) {
        this.arguments = arguments;
        this.sysOutFilter = sysOutFilter;
    }

    public Predicate<String> getSysOutFilter() {
        return this.sysOutFilter;
    }

    public List<String> getStartupArguments() {
        return this.arguments;
    }

    public GameProcessRunnable getExitRunnable() {
        return this.onExit;
    }

    public void setExitRunnable(GameProcessRunnable runnable) {
        this.onExit = runnable;
        if ((!isRunning()) && (runnable != null)) {
            runnable.onGameProcessEnded(this);
        }
    }
}