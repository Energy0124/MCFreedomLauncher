package com.mojang.launcher.events;

import com.mojang.launcher.game.process.GameProcess;

public abstract interface GameOutputLogProcessor {
    public abstract void onGameOutput(GameProcess paramGameProcess, String paramString);
}