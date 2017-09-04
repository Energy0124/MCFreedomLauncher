package com.mojang.launcher.game.process;

import java.io.IOException;

public abstract interface GameProcessFactory {
    public abstract GameProcess startGame(GameProcessBuilder paramGameProcessBuilder)
            throws IOException;
}