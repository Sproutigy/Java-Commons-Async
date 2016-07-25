package com.sproutigy.commons.async.promises;

public interface PromisedTransformer<IN, OUT> {
    Promise<OUT> transform(IN input) throws Exception;
}
