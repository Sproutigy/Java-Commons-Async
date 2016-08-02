package com.sproutigy.commons.async;

/**
 * @author LukeAheadNET
 */
@FunctionalInterface
public interface Handler<T> {
    void handle(T arg) throws Throwable;
}
