package com.sproutigy.commons.async;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public interface Deferred<V> {
    Deferred<V> named(String name);

    Promise<V> resolve();

    Promise<V> resolve(V value);

    Promise<V> reject();

    Promise<V> reject(Object cause);

    Promise<V> execute(Runnable runnable);

    Promise<V> execute(Runnable runnable, V value);

    Promise<V> execute(Callable<V> callable);

    <IN> Promise<V> execute(IN input, Transform<IN, V> transform);

    Promise<V> bindTo(Future<V> future);

    Promise<V> execute(Promise<V> otherPromise);

    Promise<V> getPromise();
}