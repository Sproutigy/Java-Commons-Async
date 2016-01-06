package com.sproutigy.commons.async.promises;

/**
 * @author LukeAheadNET
 */
public interface PromiseProvider<T> {
    Promise<T> provide(PromiseFactory factory) throws Throwable;
}
