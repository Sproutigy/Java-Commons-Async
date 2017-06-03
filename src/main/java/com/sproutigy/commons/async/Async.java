package com.sproutigy.commons.async;

import java.util.Collection;
import java.util.WeakHashMap;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class Async {
    private Async() {
    }

    private static ExecutorService executorService;
    private static ScheduledThreadPoolExecutor scheduler;

    static {
        executorService = new ThreadPoolExecutor(
                0, Integer.MAX_VALUE,
                60L, TimeUnit.SECONDS,
                new SynchronousQueue<Runnable>(),
                new ThreadFactory() {
                    AtomicInteger counter = new AtomicInteger();

                    @Override
                    public Thread newThread(Runnable r) {
                        Thread thread = new Thread(r);
                        thread.setName(Async.class.getSimpleName() + "-" + counter.incrementAndGet());
                        thread.setPriority(Thread.NORM_PRIORITY - 1);
                        thread.setDaemon(true);
                        return thread;
                    }
                });

        scheduler = new ScheduledThreadPoolExecutor(1, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName(Async.class.getSimpleName() + "-scheduler");
                thread.setDaemon(true);
                thread.setPriority(Thread.MAX_PRIORITY);
                return thread;
            }
        });

        ShutdownOnExitHook.add(scheduler);
        ShutdownOnExitHook.add(executorService);

        executorService = new UnmodifiableExecutorService(executorService);
    }

    public static ExecutorService getService() {
        return executorService;
    }

    public static void execute(Runnable command) {
        executorService.execute(command);
    }

    public static boolean isShuttingDown() {
        return ShutdownOnExitHook.shuttingDown;
    }


    public static ScheduledFuture<?> schedule(final Runnable command, long delay, TimeUnit unit) {
        return scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                execute(command);
            }
        }, delay, unit);
    }

    public static ScheduledFuture<?> scheduleAtFixedRate(final Runnable command, long initialDelay, long period, TimeUnit unit) {
        return scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                execute(command);
            }
        }, initialDelay, period, unit);
    }


    private static class ShutdownOnExitHook {
        private static WeakHashMap<ExecutorService, Object> hookedExecutorServices = new WeakHashMap<>();

        private static volatile boolean shuttingDown = false;

        static {
            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                @Override
                public void run() {
                    Collection<ExecutorService> executorServices;
                    synchronized (ShutdownOnExitHook.class) {
                        shuttingDown = true;
                        executorServices = hookedExecutorServices.keySet();
                        hookedExecutorServices = null;
                    }

                    for (ExecutorService executorService : executorServices) {
                        executorService.shutdown();
                        try {
                            executorService.awaitTermination(30, TimeUnit.SECONDS);
                        } catch (InterruptedException ignore) {
                        }
                    }
                }
            }));
        }

        private ShutdownOnExitHook() {
        }

        static void add(ExecutorService executorService) {
            synchronized (ShutdownOnExitHook.class) {
                if (hookedExecutorServices == null) {
                    // DeleteOnExitHook is running. Too late to add a file
                    throw new IllegalStateException("Shutdown in progress");
                }

                hookedExecutorServices.put(executorService, null);
            }
        }
    }

}
