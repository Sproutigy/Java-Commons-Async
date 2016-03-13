package com.sproutigy.commons.async.promises;

import com.sproutigy.commons.async.Transformer;
import com.sproutigy.commons.async.RunnableThrowable;
import com.sproutigy.commons.async.promises.listeners.PromiseCreationListener;
import org.reactivestreams.Publisher;

import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

/**
 * @author LukeAheadNET
 */
public class PromiseFactoryDecorator extends PromiseFactory {
    private PromiseFactory decorated;

    public PromiseFactoryDecorator(PromiseFactory decorated) {
        this.decorated = decorated;
    }

    @Override
    public String generateIdentifier() {
        return decorated.generateIdentifier();
    }

    @Override
    public void notifyPromiseCreationListeners(Promise promise) {
        decorated.notifyPromiseCreationListeners(promise);
    }

    @Override
    public <V> Deferred<V> defer() {
        return decorated.defer();
    }

    @Override
    public Deferred<Void> deferVoid() {
        return decorated.deferVoid();
    }

    @Override
    public <E> DeferredCollect<E> deferCollect() {
        return decorated.deferCollect();
    }

    @Override
    public Promise<Void> instantVoid() {
        return decorated.instantVoid();
    }

    @Override
    public <V> Promise<V> instantNull() {
        return decorated.instantNull();
    }

    @Override
    public <V> Promise<V> instant(V value) {
        return decorated.instant(value);
    }

    @Override
    public <V> Promise<V> instantFailure() {
        return decorated.instantFailure();
    }

    @Override
    public <V> Promise<V> instantFailure(Throwable cause) {
        return decorated.instantFailure(cause);
    }

    @Override
    public Promise<Void> async(RunnableThrowable runnable) {
        return decorated.async(runnable);
    }

    @Override
    public <V> Promise<V> async(RunnableThrowable runnable, V value) {
        return decorated.async(runnable, value);
    }

    @Override
    public <V> Promise<V> async(Callable<V> callable) {
        return decorated.async(callable);
    }

    @Override
    public <IN, OUT> Promise<OUT> async(Transformer<IN, OUT> processor, IN input) {
        return decorated.async(processor, input);
    }

    @Override
    public <V> Promise<V> from(Future<V> future) {
        return decorated.from(future);
    }

    @Override
    public <E> PromiseCollect<E> from(Publisher<E> publisher) {
        return decorated.from(publisher);
    }

    @Override
    public <E> PromiseCollect<E> instantCollect(E[] array) {
        return decorated.instantCollect(array);
    }

    @Override
    public <E> PromiseCollect<E> instantCollect(Iterator<E> iterator) {
        return decorated.instantCollect(iterator);
    }

    @Override
    public <E> PromiseCollect<E> instantCollect(Iterable<E> iterable) {
        return decorated.instantCollect(iterable);
    }

    @Override
    public <E> PromiseCollect<E> instantCollectFailure() {
        return decorated.instantCollectFailure();
    }

    @Override
    public <E> PromiseCollect<E> instantCollectFailure(Throwable cause) {
        return decorated.instantCollectFailure(cause);
    }

    @Override
    public <E> PromiseCollect<E> instantCollectEmpty() {
        return decorated.instantCollectEmpty();
    }

    @Override
    public Executor getAsyncExecutor() {
        return decorated.getAsyncExecutor();
    }

    @Override
    public void handlePromiseDone(Promise promise) {
        decorated.handlePromiseDone(promise);
    }

    @Override
    public PromiseFactory addCreationListener(PromiseCreationListener promiseCreationListener) {
        decorated.addCreationListener(promiseCreationListener);
        return this;
    }

    @Override
    public PromiseFactory removeCreationListener(PromiseCreationListener promiseCreationListener) {
        decorated.removeCreationListener(promiseCreationListener);
        return this;
    }

    @Override
    public Promise all(Promise... promises) {
        return decorated.all(promises);
    }

    @Override
    public Promise any(Promise... promises) {
        return decorated.any(promises);
    }

    @Override
    public <IN, OUT> PromiseCollect<OUT> forEachSequentially(PromiseProviderByInput<IN, OUT> provider, IN... elements) {
        return decorated.forEachSequentially(provider, elements);
    }

    @Override
    public <V> void bind(Promise<V> sourcePromise, Deferred<V> targetDeferred) {
        decorated.bind(sourcePromise, targetDeferred);
    }

    @Override
    public <E> void bind(PromiseCollect<E> sourcePromise, DeferredCollect<E> targetDeferred) {
        decorated.bind(sourcePromise, targetDeferred);
    }

    @Override
    public <E> void bind(Publisher<E> publisher, DeferredCollect<E> deferred) {
        decorated.bind(publisher, deferred);
    }
}
