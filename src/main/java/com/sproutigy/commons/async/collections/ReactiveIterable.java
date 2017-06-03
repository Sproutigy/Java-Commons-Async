package com.sproutigy.commons.async.collections;

public interface ReactiveIterable<T> {
    void iterator(ReactiveIterator<T> reactiveIterator);
}
