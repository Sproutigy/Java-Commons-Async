package com.sproutigy.commons.async.promises;

/**
 * @author LukeAheadNET
 */
@FunctionalInterface
public interface PromiseProviderByInput<IN, OUT> {
    Promise<OUT> provide(IN input, PromiseFactory factory) throws Throwable;
}
