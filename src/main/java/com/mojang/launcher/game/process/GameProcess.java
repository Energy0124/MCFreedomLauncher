package com.mojang.launcher.game.process;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

public interface GameProcess {
    List<String> getStartupArguments();

    Collection<String> getSysOutLines();

    Predicate<String> getSysOutFilter();

    boolean isRunning();

    GameProcessRunnable getExitRunnable();

    void setExitRunnable(final GameProcessRunnable p0);

    int getExitCode();

    void stop();
}
