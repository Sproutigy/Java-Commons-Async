package com.sproutigy.commons.async.collections;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.Iterator;

/**
 * @author LukeAheadNET
 */
public class ReactiveStreamsUtil {
    private ReactiveStreamsUtil() {
    }

    public static <T> Iterable<T> iterable(Publisher<T> publisher) {
        return new BlockingIterable<>(publisher);
    }

    public static <T> Iterator<T> iterator(Publisher<T> publisher) {
        return new BlockingIterator<>(publisher);
    }

    public static <T> void iteratorReactive(Publisher<T> publisher, ReactiveIterator<T> reactiveIterator) {
        publisher.subscribe(new Subscriber<T>() {
            private Subscription subscription;

            @Override
            public void onSubscribe(Subscription subscription) {
                this.subscription = subscription;
                subscription.request(1);
            }

            @Override
            public void onNext(T element) {
                reactiveIterator.onNext(element);
                subscription.request(1);
            }

            @Override
            public void onError(Throwable error) {
                reactiveIterator.onFailure(error);
            }

            @Override
            public void onComplete() {
                reactiveIterator.onComplete();
            }
        });
    }
}
