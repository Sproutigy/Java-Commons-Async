package com.sproutigy.commons.async;

import java.util.Collection;
import java.util.WeakHashMap;
import java.util.concurrent.*;

/**
 * @author LukeAheadNET
 */
public class ExecutorServiceFactory implements ThreadFactory {

    private int corePoolSize = 0;
    private int maximumPoolSize = 100 * Runtime.getRuntime().availableProcessors();
    private long keepAliveTimeNanos = TimeUnit.SECONDS.toNanos(30L);
    private boolean daemonThread = true;
    private int threadPriority = (Thread.NORM_PRIORITY - Thread.MIN_PRIORITY) / 2;
    private boolean registerShutdownHook = true;
    private boolean scheduled = false;

    public int getCorePoolSize() {
        return corePoolSize;
    }

    public void setCorePoolSize(int corePoolSize) {
        this.corePoolSize = corePoolSize;
    }

    public int getMaximumPoolSize() {
        return maximumPoolSize;
    }

    public void setMaximumPoolSize(int maximumPoolSize) {
        this.maximumPoolSize = maximumPoolSize;
    }

    public long getKeepAliveTimeNanos() {
        return keepAliveTimeNanos;
    }

    public void setKeepAliveTimeNanos(long keepAliveTimeNanos) {
        this.keepAliveTimeNanos = keepAliveTimeNanos;
    }

    public void setKeepAliveTime(long keepAliveTime, TimeUnit timeUnit) {
        setKeepAliveTimeNanos(timeUnit.toNanos(keepAliveTime));
    }

    public boolean isDaemonThread() {
        return daemonThread;
    }

    public void setDaemonThread(boolean daemonThread) {
        this.daemonThread = daemonThread;
    }

    public int getThreadPriority() {
        return threadPriority;
    }

    public void setThreadPriority(int threadPriority) {
        this.threadPriority = threadPriority;
    }

    public boolean isRegisterShutdownHook() {
        return registerShutdownHook;
    }

    public void setRegisterShutdownHook(boolean registerShutdownHook) {
        this.registerShutdownHook = registerShutdownHook;
    }

    public boolean isScheduled() {
        return scheduled;
    }

    public void setScheduled(boolean scheduled) {
        this.scheduled = scheduled;
    }

    public ExecutorService newExecutorService() {

        final ThreadPoolExecutor executorService;

        if (isScheduled()) {
            executorService = new ScheduledThreadPoolExecutor(getCorePoolSize(), this);
            executorService.setKeepAliveTime(getKeepAliveTimeNanos(), TimeUnit.NANOSECONDS);
            executorService.setMaximumPoolSize(getMaximumPoolSize());
        } else {
            //based on Executors.newCachedThreadPool()
            executorService = new ThreadPoolExecutor(getCorePoolSize(), getMaximumPoolSize(),
                    getKeepAliveTimeNanos(), TimeUnit.NANOSECONDS,
                    new SynchronousQueue<Runnable>(), this);
        }

        if (isRegisterShutdownHook()) {
            ShutdownOnExitHook.add(executorService);
        }

        return executorService;
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread thread = new Thread(r);
        thread.setDaemon(daemonThread);
        thread.setPriority(threadPriority);
        return thread;
    }


    private static class ShutdownOnExitHook {
        private static WeakHashMap<ExecutorService, Object> hookedExecutorServices = new WeakHashMap<>();

        static {
            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                @Override
                public void run() {
                    runHooks();
                }
            }));
        }

        private ShutdownOnExitHook() {
        }

        static synchronized void add(ExecutorService executorService) {
            if (hookedExecutorServices == null) {
                // DeleteOnExitHook is running. Too late to add a file
                throw new IllegalStateException("Shutdown in progress");
            }

            hookedExecutorServices.put(executorService, null);
        }

        static void runHooks() {
            Collection<ExecutorService> executorServices;

            synchronized (ShutdownOnExitHook.class) {
                executorServices = hookedExecutorServices.keySet();
                hookedExecutorServices = null;
            }

            for (ExecutorService executorService : executorServices) {
                executorService.shutdown();
            }

            for (ExecutorService executorService : executorServices) {
                executorService.shutdownNow();
            }
        }
    }

}
