package com.sproutigy.commons.async.promises.then;

import com.sproutigy.commons.async.promises.Promise;
import com.sproutigy.commons.async.promises.PromiseFactory;

public interface ThenBuildPromise<IN, OUT> {
    Promise<OUT> execute(IN value, PromiseFactory promiseFactory) throws Throwable;
}
