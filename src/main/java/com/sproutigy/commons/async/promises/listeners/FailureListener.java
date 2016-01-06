package com.sproutigy.commons.async.promises.listeners;

/**
 * @author LukeAheadNET
 */
public interface FailureListener {
    void onFailure(Throwable cause) throws Throwable;
}
