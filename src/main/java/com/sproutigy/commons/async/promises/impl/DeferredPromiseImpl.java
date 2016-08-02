package com.sproutigy.commons.async.promises.impl;

import com.sproutigy.commons.async.RunnableThrowable;
import com.sproutigy.commons.async.promises.*;

import java.util.concurrent.Callable;

/**
 * @author LukeAheadNET
 */
public class DeferredPromiseImpl<V> extends AbstractDeferredPromise<V> implements Deferred<V> {

    public DeferredPromiseImpl(PromiseFactory promiseFactory) {
        super(promiseFactory);
    }

    @Override
    public Deferred<V> pending() {
        if (state == PromiseState.Waiting) {
            changeState(PromiseState.Pending);
        }
        return this;
    }

    @Override
    public Deferred<V> progress(Object progress) {
        notifyProgress(progress);
        return this;
    }

    @Override
    public Promise<V> promise() {
        return this;
    }

    @Override
    public Deferred<V> identify(String identifier) {
        setIdentifier(identifier);
        return this;
    }

    @Override
    public Deferred<V> cancellationHandler(CancellationHandler cancellationHandler) {
        this.cancellationHandler = cancellationHandler;
        return this;
    }

    @Override
    public synchronized Deferred<V> success() {
        return success(null);
    }

    @Override
    public synchronized Deferred<V> success(V value) {
        if (isDone()) {
            throw new IllegalStateException("Promise already done");
        }
        this.value = value;
        changeState(PromiseState.Succeeded);
        return this;
    }

    @Override
    public Deferred<V> failure() {
        return failure(new Exception("Promise has been rejected"));
    }

    @Override
    public synchronized Deferred<V> failure(Throwable cause) {
        if (cause == null) {
            throw new NullPointerException("Cause cannot be null");
        }
        if (isDone()) {
            throw new IllegalStateException("Promise already done");
        }

        this.cause = cause;
        changeState(PromiseState.Failed);
        return this;
    }

    @Override
    public Deferred<V> runBlocking(RunnableThrowable runnableThrowable) {
        try {
            runnableThrowable.run();
            if (!isDone()) {
                success();
            }
            return this;
        } catch (Throwable e) {
            return failure(e);
        }
    }

    @Override
    public Deferred<V> callBlocking(Callable<V> callable) {
        try {
            V value = callable.call();
            if (!isDone()) {
                success(value);
            }
            return this;
        } catch (Throwable e) {
            return failure(e);
        }
    }

    @Override
    public Deferred<V> runAsync(RunnableThrowable runnableThrowable) {
        getExecutor().execute(() -> runBlocking(runnableThrowable));
        return this;
    }

    @Override
    public Deferred<V> callAsync(Callable<V> callable) {
        getExecutor().execute(() -> callBlocking(callable));
        return this;
    }

    public Deferred<V> executeAndCatch(RunnableThrowable runnableThrowable) {
        try {
            runnableThrowable.run();
        } catch (Throwable e) {
            failure(e);
        }

        return this;
    }
}
