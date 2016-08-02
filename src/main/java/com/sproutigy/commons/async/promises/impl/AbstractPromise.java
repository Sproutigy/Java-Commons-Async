package com.sproutigy.commons.async.promises.impl;

import com.sproutigy.commons.async.promises.*;
import com.sproutigy.commons.async.promises.listeners.DoneListener;
import com.sproutigy.commons.async.promises.listeners.FailureListener;
import com.sproutigy.commons.async.promises.listeners.SuccessListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * @author LukeAheadNET
 */
public abstract class AbstractPromise<V> implements Promise<V> {

    private final static Logger log = LoggerFactory.getLogger(AbstractPromise.class);

    private volatile String identifier = null;

    @Override
    public String getIdentifier() {
        String s;
        if (identifier == null) {
            s = this.toString();
            String packageName = this.getClass().getPackage().getName();
            if (s.startsWith(packageName + ".")) {
                s = s.substring(packageName.length() + 1);
            }
        } else {
            s = this.identifier;
        }
        return s;
    }

    public synchronized Promise<V> setIdentifier(String identifier) {
        if (this.identifier != null && !Objects.equals(identifier, this.identifier)) {
            if (log.isTraceEnabled())
                log.trace("Changed promise identifier from: [" + getIdentifier() + "] to: [" + identifier + "]");
        }
        this.identifier = identifier;
        return this;
    }

    @Override
    public V sync() throws Exception {
        await();
        if (isSuccess()) {
            return getValue();
        } else {
            throwIfFailed();
            return null;
        }
    }

    @Override
    public V sync(long timeout, TimeUnit timeoutUnit) throws Exception {
        await(timeout, timeoutUnit);
        if (isSuccess()) {
            return getValue();
        } else {
            throwIfFailed();
            return null;
        }
    }

    @Override
    public void throwIfFailed() throws Exception {
        await();
        if (isFailure()) {
            Throwable cause = cause();
            if (cause instanceof Exception) {
                throw (Exception) cause;
            }
            throw new RuntimeException(cause);
        }
    }

    @Override
    public Promise<V> bindTo(Deferred<V> deferred) {
        getFactory().bind(this, deferred);
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Promise<V> bindTo(DeferredCollect<V> deferred) {
        if (this instanceof PromiseCollect) {
            getFactory().bind((PromiseCollect) this, deferred);
        } else {
            onSuccessBlocking(v -> deferred.complete());
            onFailureBlocking(deferred::failure);
        }
        return this;
    }

    @Override
    public Promise<V> onSuccessAsync(SuccessListener<V> onSuccess) {
        addStateListener((promise, state) -> {
            if (state.isDone() && state == PromiseState.Succeeded) {
                getExecutor().execute(() -> {
                    try {
                        onSuccess.onSuccess(promise.getValue());
                    } catch (Throwable e) {
                        log.error("Error thrown while calling success listener", e);
                    }
                });
            }
        });
        return this;
    }

    @Override
    public Promise<V> onSuccessBlocking(SuccessListener<V> onSuccess) {
        addStateListener((promise, state) -> {
            if (state.isDone() && state == PromiseState.Succeeded) {
                try {
                    onSuccess.onSuccess(promise.getValue());
                } catch (Throwable e) {
                    log.error("Error thrown while calling success listener", e);
                }
            }
        });
        return this;
    }

    @Override
    public Promise<V> onFailureAsync(FailureListener onFailure) {
        addStateListener((promise, state) -> {
            if (state.isDone() && state == PromiseState.Failed) {
                getExecutor().execute(() -> {
                    try {
                        onFailure.onFailure(promise.cause());
                    } catch (Throwable e) {
                        log.error("Error thrown while calling failure listener", e);
                    }
                });
            }
        });

        return this;
    }

    @Override
    public Promise<V> onFailureBlocking(FailureListener onFailure) {
        addStateListener((promise, state) -> {
            if ((state.isDone() && state == PromiseState.Failed)) {
                try {
                    onFailure.onFailure(promise.cause());
                } catch (Throwable e) {
                    log.error("Error thrown while calling failure listener", e);
                }
            }
        });
        return this;
    }

    @Override
    public Promise<V> onDoneAsync(DoneListener<V> onDone) {
        addStateListener((promise, state) -> {
            if (state.isDone()) {
                getExecutor().execute(() -> {
                    try {
                        if (state == PromiseState.Succeeded) {
                            onDone.onDone(promise.getValue(), null);
                        } else {
                            onDone.onDone(null, promise.cause());
                        }
                    } catch (Throwable e) {
                        log.error("Error thrown while calling done listener", e);
                    }
                });
            }
        });
        return this;
    }

    @Override
    public Promise<V> onDoneBlocking(DoneListener<V> onDone) {
        addStateListener((promise, state) -> {
            if (state.isDone()) {
                try {
                    if (state == PromiseState.Succeeded) {
                        onDone.onDone(promise.getValue(), null);
                    } else {
                        onDone.onDone(null, promise.cause());
                    }
                } catch (Throwable e) {
                    log.error("Error thrown while calling done listener", e);
                }
            }
        });
        return this;
    }

    protected Executor getExecutor() {
        return getFactory().getAsyncExecutor();
    }
}
