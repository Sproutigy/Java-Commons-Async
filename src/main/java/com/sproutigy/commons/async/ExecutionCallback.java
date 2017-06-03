package com.sproutigy.commons.async;

@FunctionalInterface
public interface ExecutionCallback<V> {
    enum Result {
        Success,
        Failure,
        Cancelled;

        public boolean isSuccess() {
            return this == Success;
        }

        public boolean isCancelled() {
            return this == Cancelled;
        }
    }

    void callback(Result result, V value, Throwable cause);
}
