package com.mojang.launcher.updater;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.*;

public class ExceptionalThreadPoolExecutor extends ThreadPoolExecutor {
    private static final Logger LOGGER;

    static {
        LOGGER = LogManager.getLogger();
    }

    public ExceptionalThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, new LinkedBlockingQueue());
    }

    protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);
        if ((t == null) && ((r instanceof Future))) {
            try {
                Future<?> future = (Future) r;
                if (future.isDone()) {
                    future.get();
                }
            } catch (CancellationException ce) {
                t = ce;
            } catch (ExecutionException ee) {
                t = ee.getCause();
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
        return new ExceptionalFutureTask(runnable, value);
    }

    @Override
    protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
        return new ExceptionalFutureTask(callable);
    }

    public class ExceptionalFutureTask<T> extends FutureTask<T> {
        public ExceptionalFutureTask(final Callable<T> callable) {
            super(callable);
        }

        public ExceptionalFutureTask(final Runnable runnable, final T result) {
            super(runnable, result);
        }

        protected void done() {
            try {
                get();
            } catch (Throwable t) {
                ExceptionalThreadPoolExecutor.LOGGER.error("Unhandled exception in executor " + this, t);
            }
        }
    }
}