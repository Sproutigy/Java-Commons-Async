package com.sproutigy.commons.async;

public interface Callback<V> {
    void onCallback(V value) throws Exception;
}
