package com.mojang.launcher.game.process;

import com.google.common.base.Predicate;

import java.util.Collection;
import java.util.List;

public abstract interface GameProcess {
    public abstract List<String> getStartupArguments();

    public abstract Collection<String> getSysOutLines();

    public abstract Predicate<String> getSysOutFilter();

    public abstract boolean isRunning();

    public abstract GameProcessRunnable getExitRunnable();

    public abstract void setExitRunnable(GameProcessRunnable paramGameProcessRunnable);

    public abstract int getExitCode();

    public abstract void stop();
}