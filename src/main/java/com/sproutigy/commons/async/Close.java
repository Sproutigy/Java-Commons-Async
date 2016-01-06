package com.sproutigy.commons.async;

import com.sproutigy.commons.async.exceptions.ReactiveUncheckedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;

/**
 * Utility class to close any Closeable or AutoCloseable in using one-liner way
 *
 * @author LukeAheadNET
 */
public class Close {

    private static Logger log = LoggerFactory.getLogger(Close.class);

    private Close() {
    }

    public static void unchecked(Closeable closeable) {
        try {
            closeable.close();
        } catch (Exception e) {
            throw new ReactiveUncheckedException(e);
        }
    }

    public static void unchecked(AutoCloseable closeable) {
        try {
            closeable.close();
        } catch (Exception e) {
            throw new ReactiveUncheckedException(e);
        }
    }

    public static void silently(Closeable closeable) {
        try {
            closeable.close();
        } catch (Throwable ignore) {
        }
    }

    public static void silently(AutoCloseable closeable) {
        try {
            closeable.close();
        } catch (Throwable ignore) {
        }
    }

    public static void loggable(Closeable closeable) {
        try {
            closeable.close();
        } catch (Throwable e) {
            log.error("Error occured while closing resource", e);
        }
    }

    public static void loggable(AutoCloseable closeable) {
        try {
            closeable.close();
        } catch (Throwable e) {
            log.error("Error occured while closing resource", e);
        }
    }

    public static void loggableAndThrowable(Closeable closeable) throws Exception {
        try {
            closeable.close();
        } catch (Throwable e) {
            log.error("Error occured while closing resource", e);
            throw e;
        }
    }

    public static void loggableAndThrowable(AutoCloseable closeable) throws Exception {
        try {
            closeable.close();
        } catch (Throwable e) {
            log.error("Error occured while closing resource", e);
            throw e;
        }
    }
}
