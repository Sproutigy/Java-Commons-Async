package com.sproutigy.commons.async;

/**
 * @author LukeAheadNET
 */
@FunctionalInterface
public interface ThrowableRunnable {
    static ThrowableRunnable fromRunnable(Runnable runnable) {
        return () -> runnable.run();
    }

    static Runnable toRunnable(ThrowableRunnable throwableRunnable) {
        return () -> {
            try {
                throwableRunnable.run();
            } catch (RuntimeException e) {
                throw e;
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        };
    }

    void run() throws Throwable;
}
