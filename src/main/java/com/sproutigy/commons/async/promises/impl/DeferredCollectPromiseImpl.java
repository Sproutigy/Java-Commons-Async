package com.sproutigy.commons.async.promises.impl;

import com.sproutigy.commons.async.Close;
import com.sproutigy.commons.async.Transformer;
import com.sproutigy.commons.async.collections.BlockingIterator;
import com.sproutigy.commons.async.promises.*;
import com.sproutigy.commons.async.promises.listeners.ElementListener;
import com.sproutigy.commons.async.promises.listeners.PromiseStateListener;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author LukeAheadNET
 */
public class DeferredCollectPromiseImpl<E> extends AbstractDeferredPromise<Collection<E>> implements DeferredCollect<E>, PromiseCollect<E> {

    private final static Logger log = LoggerFactory.getLogger(DeferredCollectPromiseImpl.class);
    private final Collection<E> collection;
    protected CopyOnWriteArrayList<ElementListener<E>> listeners = new CopyOnWriteArrayList<>();
    private boolean synchronize;

    public DeferredCollectPromiseImpl(PromiseFactory promiseFactory, Collection<E> collection, boolean synchronize) {
        super(promiseFactory);
        this.collection = collection;
        this.synchronize = synchronize;
    }

    @Override
    public DeferredCollect<E> pending() {
        if (state == PromiseState.Waiting) {
            changeState(PromiseState.Pending);
        }
        return this;
    }

    @Override
    public DeferredCollect<E> progress(Object progress) {
        notifyProgress(progress);
        return this;
    }

    @Override
    public DeferredCollect<E> next(E element) {
        if (synchronize) {
            synchronized (collection) {
                nextInternal(element);
            }
        } else {
            nextInternal(element);
        }
        return this;
    }

    private void nextInternal(E element) {
        collection.add(element);
        for (ElementListener<E> elementListener : listeners) {
            notifyElementListener(elementListener, element);
        }
    }

    private void notifyElementListener(ElementListener<E> elementListener, E element) {
        try {
            elementListener.onNext(element);
        } catch (Throwable throwable) {
            log.error("Promise [" + getIdentifier() + "] element listener thrown error", throwable);
        }
    }

    @Override
    public DeferredCollect<E> complete() {
        this.value = Collections.unmodifiableCollection(collection);
        changeState(PromiseState.Succeeded);
        return this;
    }

    @Override
    public DeferredCollect<E> failure() {
        return failure(new Exception("Promise has been rejected"));
    }

    @Override
    public synchronized DeferredCollect<E> failure(Throwable cause) {
        if (cause == null) throw new NullPointerException("Cause cannot be null");
        this.cause = cause;
        changeState(PromiseState.Failed);
        return this;
    }

    @Override
    public DeferredCollect<E> identify(String identifier) {
        setIdentifier(identifier);
        return this;
    }

    @Override
    public DeferredCollect<E> cancellationHandler(CancellationHandler cancellationHandler) {
        this.cancellationHandler = cancellationHandler;
        return this;
    }

    @Override
    public PromiseCollect<E> promise() {
        return this;
    }

    @Override
    public void subscribe(Subscriber<? super E> subscriber) {
        AtomicLong pendingCounter = new AtomicLong(0);
        AtomicLong requestedCounter = new AtomicLong(0);
        AtomicLong notifiedCounter = new AtomicLong(0);
        AtomicBoolean cancelled = new AtomicBoolean(false);
        AtomicBoolean processingQueue = new AtomicBoolean(false);
        Queue<E> queue = new LinkedList<>();

        Subscription subscription = new Subscription() {
            @Override
            public void request(long n) {
                //TODO: better huge long handling

                if (n == Long.MAX_VALUE) {
                    requestedCounter.set(Long.MAX_VALUE);
                } else {
                    if (n > 0) {
                        requestedCounter.addAndGet(n);
                    }
                }

                if (!processingQueue.get()) {
                    getFactory().getAsyncExecutor().execute(() -> {
                        if (cancelled.get() || processingQueue.get()) return;

                        synchronized (queue) {
                            processingQueue.set(true);

                            while (!queue.isEmpty() && requestedCounter.get() > notifiedCounter.get()) {
                                if (cancelled.get()) return;
                                E element = queue.poll();
                                subscriber.onNext(element);
                                notifiedCounter.incrementAndGet();
                            }

                            if (pendingCounter.get() == 0 && state().isDone()) {
                                if (state() == PromiseState.Succeeded) {
                                    subscriber.onComplete();
                                } else {
                                    subscriber.onError(cause());
                                }
                            }

                            processingQueue.set(false);
                        }
                    });
                }
            }

            @Override
            public void cancel() {
                cancelled.set(true);
                queue.clear();
            }
        };

        subscriber.onSubscribe(subscription);

        addElementListener(e -> {
            pendingCounter.incrementAndGet();
            synchronized (queue) {
                if (queue.isEmpty() && requestedCounter.get() > notifiedCounter.get()) {
                    subscriber.onNext(e);
                } else {
                    queue.add(e);
                }
                pendingCounter.decrementAndGet();
            }
        });

        addStateListener((promise, state) -> {
            if (queue.isEmpty() && pendingCounter.get() == 0 && state().isDone()) {
                if (state() == PromiseState.Succeeded) {
                    subscriber.onComplete();
                } else {
                    subscriber.onError(promise.cause());
                }
            }
        });
    }

