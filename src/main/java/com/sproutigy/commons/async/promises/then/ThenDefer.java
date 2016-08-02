package com.sproutigy.commons.async.promises.then;

import com.sproutigy.commons.async.promises.Deferred;

@FunctionalInterface
public interface ThenDefer<IN, OUT> {
    void execute(IN value, Deferred<OUT> deferred) throws Throwable;
}
