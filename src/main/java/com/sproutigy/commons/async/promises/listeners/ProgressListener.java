package com.sproutigy.commons.async.promises.listeners;

/**
 * @author LukeAheadNET
 */
public interface ProgressListener<T> {
    void onProgress(T progress) throws Throwable;
}
