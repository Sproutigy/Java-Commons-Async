package com.sproutigy.commons.async.promises;

/**
 * @author LukeAheadNET
 */
@FunctionalInterface
public interface CancellationHandler {
    boolean handleCancellation(boolean mayInterruptIfRunning);
}
