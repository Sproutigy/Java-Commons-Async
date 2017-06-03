package com.sproutigy.commons.async;

import org.slf4j.Logger;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;

class QueuedExecution implements Executor, Runnable {

    private static Logger log = OptionalLogger.getLogger(QueuedExecution.class);

    private Executor executor;
    private ConcurrentLinkedQueue<Runnable> ownQueue = new ConcurrentLinkedQueue<>();
    private volatile boolean busy = false;


    public QueuedExecution(Executor executor) {
        this.executor = executor;
    }

    public Executor getExecutor() {
        return executor;
    }

    @Override
    public void execute(Runnable command) {
        ownQueue.add(command);

        boolean execute = false;
        synchronized (this) {
            if (!busy && !ownQueue.isEmpty()) {
                busy = true;
                execute = true;
            }
        }

        if (execute) {
            if (executor != null) {
                executor.execute(this);
            } else {
                run();
            }
        }
    }

    @Override
    public void run() {
        while (!ownQueue.isEmpty()) {
            Runnable runnable = ownQueue.poll();
            if (runnable != null) {
                try {
                    runnable.run();
                } catch (Throwable e) {
                    OptionalLogger.error(log, "Execution of queued runnable failed {}", runnable);
                }
            }
        }

        synchronized (this) {
            busy = false;
        }
    }
}
