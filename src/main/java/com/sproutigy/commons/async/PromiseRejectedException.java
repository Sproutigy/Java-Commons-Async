package com.sproutigy.commons.async;

public class PromiseRejectedException extends Exception {
    private Object causeObject;

    public PromiseRejectedException(Object causeObject) {
        super("Promise rejected: " + causeObject, (causeObject != null && causeObject instanceof Throwable) ? (Throwable) causeObject : null);
        this.causeObject = causeObject;
    }

    public Object getCauseObject() {
        return causeObject;
    }
}
