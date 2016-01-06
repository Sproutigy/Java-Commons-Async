package com.sproutigy.commons.async.promises.adapters;

import com.google.common.util.concurrent.ListenableFuture;
import com.sproutigy.commons.async.Sync;
import com.sproutigy.commons.async.promises.Deferred;
import com.sproutigy.commons.async.promises.Promise;
import com.sproutigy.commons.async.promises.PromiseFactory;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author LukeAheadNET
 */
public class GuavaAdapter {

    public <V> Promise<V> from(ListenableFuture<V> listenableFuture) {
        return from(listenableFuture, PromiseFactory.DEFAULT);
    }

    public <V> Promise<V> from(ListenableFuture<V> listenableFuture, PromiseFactory promiseFactory) {
        Deferred<V> deferred = promiseFactory.defer();
        deferred.pending();
        listenableFuture.addListener(() -> {
            try {
                V value = listenableFuture.get();
                deferred.success(value);
            } catch (InterruptedException e) {
                deferred.failure(e);
            } catch (ExecutionException e) {
                deferred.failure(e.getCause());
            }
        }, Sync.EXECUTOR);
        return deferred.promise();
    }

    public <V> ListenableFuture<V> to(Promise<V> promise) {
        return new ListenableFuture<V>() {
            @Override
            public void addListener(Runnable runnable, Executor executor) {
                promise.addStateListener((thisPromise, state) -> {
                    if (thisPromise.isDone()) {
                        executor.execute(runnable);
                    }
                });
            }

            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                return promise.cancel(mayInterruptIfRunning);
            }

            @Override
            public boolean isCancelled() {
                return promise.isCancelled();
            }

            @Override
            public boolean isDone() {
                return promise.isDone();
            }

            @Override
            public V get() throws InterruptedException, ExecutionException {
                return promise.get();
            }

            @Override
            public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                return promise.get(timeout, unit);
            }
        };
    }
}
