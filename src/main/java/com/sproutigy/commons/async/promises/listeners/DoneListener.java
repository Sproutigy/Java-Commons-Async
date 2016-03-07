package com.sproutigy.commons.async.promises.listeners;

public interface DoneListener<T> {
    void onDone(T value, Throwable failCause) throws Throwable;
}
