package com.sproutigy.commons.async.promises.impl;

import com.sproutigy.commons.async.promises.*;

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
        if (cause == null) throw new NullPointerException("Cause cannot be null");
        this.cause = cause;
        changeState(PromiseState.Failed);
        return this;
    }

}
