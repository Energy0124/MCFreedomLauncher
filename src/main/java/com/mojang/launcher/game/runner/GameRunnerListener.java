package com.mojang.launcher.game.runner;

import com.mojang.launcher.game.GameInstanceStatus;

public abstract interface GameRunnerListener {
    public abstract void onGameInstanceChangedState(GameRunner paramGameRunner, GameInstanceStatus paramGameInstanceStatus);
}