package com.sproutigy.commons.async.promises.listeners;

/**
 * @author LukeAheadNET
 */
public interface SuccessListener<V> {
    void onSuccess(V value) throws Throwable;
}
