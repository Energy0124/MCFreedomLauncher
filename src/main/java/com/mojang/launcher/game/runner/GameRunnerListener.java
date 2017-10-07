package com.mojang.launcher.game.runner;

import com.mojang.launcher.game.GameInstanceStatus;

public interface GameRunnerListener {
    void onGameInstanceChangedState(final GameRunner p0, final GameInstanceStatus p1);
}
