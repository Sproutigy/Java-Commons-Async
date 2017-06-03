package com.sproutigy.commons.async;

public interface Transform<S, T> {
    T transform(S argument) throws Exception;
}
