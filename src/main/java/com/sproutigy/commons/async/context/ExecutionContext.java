package com.sproutigy.commons.async.context;

import com.sproutigy.commons.async.RunnableThrowable;

public interface ExecutionContext {
    void execute(RunnableThrowable runnableThrowable);
    void executeAsync(RunnableThrowable runnableThrowable);
}
