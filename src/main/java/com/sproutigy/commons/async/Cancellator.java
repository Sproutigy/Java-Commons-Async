package com.sproutigy.commons.async;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

public final class Cancellator {
    private volatile boolean cancelled = false;
    private CopyOnWriteArrayList<Runnable> listeners = new CopyOnWriteArrayList<>();

    public boolean isCancelled() {
        return cancelled;
    }

    public void cancel() {
        if (!cancelled) {
            boolean applied = false;
            synchronized (this) {
                if (!cancelled) {
                    cancelled = true;
                    this.notifyAll();
                    applied = true;
                }
            }

            if (applied) {
                for(Runnable listener : listeners) {
                    listener.run();
                }

                listeners.clear();
            }
        }
    }

    public void await() throws InterruptedException {
        synchronized (this) {
            this.wait();
        }
    }

    public boolean awaitUnchecked() {
        synchronized (this) {
            try {
                this.wait();
                return true;
            } catch (InterruptedException e) {
                return false;
            }
        }
    }

    public void await(long timeoutDuration, TimeUnit timeUnit) throws InterruptedException {
        synchronized (this) {
            this.wait(timeUnit.toMillis(timeoutDuration));
        }
    }

    public boolean awaitUnchecked(long timeoutDuration, TimeUnit timeUnit) {
        synchronized (this) {
            try {
                this.wait(timeUnit.toMillis(timeoutDuration));
                return true;
            } catch (InterruptedException e) {
                return false;
            }
        }
    }

    public void addListener(Runnable listener) {
        if (!cancelled) {
            listeners.add(listener);
        }
    }

    public void removeListener(Runnable listener) {
        listeners.remove(listener);
    }

}
