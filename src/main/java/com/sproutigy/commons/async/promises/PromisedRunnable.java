package com.sproutigy.commons.async.promises;

public interface PromisedRunnable {
    Promise<Void> run() throws Exception;
}
