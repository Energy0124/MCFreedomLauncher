package com.mojang.launcher.events;

import com.mojang.launcher.game.process.GameProcess;

public interface GameOutputLogProcessor {
    void onGameOutput(final GameProcess p0, final String p1);
}
