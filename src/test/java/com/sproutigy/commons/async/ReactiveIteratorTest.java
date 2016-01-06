package com.sproutigy.commons.async;

import com.sproutigy.commons.async.collections.BlockingIterator;
import org.junit.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author LukeAheadNET
 */
public class ReactiveIteratorTest {

    private final long SLEEP_TIME_MILLIS = 25;

    @Test
    public void testStandardFlow() {
        final int count = 50;

        Publisher<Integer> publisher = subscriber -> {
            Executor executor = Executors.newSingleThreadExecutor();
            AtomicInteger counter = new AtomicInteger();
            final Subscription subscription = new Subscription() {
                @Override
                public void request(long n) {
                    executor.execute(new Runnable() {
                        @Override
                        public void run() {
                            long left = n;
                            while (left > 0) {
                                Sleep.untilInterrupted(SLEEP_TIME_MILLIS);

                                int c = counter.incrementAndGet();
                                if (c > count) {
                                    subscriber.onComplete();
                                    break;
                                } else {
                                    subscriber.onNext(c);
                                    left--;
                                }
                            }
                        }
                    });
                }

                @Override
                public void cancel() {
                    //ignore
                }
            };

            subscriber.onSubscribe(subscription);
        };

        List<Integer> received = new LinkedList<>();
        BlockingIterator<Integer> iterator = new BlockingIterator<>(publisher);
        while (iterator.hasNext()) {
            Integer element = iterator.next();
            received.add(element);
        }

        assertEquals(received.size(), count);

        for (int i = 0; i < count; i++) {
            assertEquals((int) received.get(i), i + 1);
        }
    }

    @Test
    public void testCancellationOfUnlimitedStream() {
        final int count = 25;

        AtomicBoolean cancelled = new AtomicBoolean(false);

        Publisher<Integer> publisher = subscriber -> {
            Executor executor = Executors.newSingleThreadExecutor();
            AtomicInteger counter = new AtomicInteger();
            final Subscription subscription = new Subscription() {
                @Override
                public void request(long n) {
                    if (cancelled.get()) {
                        throw new IllegalStateException();
                    }

                    executor.execute(() -> {
                        long left = n;
                        while (left > 0) {
                            Sleep.untilInterrupted(SLEEP_TIME_MILLIS);

                            subscriber.onNext(counter.incrementAndGet());
                            left--;
                        }
                    });
                }

                @Override
                public void cancel() {
                    cancelled.set(true);
                }
            };

            subscriber.onSubscribe(subscription);
        };

        int counter = 0;
        List<Integer> received = new LinkedList<>();
        BlockingIterator<Integer> iterator = new BlockingIterator<>(publisher);
        while (iterator.hasNext()) {
            Integer element = iterator.next();
            counter++;
            received.add(element);

            if (counter >= count) {
                Close.unchecked(iterator);
                break;
            }
        }

        assertEquals(received.size(), count);

        for (int i = 0; i < count; i++) {
            assertEquals((int) received.get(i), i + 1);
        }

        assertTrue(cancelled.get());
    }
}
