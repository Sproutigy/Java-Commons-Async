package com.sproutigy.commons.async.exceptions;

/**
 * @author LukeAheadNET
 */
public class UncheckedInterruptedException extends RuntimeException {
    public UncheckedInterruptedException() {
    }

    public UncheckedInterruptedException(Throwable cause) {
        super(cause);
    }
}
