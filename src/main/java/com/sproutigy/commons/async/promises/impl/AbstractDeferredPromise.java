package com.sproutigy.commons.async.promises.impl;

import com.sproutigy.commons.async.Handler;
import com.sproutigy.commons.async.RunnableThrowable;
import com.sproutigy.commons.async.Transformer;
import com.sproutigy.commons.async.exceptions.UncheckedInterruptedException;
import com.sproutigy.commons.async.promises.*;
import com.sproutigy.commons.async.promises.listeners.ProgressListener;
import com.sproutigy.commons.async.promises.listeners.PromiseStateListener;
import com.sproutigy.commons.async.promises.then.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

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

    @Override
    public Promise<V> then(RunnableThrowable onSuccess) {
        return then(onSuccess, null);
    }

    @Override
    public Promise<V> then(RunnableThrowable onSuccess, Handler<Throwable> onFailure) {
        return then(value -> onSuccess.run(), onFailure);
    }

    @Override
    public <OUT> Promise<OUT> then(Callable<OUT> onSuccess) {
        return then(onSuccess, null);
    }

    @Override
    public <OUT> Promise<OUT> then(Callable<OUT> onSuccess, Transformer<Throwable, OUT> onFailure) {
        return then(value -> {
            return onSuccess.call();
        }, null);
    }

    @Override
    public Promise<V> then(Handler<V> onSuccess) {
        return then(onSuccess, null);
    }

    @Override
    public Promise<V> then(Handler<V> onSuccess, Handler<Throwable> onFailure) {
        Transformer<V, V> onSuccessTransform = value -> {
            if (onSuccess != null) {
                onSuccess.handle(value);
            }

            return value;
        };

        Transformer<Throwable, V> onFailureTransform = null;
        if (onFailure != null) {
            onFailureTransform = value -> {
                onFailure.handle(cause);
                return null;
            };
        }

        return then(onSuccessTransform, onFailureTransform);
    }

    @Override
    public <OUT> Promise<OUT> then(Transformer<V, OUT> onSuccess) {
        return then(onSuccess, null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <OUT> Promise<OUT> then(Transformer<V, OUT> onSuccess, Transformer<Throwable, OUT> onFailure) {
        Deferred<OUT> deferred = promiseFactory.defer();

        addStateListener((promise, state) -> {
            if (state.isDone()) {
                getExecutor().execute(() -> {
                    deferred.pending();

                    if (state == PromiseState.Succeeded) {
                        try {
                            V input = promise.getValue();
                            OUT output;
                            if (onSuccess != null) {
                                output = onSuccess.transform(input);
                            }
                            else {
                                try {
                                    output = (OUT) input;
                                } catch (ClassCastException e) {
                                    output = null;
                                }
                            }

                            deferred.success(output);
                        }
                        catch (Throwable e) {
                            deferred.failure(e);
                        }
                    }
                    else {
                        handleThenFailure(promise, deferred, onFailure);
                    }
                });
            }

        });

        return deferred.promise();
    }

    @Override
    public <OUT> Promise<OUT> thenWaitFor(ThenPromise<V, OUT> onSuccess) {
        return thenWaitFor(onSuccess, null);
    }

    @Override
    public <OUT> Promise<OUT> thenWaitFor(ThenPromise<V, OUT> onSuccess, Transformer<Throwable, OUT> onFailure) {
        Deferred<OUT> deferred = promiseFactory.defer();

        addStateListener((promise, state) -> {
            if (state.isDone()) {
                getExecutor().execute(() -> {
                    deferred.pending();

                    if (state == PromiseState.Succeeded) {
                        try {
                            Promise<OUT> otherPromise = onSuccess.execute(promise.getValue());
                            otherPromise.bindTo(deferred);
                        }
                        catch (Throwable e) {
                            if (!deferred.isDone()) {
                                deferred.failure(e);
                            }
                        }
                    }
                    else {
                        handleThenFailure(promise, deferred, onFailure);
                    }
                });
            }
        });

        return deferred.promise();
    }

    @Override
    public <OUT> Promise<OUT> thenWaitFor(ThenBuildPromise<V, OUT> onSuccess) {
        return thenWaitFor(onSuccess, null);
    }

    @Override
    public <OUT> Promise<OUT> thenWaitFor(ThenBuildPromise<V, OUT> onSuccess, Transformer<Throwable, OUT> onFailure) {
        Deferred<OUT> deferred = promiseFactory.defer();

        addStateListener((promise, state) -> {
            if (state.isDone()) {
                getExecutor().execute(() -> {
                    deferred.pending();

                    if (state == PromiseState.Succeeded) {
                        try {
                            onSuccess.execute(promise.getValue(), promiseFactory);
                        }
                        catch (Throwable e) {
                            if (!deferred.isDone()) {
                                deferred.failure(e);
                            }
                        }
                    }
                    else {
                        handleThenFailure(promise, deferred, onFailure);
                    }
                });
            }
        });

        return deferred.promise();
    }

    @Override
    public <OUT> Promise<OUT> thenWaitFor(ThenDefer<V, OUT> onSuccess) {
        return thenWaitFor(onSuccess, null);
    }

    @Override
    public <OUT> Promise<OUT> thenWaitFor(ThenDefer<V, OUT> onSuccess, Transformer<Throwable, OUT> onFailure) {
        Deferred<OUT> deferred = promiseFactory.defer();

        addStateListener((promise, state) -> {
            if (state.isDone()) {
                deferred.pending();

                getExecutor().execute(() -> {
                    if (state == PromiseState.Succeeded) {
                        try {
                            onSuccess.execute(promise.getValue(), deferred);
                        }
                        catch (Throwable e) {
                            if (!deferred.isDone()) {
                                deferred.failure(e);
                            }
                        }
                    }
                    else {
                        handleThenFailure(promise, deferred, onFailure);
                    }
                });
            }
        });

        return deferred.promise();
    }

    @Override
    public Promise<V> thenCatch(Handler<Throwable> onFailure, Class... causeFilters) {
        Deferred<V> deferred = promiseFactory.defer();
        return handleCatch(deferred, (cause) -> {
            onFailure.handle(cause);
            deferred.success(null);
        }, causeFilters);
    }

    @Override
    public <OUT> Promise<OUT> thenCatch(Transformer<Throwable, OUT> onFailure, Class... causeFilters) {
        Deferred<OUT> deferred = promiseFactory.defer();
        return handleCatch(deferred, (cause) -> deferred.success(onFailure.transform(cause)), causeFilters);
    }

    @Override
    public <OUT> Promise<OUT> thenCatchWaitFor(ThenPromise<Throwable, OUT> onFailure, Class... causeFilters) {
        Deferred<OUT> deferred = promiseFactory.defer();
        return handleCatch(deferred, (cause) -> onFailure.execute(cause).bindTo(deferred), causeFilters);
    }

    @Override
    public <OUT> Promise<OUT> thenCatchWaitFor(ThenBuildPromise<Throwable, OUT> onFailure, Class... causeFilters) {
        Deferred<OUT> deferred = promiseFactory.defer();
        return handleCatch(deferred, (cause) -> onFailure.execute(cause, getFactory()).bindTo(deferred), causeFilters);
    }

    @Override
    public <OUT> Promise<OUT> thenCatchWaitFor(ThenDefer<Throwable, OUT> onFailure, Class... causeFilters) {
        Deferred<OUT> deferred = promiseFactory.defer();
        return handleCatch(deferred, (cause) -> onFailure.execute(cause, deferred));
    }

    private <OUT> Promise<OUT> handleCatch(Deferred<OUT> deferred, ThenCatchAction catchAction, Class... causeFilters) {
        addStateListener((promise, state) -> {
            if (state.isDone()) {
                deferred.pending();

                getExecutor().execute(() -> {
                    if (state != PromiseState.Succeeded) {
                        Throwable cause = promise.cause();

                        if (filterThrowable(cause, causeFilters)) {
                            try {
                                catchAction.execute(cause);
                            } catch (Throwable e) {
                                if (!deferred.isDone()) {
                                    deferred.failure(e);
                                }
                            }
                        }
                        else {
                            deferred.failure(cause);
                        }
                    }
                    else {
                        passthroughValue(promise, deferred);
                    }
                });
            }
        });

        return deferred.promise();
    }

    private interface ThenCatchAction {
        void execute(Throwable cause) throws Throwable;
    }


    @Override
    public void thenDone() {
        addStateListener((promise, state) -> {
            if (state.isDone()) {
                getFactory().handlePromiseDone(this);
            }
        });
    }

    private static <OUT> void handleThenFailure(Promise promise, Deferred<OUT> deferred, Transformer<Throwable, OUT> onFailure) {
        Throwable cause = promise.cause();
        if (onFailure != null) {
            try {
                OUT output = onFailure.transform(cause);
                deferred.success(output);
            } catch (Throwable e) {
                deferred.failure(e);
            }
        }
        else {
            deferred.failure(cause);
        }
    }

    private static boolean filterThrowable(Throwable cause, Class... filters) {
        if (filters.length > 0) {
            for (Class causeType : filters) {
                if (isCausedBy(causeType, cause)) {
                    return true;
                }
            }
            return false;
        }
        else {
            return true;
        }
    }

    private <V, OUT> void passthroughValue(Promise<V> promise, Deferred<OUT> defer) {
        V input = promise.getValue();
        OUT output;
        try {
            output = (OUT) input;
        } catch (ClassCastException e) {
            output = null;
        }
        defer.success(output);
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
        if (isDone()) {
            throw new IllegalStateException("Promise already done");
        }

        for (ProgressListener progressListener : progressListeners) {
            try {
                progressListener.onProgress(progress);
            } catch (Throwable error) {
                log.error("Promise [" + getIdentifier() + "] progress listener thrown error", error);
            }
        }
    }

    @Override
    public Promise<V> await() throws InterruptedException {
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

        return this;
    }

    @Override
    public Promise<V> await(long timeout, TimeUnit timeoutUnit) throws InterruptedException, TimeoutException {
        if (isDone()) return this;

        boolean useTimeout = timeout > 0 && timeoutUnit != null;
        long expireTimeMillis = useTimeout ? System.currentTimeMillis() + timeoutUnit.toMillis(timeout) : 0;

        synchronized (this) {
            if (useTimeout) {
                if (isDone()) {
                    return this;
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

        return this;
    }

    @Override
    public Promise<V> awaitUninterruptibly() {
        try {
            await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return this;
    }

    @Override
    public Promise<V> awaitUninterruptibly(long timeout, TimeUnit timeoutUnit) throws TimeoutException {
        try {
            await(timeout, timeoutUnit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return this;
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


    private static boolean isCausedBy(Class check, Throwable thrown) {
        if (thrown.getClass().isAssignableFrom(check)) {
            return true;
        }
        if (thrown.getCause() != null) {
            return isCausedBy(check, thrown.getCause());
        }
        return false;
    }
}
