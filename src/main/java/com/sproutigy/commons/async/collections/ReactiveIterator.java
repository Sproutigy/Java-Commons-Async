package com.sproutigy.commons.async.collections;

public interface ReactiveIterator<T> {
    void onNext(T element);

    void onFailure(Throwable cause);

    void onComplete();
}
