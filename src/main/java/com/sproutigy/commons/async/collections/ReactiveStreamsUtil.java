package com.sproutigy.commons.async.collections;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.Iterator;

public final class ReactiveStreamsUtil {
    private ReactiveStreamsUtil() {
    }

    public static <T> Iterable<T> asIterable(Publisher<T> publisher) {
        return new BlockingIterable<>(publisher);
    }

    public static <T> Iterator<T> asIterator(Publisher<T> publisher) {
        return new BlockingIterator<>(publisher);
    }

    public static <T> void bindReactiveIterator(Publisher<T> publisher, final ReactiveIterator<T> reactiveIterator) {
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
