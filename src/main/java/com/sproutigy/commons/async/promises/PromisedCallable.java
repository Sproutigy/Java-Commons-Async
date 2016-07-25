package com.sproutigy.commons.async.promises;

public interface PromisedCallable<V> {
    Promise<V> call() throws Exception;
}
