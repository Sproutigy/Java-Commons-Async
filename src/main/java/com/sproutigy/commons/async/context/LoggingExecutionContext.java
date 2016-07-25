package com.sproutigy.commons.async.context;

import com.sproutigy.commons.async.Async;
import com.sproutigy.commons.async.RunnableThrowable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingExecutionContext implements ExecutionContext {
    private static Logger log = LoggerFactory.getLogger(LoggingExecutionContext.class);

    @Override
    public void execute(RunnableThrowable runnableThrowable) {
        try {
            runnableThrowable.run();
        } catch(Throwable cause) {
            log.error("Execution failed", cause);
        }
    }

    @Override
    public void executeAsync(RunnableThrowable runnableThrowable) {
        Async.commonExecutor().submit(() -> {
            try {
                runnableThrowable.run();
            } catch(Throwable cause) {
                log.error("Execution failed", cause);
            }
        });
    }
}
