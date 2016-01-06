package com.sproutigy.commons.async.collections;

/**
 * @author LukeAheadNET
 */
public interface ReactiveIterator<T> {
    void onNext(T element);

    void onFailure(Throwable cause);

    void onComplete();
}
