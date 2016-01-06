package com.sproutigy.commons.async.promises.impl;

import com.sproutigy.commons.async.Transformer;
import com.sproutigy.commons.async.exceptions.UncheckedInterruptedException;
import com.sproutigy.commons.async.promises.*;
import com.sproutigy.commons.async.promises.listeners.ProgressListener;
import com.sproutigy.commons.async.promises.listeners.PromiseStateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author LukeAheadNET
 */
public abstract class AbstractDeferredPromise<V> extends AbstractPromise<V> implements Promise<V> {

    private final static Logger log = LoggerFactory.getLogger(AbstractDeferredPromise.class);
    protected PromiseFactory promiseFactory;
    protected volatile V value;
    protected volatile Throwable cause;
    protected volatile PromiseState state = PromiseState.Waiting;
    protected CancellationHandler cancellationHandler;
    protected CopyOnWriteArrayList<PromiseStateListener<V>> stateListeners = new CopyOnWriteArrayList<>();
    protected CopyOnWriteArrayList<ProgressListener> progressListeners = new CopyOnWriteArrayList<>();
    public AbstractDeferredPromise(PromiseFactory promiseFactory) {
        this.promiseFactory = promiseFactory;
    }

    @Override
    public Throwable cause() {
        awaitUnchecked();
        if (isSuccess()) {
            throw new IllegalStateException("Promise has succeeded");
        }
        return cause;
    }

    @Override
    public PromiseState state() {
        return state;
    }

    private <IN, OUT> void handleThen(Promise<IN> sourcePromise, Deferred<OUT> targetDeferred, Transformer<IN, OUT> onSuccess, Transformer<Throwable, OUT> onFailure) {
        if (log.isTraceEnabled())
            log.trace("Executing processing of [" + targetDeferred.getIdentifier() + "] after [" + sourcePromise.getIdentifier() + "] on thread " + Thread.currentThread().toString());
        targetDeferred.pending();

        if (sourcePromise.isSuccess()) {
            IN input = sourcePromise.getValue();
            try {
                if (onSuccess == null) {
                    targetDeferred.success((OUT) input);
                } else {
                    OUT output = onSuccess.process(input);
                    targetDeferred.success(output);
                }
            } catch (Throwable e) {
                targetDeferred.failure(e);
            }
        } else {
            Throwable cause = sourcePromise.cause();
            if (onFailure == null) {
                targetDeferred.failure(cause);
            } else {
                try {
                    OUT output = onFailure.process(cause);
                    targetDeferred.success(output);
                } catch (Throwable e) {
                    targetDeferred.failure(e);
                }
            }
        }
    }

    @Override
    public <OUT> Promise<OUT> thenFactorize(PromiseFactory promiseFactory, Transformer<V, OUT> onSuccess) {
        return thenFactorize(promiseFactory, onSuccess, null);
    }

    @Override
    public <OUT> Promise<OUT> thenFactorize(PromiseFactory promiseFactory, Transformer<V, OUT> onSuccess, Transformer<Throwable, OUT> onFailure) {
        Deferred<OUT> deferred = promiseFactory.defer();

        addStateListener((promise, state) -> {
            if (!state.isDone()) return;
            promiseFactory.getAsyncExecutor().execute(() -> handleThen(promise, deferred, onSuccess, onFailure));
        });

        return deferred.promise();
    }

    @Override
    public <OUT> Promise<OUT> thenFactorize(PromiseFactory promiseFactory, Transformer<V, OUT> onSuccess, Transformer<Throwable, OUT> onFailure, ProgressListener onProgress) {
        progress(onProgress);
        return thenFactorize(promiseFactory, onSuccess, onFailure);
    }

    @Override
    public <OUT> Promise<OUT> then(Transformer<V, OUT> onSuccess) {
        return then(onSuccess, null);
    }

    @Override
    public <OUT> Promise<OUT> then(Transformer<V, OUT> onSuccess, Transformer<Throwable, OUT> onFailure) {
        return thenFactorize(getFactory(), onSuccess, onFailure);
    }

    @Override
    public <OUT> Promise<OUT> then(Transformer<V, OUT> onSuccess, Transformer<Throwable, OUT> onFailure, ProgressListener onProgress) {
        return thenFactorize(getFactory(), onSuccess, onFailure, onProgress);
    }

    @Override
    public <OUT> Promise<OUT> thenPromised(PromiseProviderByInput<V, OUT> onSuccess) {
        return thenPromised(onSuccess, null);
    }

    @Override
    public <OUT> Promise<OUT> thenPromised(PromiseProviderByInput<V, OUT> onSuccess, PromiseProviderByInput<Throwable, OUT> onFailure) {
        Deferred<OUT> deferred = promiseFactory.defer();
        addStateListener((promise, state) -> {
            if (!state.isDone()) return;

            deferred.pending();

            if (state == PromiseState.Succeeded) {
                V input = promise.getValue();
                try {
                    if (onSuccess == null) {
                        deferred.success((OUT) input);
                    } else {
                        Promise<OUT> nextPromise = onSuccess.provide(input, getFactory());
                        if (nextPromise != null) {
                            promiseFactory.bind(nextPromise, deferred);
                        } else {
                            deferred.success((OUT) input);
                        }
                    }
                } catch (Throwable e) {
                    deferred.failure(e);
                }
            } else {
                Throwable cause = promise.cause();
                if (onFailure == null) {
                    deferred.failure(cause);
                } else {
                    try {
                        Promise<OUT> nextPromise = onFailure.provide(cause, getFactory());
                        if (nextPromise != null) {
                            promiseFactory.bind(nextPromise, deferred);
                        } else {
                            deferred.failure(cause);
                        }
                    } catch (Throwable e) {
                        deferred.failure(e);
                    }
                }
            }
        });
        return deferred.promise();
    }

