package com.sproutigy.commons.async;

/**
 * @author LukeAheadNET
 */
@FunctionalInterface
public interface RunnableThrowable {
    static RunnableThrowable fromRunnable(Runnable runnable) {
        return runnable::run;
    }

    static Runnable toRunnable(RunnableThrowable runnableThrowable) {
        return () -> {
            try {
                runnableThrowable.run();
            } catch (RuntimeException e) {
                throw e;
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        };
    }

    void run() throws Exception;
}
