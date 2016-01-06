package com.sproutigy.commons.async.collections;

/**
 * @author LukeAheadNET
 */
public interface ReactiveIterable<T> {
    void iterator(ReactiveIterator<T> reactiveIterator);
}
