package com.sproutigy.commons.async.exceptions;

/**
 * @author LukeAheadNET
 */
public class CancelledException extends Exception {
    public CancelledException() {
    }

    public CancelledException(String message) {
        super(message);
    }

    public CancelledException(String message, Throwable cause) {
        super(message, cause);
    }

    public CancelledException(Throwable cause) {
        super(cause);
    }

}
