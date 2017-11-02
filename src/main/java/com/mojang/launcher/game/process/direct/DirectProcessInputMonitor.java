package com.mojang.launcher.game.process.direct;

import com.mojang.launcher.events.GameOutputLogProcessor;
import com.mojang.launcher.game.process.GameProcessRunnable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

class DirectProcessInputMonitor extends Thread {
    private static final Logger LOGGER = LogManager.getLogger();

    private final DirectGameProcess process;
    private final GameOutputLogProcessor logProcessor;

    DirectProcessInputMonitor(final DirectGameProcess process, final GameOutputLogProcessor logProcessor) {
        this.process = process;
        this.logProcessor = logProcessor;
    }

    @Override
    public void run() {
        final InputStreamReader reader = new InputStreamReader(this.process.getRawProcess().getInputStream());
        final BufferedReader buf = new BufferedReader(reader);
        String line;
        while (this.process.isRunning()) {
            try {
                while ((line = buf.readLine()) != null) {
                    this.logProcessor.onGameOutput(this.process, line);
                    if (this.process.getSysOutFilter().test(line) == Boolean.TRUE) {
                        this.process.getSysOutLines().add(line);
                    }
                }
            } catch (IOException ex) {
                DirectProcessInputMonitor.LOGGER.error(ex);
            } finally {
                try {
                    reader.close();
                } catch (IOException e) {
                    LOGGER.warn("Unable to close reader");
                }
            }
        }
        final GameProcessRunnable onExit = this.process.getExitRunnable();
        if (onExit != null) {
            onExit.onGameProcessEnded(this.process);
        }
    }
}
