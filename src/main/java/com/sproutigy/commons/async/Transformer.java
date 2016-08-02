package com.sproutigy.commons.async;

/**
 * @author LukeAheadNET
 */
@FunctionalInterface
public interface Transformer<IN, OUT> {
    OUT transform(IN input) throws Throwable;
}
