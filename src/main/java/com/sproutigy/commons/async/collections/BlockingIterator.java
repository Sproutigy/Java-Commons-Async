package com.sproutigy.commons.async.collections;

import com.sproutigy.commons.async.exceptions.ReactiveUncheckedException;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Queue;

/**
 * @author LukeAheadNET
 */
public class BlockingIterator<T> implements Iterator<T>, CloseableIterator<T> {

    private static Logger log = LoggerFactory.getLogger(BlockingIterator.class);

    private Queue<T> queue = new LinkedList<>();
    private volatile boolean success = false;
    private volatile boolean error = false;
    private volatile Throwable cause = null;
    private volatile boolean closed = false;

    private Subscription subscription;

    public BlockingIterator(Publisher<T> publisher) {
        if (log.isTraceEnabled()) log.trace(getIdentifier() + " subscribing...");

        publisher.subscribe(new Subscriber<T>() {
            @Override
            public void onSubscribe(Subscription subscription) {
                if (log.isTraceEnabled()) log.trace(getIdentifier() + " subscribed");

                BlockingIterator.this.subscription = subscription;

                synchronized (BlockingIterator.this) {
                    notifyWaitingMonitors();
                }
            }

            @Override
            public void onNext(T t) {
                if (log.isTraceEnabled()) log.trace(getIdentifier() + " signaled next element: " + t.toString());
                if (closed) return;

                synchronized (BlockingIterator.this) {
                    queue.add(t);
                    notifyWaitingMonitors();
                }
            }

            @Override
            public void onError(Throwable t) {
                if (log.isTraceEnabled()) log.trace(getIdentifier() + " signaled error", t);
                if (closed) return;

                synchronized (BlockingIterator.this) {
                    if (success || error) return; //ignore if already done state

                    error = true;
                    cause = t;
                    notifyWaitingMonitors();
                }
            }

            @Override
            public void onComplete() {
                if (log.isTraceEnabled()) log.trace(getIdentifier() + " signaled completion");
                if (closed) return;

                synchronized (BlockingIterator.this) {
                    if (success || error) return; //ignore if already done state

                    success = true;
                    notifyWaitingMonitors();
                }
            }

            private void notifyWaitingMonitors() {
                BlockingIterator.this.notifyAll();
                if (log.isTraceEnabled()) log.trace(getIdentifier() + " notified waiting monitors");
            }
        });
    }

    @Override
    public synchronized boolean hasNext() {
        synchronized (this) {
            if (queue.size() > 0) {
                return true;
            }

            if (error) {
                throw new ReactiveUncheckedException(cause);
            }

            if (success && queue.isEmpty()) {
                return false;
            }
        }

        return fetch();
    }

    @Override
    public T next() {
        synchronized (this) {
            if (!queue.isEmpty()) {
                return queue.poll();
            }

            if (error) {
                throw new ReactiveUncheckedException(cause);
            }
        }

        if (fetch()) {
            return queue.poll();
        }

        throw new NoSuchElementException();
    }

    public Subscription getSubscription() {
        if (subscription == null) {
            synchronized (this) {
                while (subscription == null) {
                    if (log.isTraceEnabled()) log.trace(getIdentifier() + " waiting for subscription notification...");
                    try {
                        this.wait();
                    } catch (InterruptedException e) {
                        throw new ReactiveUncheckedException(e);
                    }
                    if (log.isTraceEnabled())
                        log.trace(getIdentifier() + " subscription waiting monitor has been notified");
                }
            }
        }

        return subscription;
    }

    public void preload(int count) {
        if (log.isTraceEnabled()) log.trace(getIdentifier() + " requesting " + count + " elements...");

        getSubscription().request(count);
        if (log.isTraceEnabled()) log.trace(getIdentifier() + " requested");
    }

    private String getIdentifier() {
        return Integer.toHexString(hashCode());
    }

    private boolean fetch() {
        if (log.isTraceEnabled()) log.trace(getIdentifier() + " requesting element...");
        getSubscription().request(1);
        if (log.isTraceEnabled()) log.trace(getIdentifier() + " requested");

        synchronized (this) {
            while (!closed && !success && !error && queue.isEmpty()) {
                try {
                    if (log.isTraceEnabled()) log.trace(getIdentifier() + " waiting for notification...");
                    this.wait();
                    if (log.isTraceEnabled()) log.trace(getIdentifier() + " waiting monitor has been notified");
                } catch (InterruptedException e) {
                    throw new ReactiveUncheckedException(e);
                }
            }
        }

        if (error) {
            throw new ReactiveUncheckedException(cause);
        }

        return !queue.isEmpty();
    }

    @Override
    public void close() throws IOException {
        if (log.isTraceEnabled()) log.trace(this.toString() + " closed");
        closed = true;
        subscription.cancel();
    }

}
