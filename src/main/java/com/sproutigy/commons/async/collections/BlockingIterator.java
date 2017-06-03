package com.sproutigy.commons.async.collections;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Queue;

public class BlockingIterator<T> implements Iterator<T>, CloseableIterator<T> {

    private Queue<T> queue = new LinkedList<>();
    private volatile boolean success = false;
    private volatile boolean error = false;
    private volatile Throwable cause = null;
    private volatile boolean closed = false;

    private Subscription subscription;

    public BlockingIterator(Publisher<T> publisher) {
        publisher.subscribe(new Subscriber<T>() {
            @Override
            public void onSubscribe(Subscription subscription) {

                BlockingIterator.this.subscription = subscription;

                synchronized (BlockingIterator.this) {
                    notifyWaitingMonitors();
                }
            }

            @Override
            public void onNext(T t) {
                if (closed) return;

                synchronized (BlockingIterator.this) {
                    queue.add(t);
                    notifyWaitingMonitors();
                }
            }

            @Override
            public void onError(Throwable t) {
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
                if (closed) return;

                synchronized (BlockingIterator.this) {
                    if (success || error) return; //ignore if already done state

                    success = true;
                    notifyWaitingMonitors();
                }
            }

            private void notifyWaitingMonitors() {
                BlockingIterator.this.notifyAll();
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
                throw new RuntimeException(cause);
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
                throw new RuntimeException(cause);
            }
        }

        if (fetch()) {
            return queue.poll();
        }

        throw new NoSuchElementException();
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("remove");
    }

    public Subscription getSubscription() {
        if (subscription == null) {
            synchronized (this) {
                while (subscription == null) {
                    try {
                        this.wait();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }

        return subscription;
    }

    public void preload(int count) {
        getSubscription().request(count);
    }

    private String getIdentifier() {
        return Integer.toHexString(hashCode());
    }

    private boolean fetch() {
        getSubscription().request(1);

        synchronized (this) {
            while (!closed && !success && !error && queue.isEmpty()) {
                try {
                    this.wait();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        if (error) {
            throw new RuntimeException(cause);
        }

        return !queue.isEmpty();
    }

    @Override
    public void close() throws IOException {
        closed = true;
        subscription.cancel();
    }

}
