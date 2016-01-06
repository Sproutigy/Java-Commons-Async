package com.sproutigy.commons.async.exceptions;

/**
 * @author LukeAheadNET
 */
public class ReactiveUncheckedException extends RuntimeException {
    public ReactiveUncheckedException() {
    }

    public ReactiveUncheckedException(String message) {
        super(message);
    }

    public ReactiveUncheckedException(String message, Throwable cause) {
        super(message, cause);
    }

    public ReactiveUncheckedException(Throwable cause) {
        super(cause);
    }

    public ReactiveUncheckedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
