package com.sproutigy.commons.async;

public interface PromiseExecutor<V> {
    void run(Callback<V> resolve, Callback<Throwable> reject) throws Throwable;
}