    @Override
    public <OUT> Promise<OUT> thenBlocking(Transformer<V, OUT> onSuccess) {
        return thenBlocking(onSuccess, null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <OUT> Promise<OUT> thenBlocking(Transformer<V, OUT> onSuccess, Transformer<Throwable, OUT> onFailure) {
        Deferred<OUT> deferred = promiseFactory.defer();

        addStateListener((promise, state) -> {
            if (!state.isDone()) return;
            handleThen(promise, deferred, onSuccess, onFailure);
        });

        return deferred.promise();
    }

    @Override
    public Promise<V> catchFail(Transformer<Throwable, V> onFailure) {
        return thenBlocking(null, onFailure);
    }

    @Override
    public Promise<V> addStateListener(PromiseStateListener<V> stateListener) {
        boolean added = false;
        if (!isDone()) {
            synchronized (this) {
                if (!isDone()) {
                    stateListeners.add(stateListener);
                    added = true;
                }
            }
        }

        if (!added) {
            try {
                stateListener.onPromiseStateChanged(this, state());
            } catch (Throwable throwable) {
                log.error("Promise [" + getIdentifier() + "] state listener thrown error", throwable);
            }
        }

        return this;
    }

    @Override
    public Promise<V> removeStateListener(PromiseStateListener<V> stateListener) {
        stateListeners.remove(stateListener);
        return this;
    }

    @Override
    public Promise<V> progress(ProgressListener onProgress) {
        if (!isDone()) {
            progressListeners.add(onProgress);
        }
        return this;
    }

    @Override
    public Promise<V> removeProgressListener(ProgressListener onProgress) {
        progressListeners.remove(onProgress);
        return this;
    }

    @SuppressWarnings("unchecked")
    protected void notifyProgress(Object progress) {
        for (ProgressListener progressListener : progressListeners) {
            try {
                progressListener.onProgress(progress);
            } catch (Throwable error) {
                log.error("Promise [" + getIdentifier() + "] progress listener thrown error", error);
            }
        }
    }

    @Override
    public void await() throws InterruptedException {
        if (isDone()) return;

        while (!isDone()) {
            try {
                if (log.isTraceEnabled())
                    log.trace("Promise [" + this.getIdentifier() + "] is awaited by the thread " + Thread.currentThread().toString());

                synchronized (this) {
                    wait();
                }
            } catch (InterruptedException e) {
                throw new UncheckedInterruptedException(e);
            }
        }
    }

    @Override
    public void await(long timeout, TimeUnit timeoutUnit) throws InterruptedException, TimeoutException {
        if (isDone()) return;

        boolean useTimeout = timeout > 0 && timeoutUnit != null;
        long expireTimeMillis = useTimeout ? System.currentTimeMillis() + timeoutUnit.toMillis(timeout) : 0;

        synchronized (this) {
            if (useTimeout) {
                if (isDone()) {
                    return;
                }

                if (log.isTraceEnabled())
                    log.trace("Promise [" + this.getIdentifier() + "] is awaited by the thread " + Thread.currentThread().toString());

                timeoutUnit.timedWait(this, timeout);
            }

            while (!isDone()) {
                if (useTimeout) {
                    long timeLeftMillis = expireTimeMillis - System.currentTimeMillis();
                    if (timeLeftMillis <= 0) {
                        throw new TimeoutException();
                    }
                    wait(timeLeftMillis);
                } else {
                    wait();
                }
            }
        }
    }

    @Override
    public void awaitUninterruptibly() {
        try {
            await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void awaitUninterruptibly(long timeout, TimeUnit timeoutUnit) throws TimeoutException {
        try {
            await(timeout, timeoutUnit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void awaitUnchecked() {
        try {
            await();
        } catch (InterruptedException e) {
            throw new UncheckedInterruptedException(e);
        }
    }

    @Override
    public V getValue() {
        if (isFailure()) {
            throw new IllegalStateException("Promise has failed", cause());
        }
        return value;
    }

    @Override
    public V getNow() {
        return value;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        if (cancellationHandler != null) {
            return cancellationHandler.handleCancellation(mayInterruptIfRunning);
        }
        return false;
    }

    @Override
    public boolean isCancelled() {
        awaitUnchecked();
        return state() == PromiseState.Cancelled;
    }

    @Override
    public boolean isDone() {
        return state().isDone();
    }

    @Override
    public V get() throws InterruptedException, ExecutionException {
        await();
        if (isFailure())
            throw new ExecutionException(cause());
        return getValue();
    }

    @Override
    public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        await(timeout, unit);
        if (isFailure())
            throw new ExecutionException(cause());
        return getValue();
    }

    @Override
    public PromiseFactory getFactory() {
        return promiseFactory;
    }

    @Override
    public boolean isSuccess() {
        awaitUnchecked();
        return state() == PromiseState.Succeeded;
    }

    @Override
    public boolean isFailure() {
        awaitUnchecked();
        return state() == PromiseState.Failed;
    }

    protected void changeState(PromiseState newState) {
        boolean callListeners = false;
        synchronized (this) {
            if (!state.isDone() && state.ordinal() < newState.ordinal()) {
                state = newState;
                if (log.isTraceEnabled())
                    log.trace("Promise [" + this.getIdentifier() + "] state: " + newState.toString());

                callListeners = true;

                if (state.isDone()) {
                    this.notifyAll(); //resume awaiting threads
                }
            }
        }

        if (callListeners) {
            for (PromiseStateListener<V> listener : stateListeners) {
                try {
                    listener.onPromiseStateChanged(this, newState);
                } catch (Throwable e) {
                    log.error("Promise state listener thrown error", e);
                }
            }
        }
    }
}
