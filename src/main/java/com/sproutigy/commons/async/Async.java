package com.sproutigy.commons.async;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

/**
 * @author LukeAheadNET
 */
public class Async {

    private static ExecutorService DEFAULT_EXECUTOR;
    private static ScheduledExecutorService DEFAULT_SCHEDULED_EXECUTOR;

    static {
        DEFAULT_EXECUTOR = newExecutor();
        DEFAULT_SCHEDULED_EXECUTOR = newScheduler();
    }

    private Async() {
    }

    public static ExecutorService commonExecutor() {
        if (DEFAULT_EXECUTOR == null) {
            synchronized (Async.class) {
                if (DEFAULT_EXECUTOR == null) {
                    DEFAULT_EXECUTOR = newExecutor();
                }
            }
        }
        return DEFAULT_EXECUTOR;
    }

    public static ScheduledExecutorService commonScheduler() {
        if (DEFAULT_SCHEDULED_EXECUTOR == null) {
            synchronized (Async.class) {
                if (DEFAULT_SCHEDULED_EXECUTOR == null) {
                    DEFAULT_SCHEDULED_EXECUTOR = newScheduler();
                }
            }
        }
        return DEFAULT_SCHEDULED_EXECUTOR;
    }

    public static ExecutorService newExecutor() {
        return new ExecutorServiceFactory().newExecutorService();
    }

    public static ScheduledExecutorService newScheduler() {
        ExecutorServiceFactory factory = new ExecutorServiceFactory();
        factory.setScheduled(true);
        factory.setCorePoolSize(0);
        factory.setMaximumPoolSize(1);
        factory.setThreadPriority(Thread.NORM_PRIORITY);
        return (ScheduledExecutorService) factory.newExecutorService();
    }

    public static Thread newThread(Runnable runnable) {
        return new ExecutorServiceFactory().newThread(runnable);
    }

}
