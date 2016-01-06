package com.sproutigy.commons.async.promises;

/**
 * @author LukeAheadNET
 */
public interface Deferred<V> {
    Promise<V> promise();

    String getIdentifier();

    Deferred<V> identify(String identifier);

    Deferred<V> cancellationHandler(CancellationHandler cancellationHandler);

    PromiseState state();

    Deferred<V> pending();

    Deferred<V> progress(Object progress);

    Deferred<V> success();

    Deferred<V> success(V value);

    Deferred<V> failure();

    Deferred<V> failure(Throwable cause);

    boolean isDone();
}
