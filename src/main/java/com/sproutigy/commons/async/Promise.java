package com.sproutigy.commons.async;

import org.slf4j.Logger;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public final class Promise<V> implements Future<V> {
    private static AtomicLong counter = new AtomicLong();

    private static Logger log = OptionalLogger.getLogger(Promise.class);
    private static final String PACKAGE_NAME = Promise.class.getPackage().getName();

    public enum State {
        Pending, Fulfilled, Rejected
    }

    private long num;
    private String name;
    private State state = State.Pending;
    private Object result;

    private List<Handler> handlers = new LinkedList<>();
    private Executor executor = new QueuedExecution(Async.getService());


    private static class Handler {
        Callback onFulfilled;
        Callback onRejected;

        public Handler(Callback onFulfilled, Callback onRejected) {
            this.onFulfilled = onFulfilled;
            this.onRejected = onRejected;
        }
    }

    protected Promise() {
        num = counter.incrementAndGet();
        OptionalLogger.trace(log, "Promise created: {}", this);
    }

    public Promise(final PromiseExecutor<V> promiseExecutor) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    promiseExecutor.run(new Callback<V>() {
                        @Override
                        public void onCallback(V value) throws Exception {
                            bindResultTo(value, Promise.this);
                        }
                    }, new Callback<Throwable>() {
                        @Override
                        public void onCallback(Throwable cause) throws Exception {
                            doReject(cause);
                        }
                    });
                } catch (Throwable cause) {
                    doReject(cause);
                }
            }
        });
    }

    public String getName() {
        return name;
    }

    public Promise<V> setName(String name) {
        this.name = name;
        return this;
    }

    @Override
    public String toString() {
        String defaultString = "Promise #" + num;
        if (name == null) {
            return defaultString;
        }
        return name + " (" + defaultString + ")";
    }

    public State getState() {
        return state;
    }

    public boolean isRejected() {
        awaitUnterruptibly();
        return getState() == State.Rejected;
    }

    public boolean isFulfilled() {
        awaitUnterruptibly();
        return getState() == State.Fulfilled;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return getState() == State.Fulfilled || getState() == State.Rejected;
    }

    @Override
    @SuppressWarnings("unchecked")
    public V get() throws InterruptedException, ExecutionException {
        await();
        try {
            throwCause();
        } catch (Throwable e) {
            throw new ExecutionException(e);
        }
        return (V) result;
    }

    public V get(long timeoutMilliseconds) throws InterruptedException, ExecutionException, TimeoutException {
        return get(timeoutMilliseconds, TimeUnit.MILLISECONDS);
    }

    @Override
    @SuppressWarnings("unchecked")
    public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        await(timeout, unit);
        try {
            throwCause();
        } catch (Throwable e) {
            throw new ExecutionException(e);
        }
        return (V) result;
    }

    @SuppressWarnings("unchecked")
    public V getResult() throws Exception {
        await();

        throwCause();

        return (V) result;
    }

    public V getValue() {
        try {
            return getResult();
        } catch (Error | RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Throwable getCause() {
        awaitUnterruptibly();
        try {
            throwCause();
            return null;
        } catch (Throwable cause) {
            return cause;
        }
    }

    public void throwCause() throws Exception {
        if (getState() == State.Rejected) {
            if (result instanceof Throwable) {
                if (result instanceof Error) {
                    throw (Error) result;
                } else if (result instanceof Exception) {
                    throw (Exception) result;
                } else {
                    throw new RuntimeException((Throwable) result);
                }
            } else {
                throw new PromiseRejectedException(result);
            }
        }
    }

    public V join() {
        return getValue();
    }

    protected Promise<V> doResolve() {
        return doResolve(null);
    }

    protected synchronized Promise<V> doResolve(V value) {
        done(State.Fulfilled, value);
        return this;
    }

    protected synchronized Promise<V> doReject(Object cause) {
        done(State.Rejected, cause);
        return this;
    }

    protected synchronized void done(final State state, final Object result) {
        if (isDone()) {
            OptionalLogger.warn(log, "Change state attempt on promise already completed: {}", this);
            throw new IllegalStateException("Promise already completed, cannot change the state");
        }

        if (state == State.Fulfilled) {
            OptionalLogger.trace(log, "Promise fulfilled: {}", this);
        } else if (state == State.Rejected) {
            OptionalLogger.trace(log, "Promise rejected: {}", this);
        }

        this.state = state;
        this.result = result;

        synchronized (this) {
            this.notifyAll();
        }

        executeHandlers();
    }

    protected synchronized void addHandler(final Handler handler) {
        if (isDone()) {
            executeHandler(handler);
        } else {
            handlers.add(handler);
        }
    }

    private void executeHandlers() {
        for (final Handler handler : handlers) {
            executeHandler(handler);
        }
    }

    private void executeHandler(final Handler handler) {
        if (isDone()) {
            if (getState() == State.Fulfilled && handler.onFulfilled == null) {
                return;
            }
            if (getState() == State.Rejected && handler.onRejected == null) {
                return;
            }
        }

        executor.execute(new Runnable() {
            @Override
            public void run() {
                if (state == State.Fulfilled) {
                    try {
                        onFulfillCall(handler.onFulfilled, Promise.this.result);
                    } catch (Throwable e) {
                        OptionalLogger.error(log, "Promise onFulfill callback {} error", handler.onFulfilled, e);
                    }
                } else {
                    try {
                        onRejectCall(handler.onRejected, Promise.this.result);
                    } catch (Throwable e) {
                        OptionalLogger.error(log, "Promise onReject callback {} error", handler.onRejected, e);
                    }
                }
            }
        });
    }

    public synchronized Promise<V> onFulfill(Callback<V> onFulfilled) {
        addHandler(new Handler(onFulfilled, null));
        return this;
    }

    @SuppressWarnings("unchecked")
    private static void onFulfillCall(Callback onFulfilled, Object value) {
        if (onFulfilled != null) {
            try {
                onFulfilled.onCallback(value);
            } catch (Throwable e) {
                OptionalLogger.error(log, "onFulfill callback error", e);
            }
        }
    }

    public synchronized Promise<V> onReject(Callback<Throwable> onReject) {
        addHandler(new Handler(null, onReject));
        return this;
    }

    @SuppressWarnings("unchecked")
    private static void onRejectCall(Callback onRejected, Object cause) {
        if (onRejected != null) {
            try {
                onRejected.onCallback(cause);
            } catch (Throwable e) {
                OptionalLogger.error(log, "onReject callback error", e);
            }
        }
    }

    public Promise<V> onDone(Callback<V> onFulfill, Callback<Throwable> onReject) {
        addHandler(new Handler(onFulfill,onReject));
        return this;
    }

    @SuppressWarnings("unchecked")
    private static void bindResultTo(final Object o, final Promise promise) {
        if (o instanceof Future) {
            FutureWatch.listen((Future) o, new ExecutionCallback<Object>() {
                @Override
                public void callback(Result result, Object value, Throwable cause) {
                    if (result.isSuccess()) {
                        if (value instanceof Future) {
                            bindResultTo(value, promise);
                        } else {
                            promise.doResolve(value);
                        }
                    } else {
                        promise.doReject(cause);
                    }
                }
            });
        } else {
            promise.doResolve(o);
        }
    }

    @SuppressWarnings("unchecked")
    public <R> Promise<R> then(final Transform<V, R> onFulfilled, final Transform<V, R> onRejected) {
        final Deferred<R> deferred = Promise.defer();

        final Callback<V> onRejectedCallback = new Callback<V>() {
            @Override
            public void onCallback(V argument) throws Exception {
                if (onRejected != null) {
                    try {
                        Object value = onRejected.transform(argument);
                        bindResultTo(value, deferred.getPromise());
                    } catch (Throwable cause) {
                        deferred.reject(cause);
                    }
                } else {
                    deferred.reject(argument);
                }
            }
        };

        final Callback<V> onFulfilledCallback = new Callback<V>() {
            @Override
            public void onCallback(V argument) throws Exception {
                if (argument instanceof Promise) {
                    ((Promise) argument).onFulfill(this);
                    ((Promise) argument).onReject(onRejectedCallback);
                    return;
                }

                if (onFulfilled != null) {
                    try {
                        Object value = onFulfilled.transform(argument);
                        bindResultTo(value, deferred.getPromise());
                    } catch (Throwable cause) {
                        deferred.reject(cause);
                    }
                } else {
                    try {
                        bindResultTo(argument, deferred.getPromise());
                    } catch (ClassCastException ignore) {
                        deferred.resolve(null);
                    }
                }
            }
        };

        addHandler(new Handler(onFulfilledCallback, onRejectedCallback));

        return deferred.getPromise();
    }

    public <R> Promise<R> then(final Transform<V, R> onFulfilled) {
        return then(onFulfilled, null);
    }

    @SuppressWarnings("unchecked")
    private Promise<V> then(final Callback<V> onFulfilled, final Callback onRejected) {
        Transform<V, V> onFulfilledTransform = null;

        if (onFulfilled != null) {
            onFulfilledTransform = new Transform<V, V>() {
                @Override
                public V transform(V argument) throws Exception {
                    onFulfilled.onCallback(argument);
                    return argument;
                }
            };
        }

        Transform<V, V> onRejectedTransform = null;

        if (onRejected != null) {
            onRejectedTransform = new Transform<V, V>() {
                @Override
                public V transform(V argument) throws Exception {
                    onRejected.onCallback(argument);
                    return null;
                }
            };
        }

        return then(onFulfilledTransform, onRejectedTransform);
    }

    @SuppressWarnings("unchecked")
    public <R> Promise<R> thenAwait(Transform<V, Future<R>> onFulfilled) {
        return then((Transform<V, R>) onFulfilled);
    }

    @SuppressWarnings("unchecked")
    public <R> Promise<R> thenAwait(Transform<V, Future<R>> onFulfilled, final Transform<V, R> onRejected) {
        return then((Transform<V, R>) onFulfilled, onRejected);
    }

    private Promise<V> then(Callback<V> onFulfilled) {
        return then(onFulfilled, null);
    }

    //rename to "catch" on postprocessing (as it is Java keyword it has to be done on byte-code level)
    public <R> Promise<R> CATCH(final Transform<Throwable, R> catchHandler) {
        return catchHandle(catchHandler);
    }

    public <R> Promise<R> catchHandle(final Transform<Throwable, R> catchHandler) {
        Deferred<R> deferred = Promise.defer();
        onReject(new Callback<Throwable>() {
            @Override
            public void onCallback(Throwable rejectedValue) throws Exception {
                try {
                    R val = catchHandler.transform(rejectedValue);
                    deferred.resolve(val);
                } catch (Throwable e) {
                    deferred.reject(e);
                }
            }
        });
        return deferred.getPromise();
    }

    public static <T> Deferred<T> defer() {
        final Promise<T> promise = new Promise<>();

        return new Deferred<T>() {
            @Override
            public Deferred<T> named(String name) {
                promise.setName(name);
                return this;
            }

            @Override
            public Promise<T> resolve() {
                return resolve(null);
            }

            @Override
            public Promise<T> resolve(T value) {
                return promise.doResolve(value);
            }

            @Override
            public Promise<T> reject() {
                return reject(null);
            }

            @Override
            public Promise<T> reject(Object cause) {
                return promise.doReject(cause);
            }

            @Override
            public Promise<T> execute(final Runnable runnable) {
                return execute(runnable, (T)null);
            }

            @Override
            public Promise<T> execute(final Runnable runnable, final T value) {
                Async.execute(new Runnable() {
                    @Override
                    public void run() {
                        OptionalLogger.trace(log, "Promise execution: {}", getPromise());
                        try {
                            runnable.run();
                            promise.doResolve(value);
                        } catch (Throwable cause) {
                            promise.doReject(cause);
                        }

                    }
                });

                return promise;
            }

            @Override
            public Promise<T> execute(final Callable<T> callable) {
                Async.execute(new Runnable() {
                    @Override
                    public void run() {
                        OptionalLogger.trace(log, "Promise execution: {}", getPromise());
                        try {
                            T value = callable.call();
                            promise.doResolve(value);
                        } catch (Throwable cause) {
                            promise.doReject(cause);
                        }
                    }
                });

                return promise;
            }

            @Override
            public <IN> Promise<T> execute(IN input, Transform<IN, T> transform) {
                return execute(new Callable<T>() {
                    @Override
                    public T call() throws Exception {
                        return transform.transform(input);
                    }
                });
            }

            @Override
            public Promise<T> bindTo(Future<T> future) {
                FutureWatch.listen(future, new ExecutionCallback<T>() {
                    @Override
                    public void callback(Result result, T value, Throwable cause) {
                        if (result.isSuccess()) {
                            promise.doResolve(value);
                        } else {
                            promise.doReject(cause);
                        }
                    }
                });
                return promise;
            }

            @Override
            public Promise<T> execute(Promise<T> otherPromise) {
                if (otherPromise == promise) {
                    throw new IllegalStateException("Could not bind same promise");
                }

                promise.onDone(new Callback<T>() {
                    @Override
                    public void onCallback(T value) throws Exception {
                        resolve(value);
                    }
                }, new Callback<Throwable>() {
                    @Override
                    public void onCallback(Throwable cause) throws Exception {
                        reject(cause);
                    }
                });

                return promise;
            }

            @Override
            public Promise<T> getPromise() {
                return promise;
            }
        };
    }

    public static <T> Promise<T> reject(Throwable reason) {
        return Promise.<T>defer().reject(reason);
    }

    public static <V> Promise<V> resolve() {
        return resolve(null);
    }

    public static <T> Promise<T> resolve(T value) {
        return Promise.<T>defer().resolve(value);
    }

    @SuppressWarnings("unchecked")
    public static Promise all(Promise... promises) {
        final Deferred deferred = Promise.defer();
        int count = promises.length;

        final AtomicInteger leftCounter = new AtomicInteger(promises.length);

        for (Promise promise : promises) {
            promise.onDone(new Callback() {
                @Override
                public void onCallback(Object argument) throws Exception {
                    if (leftCounter.decrementAndGet() == 0) {
                        deferred.resolve(argument);
                    }

                }
            }, new Callback() {
                @Override
                public void onCallback(Object argument) throws Exception {
                    if (leftCounter.decrementAndGet() == 0) {
                        deferred.reject(argument);
                    }
                }
            });
            count++;
        }

        if (count == 0) {
            deferred.resolve();
        }

        return deferred.getPromise();
    }

    @SuppressWarnings("unchecked")
    public static Promise race(Promise... promises) {
        final Deferred deferred = Promise.defer();
        for (Promise promise : promises) {
            promise.onDone(new Callback() {
                @Override
                public void onCallback(Object argument) throws Exception {
                    synchronized (deferred) {
                        if (!deferred.getPromise().isDone()) {
                            deferred.resolve(argument);
                        }
                    }
                }
            }, new Callback() {
                @Override
                public void onCallback(Object argument) throws Exception {
                    synchronized (deferred) {
                        if (!deferred.getPromise().isDone()) {
                            deferred.reject(argument);
                        }
                    }
                }
            });
        }

        if (promises.length == 0) {
            deferred.resolve();
        }

        return deferred.getPromise();
    }

    public static void await(Promise... promises) throws InterruptedException {
        for (Promise promise : promises) {
            promise.await();
        }
    }

    public Promise<V> await() throws InterruptedException {
        if (!isDone()) {
            synchronized (this) {
                if (!isDone()) {
                    this.wait();
                }
            }
        }
        return this;
    }

    public Promise<V> await(long timeoutMilliseconds) throws InterruptedException, TimeoutException {
        if (!isDone()) {
            synchronized (this) {
                if (!isDone()) {
                    this.wait(timeoutMilliseconds);
                }
            }
            if (!isDone()) {
                throw new TimeoutException("Promise awaiting timeout");
            }
        }
        return this;
    }

    public Promise<V> await(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
        if (!isDone()) {
            synchronized (this) {
                if (!isDone()) {
                    this.wait(unit.toMillis(timeout));
                }
            }
            if (!isDone()) {
                throw new TimeoutException("Promise awaiting timeout");
            }
        }
        return this;
    }

    public Promise<V> awaitUnterruptibly() {
        while (true) {
            try {
                await();
                return this;
            } catch (InterruptedException ignore) {
            }
        }
    }

    public static Promise<?> execute(Runnable runnable) {
        return Promise.defer().execute(runnable);
    }

    public static <V> Promise<V> execute(Runnable runnable, V value) {
        return Promise.<V>defer().execute(runnable, value);
    }

    public static <IN, OUT> Promise<OUT> execute(IN input, Transform<IN, OUT> transform) {
        return Promise.<OUT>defer().execute(input, transform);
    }

    public static <V> Promise<V> execute(Callable<V> callable) {
        return Promise.<V>defer().execute(callable);
    }

    public static <V> Promise<V> from(Future<V> future) {
        if (future instanceof Promise) {
            return (Promise<V>) future;
        }
        return Promise.<V>defer().bindTo(future);
    }
}
