package com.sproutigy.commons.async.promises.then;

import com.sproutigy.commons.async.promises.Promise;

public interface ThenPromised<IN, OUT> {
    Promise<OUT> execute(IN value) throws Throwable;
}