    @Override
    public PromiseCollect<E> addElementListener(ElementListener<E> elementListener) {
        synchronized (collection) {
            if (!collection.isEmpty()) {
                for (E element : collection) {
                    notifyElementListener(elementListener, element);
                }
            }
            if (!isDone()) {
                listeners.add(elementListener);
            }
        }

        return this;
    }

    @Override
    public PromiseCollect<E> removeElementListener(ElementListener<E> elementListener) {
        listeners.remove(elementListener);
        return this;
    }

    @Override
    public PromiseCollect<E> onEveryBlocking(ElementListener<E> elementListener) {
        return addElementListener(elementListener);
    }

    @Override
    public PromiseCollect<E> onEveryAsync(ElementListener<E> elementListener) {
        return addElementListener(element -> {
            getFactory().getAsyncExecutor().execute(() -> {
                notifyElementListener(elementListener, element);
            });
        });
    }

    @Override
    public <OE> PromiseCollect<OE> mapBlocking(Transformer<E, OE> mapper) {
        return deferCollectionMapper(false, mapper).promise();
    }

    @Override
    public <OE> PromiseCollect<OE> mapAsync(Transformer<E, OE> mapper) {
        //TODO: currently map async may change order of items! try to avoid it
        return deferCollectionMapper(true, mapper).promise();
    }

    @Override
    public int count() throws Exception {
        sync();
        return collection.size();
    }

    @Override
    public E first() throws Exception {
        if (isDone()) {
            throwIfFailed();
            if (collection instanceof List) {
                if (collection.size() == 0) {
                    return null;
                } else {
                    return ((List<E>) collection).get(0);
                }
            }
        }
        Iterator<E> iterator = iterator();
        try {
            if (iterator.hasNext()) {
                return iterator.next();
            }
        } finally {
            if (iterator instanceof Closeable) {
                Close.silently((Closeable) iterator);
            }
        }
        return null;
    }

    @Override
    public E last() throws Exception {
        if (isDone()) {
            throwIfFailed();
            if (collection instanceof List) {
                if (collection.size() == 0) {
                    return null;
                } else {
                    return ((List<E>) collection).get(collection.size() - 1);
                }
            }
        }
        Iterator<E> iterator = iterator();
        try {
            E element = null;
            while (iterator.hasNext()) {
                element = iterator.next();
            }
            return element;
        } finally {
            if (iterator instanceof Closeable) {
                Close.silently((Closeable) iterator);
            }
        }
    }

    private <OE> DeferredCollect<OE> deferCollectionMapper(boolean async, Transformer<E, OE> mapper) {
        DeferredCollect<OE> deferred = getFactory().deferCollect();

        PromiseStateListener<Collection<E>> stateListener = new PromiseStateListener<Collection<E>>() {
            @Override
            public void onPromiseStateChanged(Promise<Collection<E>> promise, PromiseState state) {
                if (state == PromiseState.Pending) {
                    deferred.pending();
                } else if (state == PromiseState.Succeeded) {
                    deferred.complete();
                } else if (state == PromiseState.Failed) {
                    deferred.failure(promise.cause());
                }
            }
        };

        addStateListener(stateListener);

        ElementListener<E> elementListener = new ElementListener<E>() {
            @Override
            public void onNext(E element) {
                try {
                    if (!deferred.promise().state().isDone()) {
                        deferred.next(mapper.transform(element));
                    }
                } catch (Throwable throwable) {
                    deferred.failure(throwable);
                }
            }
        };

        if (async) {
            onEveryAsync(elementListener);
        } else {
            onEveryBlocking(elementListener);
        }

        return deferred;
    }

    @Override
    public Iterator<E> iterator() {
        if (isDone()) {
            return collection.iterator();
        } else {
            return new BlockingIterator<>(this);
        }
    }
}
