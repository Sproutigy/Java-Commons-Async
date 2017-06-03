package com.sproutigy.commons.async.collections;

import org.reactivestreams.Publisher;

import java.util.Iterator;

public class BlockingIterable<T> implements Iterable<T> {
    private Publisher<T> publisher;

    public BlockingIterable(Publisher<T> publisher) {
        this.publisher = publisher;
    }

    @Override
    public Iterator<T> iterator() {
        return new BlockingIterator<>(publisher);
    }
}
