package com.sproutigy.commons.async.promises.then;

import com.sproutigy.commons.async.promises.Promise;

@FunctionalInterface
public interface ThenPromise<IN, OUT> {
    Promise<OUT> execute(IN value) throws Throwable;
}
