package com.sproutigy.commons.async.promises.listeners;

import com.sproutigy.commons.async.promises.Promise;

/**
 * @author LukeAheadNET
 */
@FunctionalInterface
public interface PromiseCreationListener {
    void onPromiseCreation(Promise promise) throws Throwable;
}
