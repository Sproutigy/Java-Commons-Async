package com.sproutigy.commons.async.promises;

/**
 * @author LukeAheadNET
 */
public interface DeferredCollect<E> {

    String getIdentifier();

    DeferredCollect<E> identify(String identifier);

    DeferredCollect<E> cancellationHandler(CancellationHandler cancellationHandler);

    PromiseCollect<E> promise();

    PromiseState state();

    DeferredCollect<E> pending();

    DeferredCollect<E> progress(Object progress);

    DeferredCollect<E> next(E element);

    DeferredCollect<E> complete();

    DeferredCollect<E> failure();

    DeferredCollect<E> failure(Throwable cause);

}
