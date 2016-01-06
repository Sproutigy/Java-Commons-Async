package com.sproutigy.commons.async.promises.listeners;

/**
 * @author LukeAheadNET
 */
public interface ElementListener<E> {
    void onNext(E element) throws Throwable;
}
