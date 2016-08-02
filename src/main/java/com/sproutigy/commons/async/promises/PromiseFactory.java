package com.sproutigy.commons.async.promises;

import com.sproutigy.commons.async.Async;
import com.sproutigy.commons.async.Transformer;
import com.sproutigy.commons.async.RunnableThrowable;
import com.sproutigy.commons.async.promises.impl.DeferredCollectPromiseImpl;
import com.sproutigy.commons.async.promises.impl.DeferredPromiseImpl;
import com.sproutigy.commons.async.promises.listeners.PromiseCreationListener;
import com.sproutigy.commons.async.promises.listeners.PromiseStateListener;
import com.sproutigy.commons.async.promises.then.ThenBuildPromise;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author LukeAheadNET
 */
public class PromiseFactory {

    public static final PromiseFactory DEFAULT = new PromiseFactory();
    protected static final Logger log = LoggerFactory.getLogger(PromiseFactory.class);
    private static final DateFormat dateFormat = DateFormat.getDateTimeInstance();
    private AtomicLong counter = new AtomicLong(0);
    private Executor asyncExecutor;
    private CopyOnWriteArrayList<PromiseCreationListener> promiseCreationListeners = new CopyOnWriteArrayList<>();

    public PromiseFactory() {
        this(Async.commonExecutor());
    }

    public PromiseFactory(Executor asyncExecutor) {
        this.asyncExecutor = asyncExecutor;
    }

    protected String generateIdentifier() {
        return this.toString() + ":#" + counter.incrementAndGet() + " (" + dateFormat.format(new Date()) + ")";
    }

    protected void notifyPromiseCreationListeners(Promise promise) {
        for (PromiseCreationListener promiseCreationListener : promiseCreationListeners) {
            try {
                promiseCreationListener.onPromiseCreation(promise);
            } catch (Throwable error) {
                log.error("Promise creation listener error", error);
            }
        }
    }

    public <V> Deferred<V> defer() {
        Deferred<V> deferred = new DeferredPromiseImpl<>(this);
        deferred.identify(generateIdentifier());
        if (log.isTraceEnabled()) log.trace("Created deferred promise [" + deferred.getIdentifier() + "]");
        notifyPromiseCreationListeners(deferred.promise());
        return deferred;
    }

    public <V> Deferred<V> deferPending() {
        Deferred<V> deferred = defer();
        deferred.pending();
        return deferred;
    }

    public Deferred<Void> deferVoid() {
        return defer();
    }

    public Deferred<Void> deferVoidPending() {
        return deferPending();
    }

    public <E> DeferredCollect<E> deferCollect() {
        DeferredCollect<E> deferred = new DeferredCollectPromiseImpl<>(this, new LinkedList<>(), true);
        deferred.identify(generateIdentifier());
        if (log.isTraceEnabled()) {
            log.trace("Created collection deferred promise [" + deferred.getIdentifier() + "]");
        }
        notifyPromiseCreationListeners(deferred.promise());
        return deferred;
    }

    public <E> DeferredCollect<E> deferCollectPending() {
        DeferredCollect<E> deferred = deferCollect();
        deferred.pending();
        return deferred;
    }

    public Promise<Void> instantVoid() {
        Deferred<Void> deferred = defer();
        deferred.pending().success();
        return deferred.promise();
    }

    public <V> Promise<V> instantNull() {
        Deferred<V> deferred = defer();
        deferred.pending().success(null);
        return deferred.promise();
    }

    public <V> Promise<V> instant(V value) {
        Deferred<V> deferred = defer();
        deferred.pending().success(value);
        return deferred.promise();
    }

    public <V> Promise<V> instantFailure() {
        Deferred<V> deferred = defer();
        deferred.pending().failure();
        return deferred.promise();
    }

    public <V> Promise<V> instantFailure(Throwable cause) {
        Deferred<V> deferred = defer();
        deferred.pending().failure(cause);
        return deferred.promise();
    }

    public Promise<Void> async(RunnableThrowable runnable) {
        return async(runnable, (Void)null);
    }

    public <V> Promise<V> async(RunnableThrowable runnable, V value) {
        Deferred<V> deferred = defer();
        asyncExecutor.execute(() -> {
            try {
                deferred.pending();
                runnable.run();
                deferred.success(value);
            } catch (Throwable e) {
                deferred.failure(e);
            }
        });
        return deferred.promise();
    }

    public <V> Promise<V> async(Callable<V> callable) {
        Deferred<V> deferred = defer();
        asyncExecutor.execute(() -> {
            try {
                deferred.pending();
                V value = callable.call();
                deferred.success(value);
            } catch (Throwable e) {
                deferred.failure(e);
            }
        });
        return deferred.promise();
    }

