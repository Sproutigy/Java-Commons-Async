package com.sproutigy.commons.async.promises.then;

public interface ThenExecute<IN> {
    void execute(IN value) throws Throwable;
}
