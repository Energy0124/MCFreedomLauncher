package com.mojang.launcher.game.process;

import java.util.List;
import java.util.function.Predicate;

public abstract class AbstractGameProcess implements GameProcess {
    protected final List<String> arguments;
    protected final Predicate<String> sysOutFilter;
    private GameProcessRunnable onExit;

    public AbstractGameProcess(final List<String> arguments, final Predicate<String> sysOutFilter) {
        this.arguments = arguments;
        this.sysOutFilter = sysOutFilter;
    }

    @Override
    public Predicate<String> getSysOutFilter() {
        return this.sysOutFilter;
    }

    @Override
    public List<String> getStartupArguments() {
        return this.arguments;
    }

    @Override
    public GameProcessRunnable getExitRunnable() {
        return this.onExit;
    }

    @Override
    public void setExitRunnable(final GameProcessRunnable runnable) {
        this.onExit = runnable;
        if (!this.isRunning() && runnable != null) {
            runnable.onGameProcessEnded(this);
        }
    }
}