    public <IN, OUT> Promise<OUT> async(IN input, Transformer<IN, OUT> transformer) {
        Deferred<OUT> deferred = defer();
        asyncExecutor.execute(() -> {
            try {
                deferred.pending();
                OUT output = transformer.transform(input);
                deferred.success(output);
            } catch (Throwable e) {
                deferred.failure(e);
            }
        });
        return deferred.promise();
    }

    public <V> Promise<V> from(Future<V> future) {
        Deferred<V> deferred = defer();

        Runnable fetcher = new Runnable() {
            long delay = 1;

            @Override
            public void run() {
                try {
                    try {
                        V value = future.get(1, TimeUnit.MICROSECONDS);
                        deferred.success(value);
                    } catch (TimeoutException timeout) {
                        //release thread for other queued executions and retry again later
                        Executor executor = getAsyncExecutor();
                        if (executor instanceof ScheduledExecutorService) {
                            ((ScheduledExecutorService) executor).schedule(this, delay, TimeUnit.MICROSECONDS);
                        } else {
                            final Runnable thatRunnable = this;
                            Async.commonScheduler().schedule(() -> executor.execute(thatRunnable), delay, TimeUnit.MICROSECONDS);
                        }
                        delay *= 2;
                    }
                } catch (Throwable e) {
                    deferred.failure(e);
                }
            }
        };

        getAsyncExecutor().execute(() -> {
            deferred.pending();
            fetcher.run();
        });

        return deferred.promise();
    }

    public <E> PromiseCollect<E> from(Publisher<E> publisher) {
        DeferredCollect<E> deferred = deferCollect();

        publisher.subscribe(new Subscriber<E>() {
            Subscription subscription;

            @Override
            public void onSubscribe(Subscription subscription) {
                deferred.pending();
                this.subscription = subscription;
            }

            @Override
            public void onNext(E element) {
                deferred.next(element);
            }

            @Override
            public void onError(Throwable cause) {
                deferred.failure(cause);
            }

            @Override
            public void onComplete() {
                deferred.complete();
            }
        });

        return deferred.promise();
    }

    public <V> Promise<V> from(CompletableFuture<V> completableFuture) {
        Deferred<V> deferred = defer();
        deferred.pending();
        completableFuture.whenComplete((value, throwable) -> {
            if (throwable != null) {
                deferred.failure(throwable);
            } else {
                deferred.success(value);
            }
        });
        return deferred.promise();
    }

    public <V> Promise<V> promise(Callable<Promise<V>> callable) {
        try {
            return callable.call();
        } catch (Throwable e) {
            Deferred<V> deferred = defer();
            deferred.failure(e);
            return deferred.promise();
        }
    }


    public <E> PromiseCollect<E> instantCollect(E[] array) {
        DeferredCollect<E> deferred = deferCollect();
        deferred.pending();
        for (E element : array) {
            deferred.next(element);
        }
        deferred.complete();
        return deferred.promise();
    }

    public <E> PromiseCollect<E> instantCollect(Iterator<E> iterator) {
        DeferredCollect<E> deferred = deferCollect();
        deferred.pending();
        try {
            while (iterator.hasNext()) {
                deferred.next(iterator.next());
            }
            deferred.complete();
        } catch (Throwable e) {
            deferred.failure(e);
        }
        return deferred.promise();
    }

    public <E> PromiseCollect<E> instantCollect(Iterable<E> iterable) {
        DeferredCollect<E> deferred = deferCollect();
        deferred.pending();
        try {
            Iterator<E> iterator = iterable.iterator();
            while (iterator.hasNext()) {
                deferred.next(iterator.next());
            }
            deferred.complete();
        } catch (Throwable e) {
            deferred.failure(e);
        }
        return deferred.promise();
    }

    public <E> PromiseCollect<E> instantCollectFailure() {
        DeferredCollect<E> deferred = deferCollect();
        deferred.pending().failure();
        return deferred.promise();
    }

    public <E> PromiseCollect<E> instantCollectFailure(Throwable cause) {
        DeferredCollect<E> deferred = deferCollect();
        deferred.pending().failure(cause);
        return deferred.promise();
    }

    public <E> PromiseCollect<E> instantCollectEmpty() {
        DeferredCollect<E> deferred = deferCollect();
        return deferred.pending().complete().promise();
    }


    public Executor getAsyncExecutor() {
        return asyncExecutor;
    }

    public void handlePromiseDone(Promise promise) {
        if (promise.isFailure()) {
            log.error("Unhandled error on promise [" + promise.getIdentifier() + "]", promise.cause());
        }
    }

    public PromiseFactory addCreationListener(PromiseCreationListener promiseCreationListener) {
        promiseCreationListeners.add(promiseCreationListener);
        return this;
    }

    public PromiseFactory removeCreationListener(PromiseCreationListener promiseCreationListener) {
        promiseCreationListeners.remove(promiseCreationListener);
        return this;
    }


