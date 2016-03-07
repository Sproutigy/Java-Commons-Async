package com.sproutigy.commons.async.promises;

import com.sproutigy.commons.async.Transformer;
import com.sproutigy.commons.async.ThrowableRunnable;
import com.sproutigy.commons.async.promises.listeners.*;

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

    void await() throws InterruptedException;

    void await(long timeout, TimeUnit timeoutUnit) throws InterruptedException, TimeoutException;

    void awaitUninterruptibly();

    void awaitUninterruptibly(long timeout, TimeUnit timeoutUnit) throws TimeoutException;

    V sync() throws Exception;

    V sync(long timeout, TimeUnit timeoutUnit) throws Exception;

    V getValue();

    V getNow();

    Throwable cause();

    void throwIfFailed() throws Exception;

    PromiseState state();

    <OUT> Promise<OUT> then(Transformer<V, OUT> onSuccess);

    <OUT> Promise<OUT> then(Transformer<V, OUT> onSuccess, Transformer<Throwable, OUT> onFailure);

    <OUT> Promise<OUT> then(Transformer<V, OUT> onSuccess, Transformer<Throwable, OUT> onFailure, ProgressListener onProgress);

    <OUT> Promise<OUT> thenFactorize(PromiseFactory promiseFactory, Transformer<V, OUT> onSuccess);

    <OUT> Promise<OUT> thenFactorize(PromiseFactory promiseFactory, Transformer<V, OUT> onSuccess, Transformer<Throwable, OUT> onFailure);

    <OUT> Promise<OUT> thenFactorize(PromiseFactory promiseFactory, Transformer<V, OUT> onSuccess, Transformer<Throwable, OUT> onFailure, ProgressListener onProgress);

    <OUT> Promise<OUT> thenBlocking(Transformer<V, OUT> onSuccess);

    <OUT> Promise<OUT> thenBlocking(Transformer<V, OUT> onSuccess, Transformer<Throwable, OUT> onFailure);

    <OUT> Promise<OUT> thenPromised(PromiseProviderByInput<V, OUT> onSuccess);

    <OUT> Promise<OUT> thenPromised(PromiseProviderByInput<V, OUT> onSuccess, PromiseProviderByInput<Throwable, OUT> onFailure);

    Promise<V> bindTo(Deferred<V> deferred);

    Promise<V> bindTo(DeferredCollect<V> deferred);

    Promise<V> catchFail(Transformer<Throwable, V> onFailure);

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

    Promise<V> fin(ThrowableRunnable runnable);

    void done();
}
