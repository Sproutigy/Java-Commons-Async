package com.sproutigy.commons.async.promises.listeners;

import com.sproutigy.commons.async.promises.Promise;
import com.sproutigy.commons.async.promises.PromiseState;

/**
 * @author LukeAheadNET
 */
public interface PromiseStateListener<V> {
    void onPromiseStateChanged(Promise<V> promise, PromiseState state) throws Throwable;
}