    public Promise all(Promise... promises) {
        Deferred deferred = defer();
        deferred.pending();

        int count = 0;
        for (Promise promise : promises) {
            if (promise != null) {
                count++;
            }
        }

        AtomicInteger counter = new AtomicInteger(count);

        final PromiseStateListener listener = new PromiseStateListener() {
            @Override
            public void onPromiseStateChanged(Promise promise, PromiseState state) {
                if (!state.isDone()) return;

                if (state == PromiseState.Succeeded) {
                    if (counter.decrementAndGet() == 0) {
                        try {
                            deferred.success();
                        } catch (IllegalStateException ignore) {
                        }
                    }
                } else {
                    try {
                        deferred.failure(promise.cause());
                    } catch (IllegalStateException ignore) {
                    }

                    for (Promise that : promises) {
                        that.removeStateListener(this);
                    }
                }
            }
        };

        for (Promise promise : promises) {
            if (promise != null) {
                promise.addStateListener(listener);
            }
        }

        return deferred.promise();
    }


    public Promise any(Promise... promises) {
        Deferred deferred = defer();
        deferred.pending();

        final PromiseStateListener listener = new PromiseStateListener() {
            @Override
            public void onPromiseStateChanged(Promise promise, PromiseState state) {
                if (!state.isDone()) return;

                if (state == PromiseState.Succeeded) {
                    try {
                        deferred.success();
                    } catch (IllegalStateException ignore) {
                    }
                } else {
                    try {
                        deferred.failure(promise.cause());
                    } catch (IllegalStateException ignore) {
                    }
                }

                for (Promise that : promises) {
                    that.removeStateListener(this);
                }
            }
        };

        for (Promise promise : promises) {
            if (promise != null) {
                promise.addStateListener(listener);
            }
        }

        return deferred.promise();
    }


    public <IN, OUT> PromiseCollect<OUT> forEachSequentially(ThenBuildPromise<IN, OUT> provider, IN... elements) {
        DeferredCollect<OUT> deferred = deferCollect();

        if (elements.length == 0) {
            deferred.complete();
        } else {
            RunnableThrowable runnable = new RunnableThrowable() {
                volatile int index = 0;

                @Override
                public void run() {
                    deferred.pending();

                    if (index < elements.length) {
                        IN element = elements[index];
                        index++;
                        try {
                            provider.execute(element, PromiseFactory.this)
                                    .onFailureBlocking(deferred::failure)
                                    .onSuccessBlocking(output -> {
                                        deferred.next(output);
                                        PromiseFactory.this.async(this);
                                    });
                        } catch (Throwable cause) {
                            deferred.failure(cause);
                        }
                    } else {
                        deferred.complete();
                    }
                }
            };

            async(runnable);
        }

        return deferred.promise();
    }

    public <V> void bind(Promise<V> sourcePromise, Deferred<V> targetDeferred) {
        if (sourcePromise.isDone()) {
            if (sourcePromise.isSuccess()) {
                targetDeferred.success(sourcePromise.getValue());
            } else {
                targetDeferred.failure(sourcePromise.cause());
            }
            return;
        }

        sourcePromise.addStateListener((promise, state) -> {
            if (state == PromiseState.Pending) {
                targetDeferred.pending();
            } else if (state == PromiseState.Succeeded) {
                targetDeferred.success(sourcePromise.getValue());
            } else if (state == PromiseState.Failed || state == PromiseState.Cancelled) {
                targetDeferred.failure(sourcePromise.cause());
            }
        });
    }

    public <E> void bind(PromiseCollect<E> sourcePromise, DeferredCollect<E> targetDeferred) {
        sourcePromise.addElementListener(targetDeferred::next);
        sourcePromise.addStateListener((promise, state) -> {
            if (state == PromiseState.Pending) {
                targetDeferred.pending();
            }
            if (state == PromiseState.Succeeded) {
                targetDeferred.complete();
            }
            if (state == PromiseState.Failed) {
                targetDeferred.failure(promise.cause());
            }
        });
    }

    public <E> void bind(Publisher<E> publisher, DeferredCollect<E> deferred) {
        publisher.subscribe(new Subscriber<E>() {

            private Subscription subscription;

            @Override
            public void onSubscribe(final Subscription subscription) {
                this.subscription = subscription;
                subscription.request(1);

                deferred.cancellationHandler(mayInterruptIfRunning -> {
                    boolean cancelled = false;
                    try {
                        subscription.cancel();
                        cancelled = true;
                    } catch (Throwable e) {
                        log.warn("Error while trying to cancel reactive stream", e);
                    }
                    return cancelled;
                });
            }

            @Override
            public void onNext(E element) {
                deferred.next(element);
                subscription.request(1);
            }

            @Override
            public void onError(Throwable throwable) {
                deferred.failure(throwable);
            }

            @Override
            public void onComplete() {
                deferred.complete();
            }
        });
    }


    public String toString() {
        if (this == DEFAULT) {
            return "PromiseFactory.DEFAULT";
        } else {
            return super.toString();
        }
    }
}
