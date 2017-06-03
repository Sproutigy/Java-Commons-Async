package com.sproutigy.commons.async.collections;

import java.io.Closeable;
import java.util.Iterator;

public interface CloseableIterator<T> extends Iterator<T>, AutoCloseable, Closeable {

}
