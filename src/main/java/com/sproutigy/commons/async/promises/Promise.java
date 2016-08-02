package com.sproutigy.commons.async.promises;

import com.sproutigy.commons.async.Handler;
import com.sproutigy.commons.async.RunnableThrowable;
import com.sproutigy.commons.async.Transformer;
import com.sproutigy.commons.async.promises.listeners.*;
import com.sproutigy.commons.async.promises.then.*;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author LukeAheadNET
 */
public interface Promise<V> extends Future<V> {
    String getIdentifier();

    Promise<V> setIdentifier(String identifier);

    PromiseFactory getFactory();

    boolean isSuccess();

    boolean isFailure();

    Promise<V> await() throws InterruptedException;

    Promise<V> await(long timeout, TimeUnit timeoutUnit) throws InterruptedException, TimeoutException;

    Promise<V> awaitUninterruptibly();

    Promise<V> awaitUninterruptibly(long timeout, TimeUnit timeoutUnit) throws TimeoutException;

    V sync() throws Exception;

    V sync(long timeout, TimeUnit timeoutUnit) throws Exception;

    V getValue();

    V getNow();

    Throwable cause();

    void throwIfFailed() throws Exception;

    PromiseState state();

    Promise<V> then(RunnableThrowable onSuccess);
    Promise<V> then(RunnableThrowable onSuccess, Handler<Throwable> onFailure);

    <OUT> Promise<OUT> then(Callable<OUT> onSuccess);
    <OUT> Promise<OUT> then(Callable<OUT> onSuccess, Transformer<Throwable, OUT> onFailure);

    Promise<V> then(Handler<V> onSuccess);
    Promise<V> then(Handler<V> onSuccess, Handler<Throwable> onFailure);

    <OUT> Promise<OUT> then(Transformer<V, OUT> onSuccess);
    <OUT> Promise<OUT> then(Transformer<V, OUT> onSuccess, Transformer<Throwable, OUT> onFailure);

    <OUT> Promise<OUT> thenWaitFor(ThenPromise<V, OUT> onSuccess);
    <OUT> Promise<OUT> thenWaitFor(ThenPromise<V, OUT> onSuccess, Transformer<Throwable, OUT> onFailure);

    <OUT> Promise<OUT> thenWaitFor(ThenBuildPromise<V, OUT> onSuccess);
    <OUT> Promise<OUT> thenWaitFor(ThenBuildPromise<V, OUT> onSuccess, Transformer<Throwable, OUT> onFailure);

    <OUT> Promise<OUT> thenWaitFor(ThenDefer<V, OUT> onSuccess);
    <OUT> Promise<OUT> thenWaitFor(ThenDefer<V, OUT> onSuccess, Transformer<Throwable, OUT> onFailure);

    Promise<V> thenCatch(Handler<Throwable> onFailure, Class... causeFilters);
    <OUT> Promise<OUT> thenCatch(Transformer<Throwable, OUT> onFailure, Class... causeFilters);

    <OUT> Promise<OUT> thenCatchWaitFor(ThenPromise<Throwable, OUT> onFailure, Class... causeFilters);
    <OUT> Promise<OUT> thenCatchWaitFor(ThenBuildPromise<Throwable, OUT> onFailure, Class... causeFilters);
    <OUT> Promise<OUT> thenCatchWaitFor(ThenDefer<Throwable, OUT> onFailure, Class... causeFilters);

    void thenDone();

    Promise<V> bindTo(Deferred<V> deferred);

    Promise<V> bindTo(DeferredCollect<V> deferred);

    Promise<V> addStateListener(PromiseStateListener<V> stateListener);

    Promise<V> removeStateListener(PromiseStateListener<V> stateListener);

    Promise<V> progress(ProgressListener onProgress);

    Promise<V> removeProgressListener(ProgressListener onProgress);

    Promise<V> onSuccessAsync(SuccessListener<V> onSuccess);

    Promise<V> onSuccessBlocking(SuccessListener<V> onSuccess);

    Promise<V> onFailureAsync(FailureListener onFailure);

    Promise<V> onFailureBlocking(FailureListener onFailure);

    Promise<V> onDoneAsync(DoneListener<V> onDone);

    Promise<V> onDoneBlocking(DoneListener<V> onDone);

}
