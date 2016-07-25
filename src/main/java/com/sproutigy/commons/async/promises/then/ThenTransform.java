package com.sproutigy.commons.async.promises.then;

public interface ThenTransform<IN, OUT> {
    OUT execute(IN value) throws Throwable;
}
