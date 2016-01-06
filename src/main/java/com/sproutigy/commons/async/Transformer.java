package com.sproutigy.commons.async;

/**
 * @author LukeAheadNET
 */
@FunctionalInterface
public interface Transformer<IN, OUT> {
    OUT process(IN input) throws Throwable;
}
