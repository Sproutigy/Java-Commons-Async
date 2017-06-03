package com.sproutigy.commons.async;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class OptionalLogger {
    private OptionalLogger() {
    }


    public static <T> Logger getLogger(Class<T> clazz) {
        try {
            Class.forName("org.slf4j.Logger");
            return LoggerFactory.getLogger(clazz);
        } catch (ClassNotFoundException ignore) {
        }
        return null;
    }


    public static void trace(Logger logger, String message, Object... objects) {
        if (logger != null) {
            logger.trace(message, objects);
        } else {
            System.err.println("[TRACE] " + resolveString(message, objects));
        }
    }

    public static void debug(Logger logger, String message, Object... objects) {
        if (logger != null) {
            logger.debug(message, objects);
        } else {
            System.err.println("[DEBUG] " + resolveString(message, objects));
        }
    }

    public static void info(Logger logger, String message, Object... objects) {
        if (logger != null) {
            logger.info(message, objects);
        } else {
            System.err.println("[INFO] " + resolveString(message, objects));
        }
    }

    public static void warn(Logger logger, String message, Object... objects) {
        if (logger != null) {
            logger.warn(message, objects);
        } else {
            System.err.println("[WARN] " + resolveString(message, objects));
        }
    }

    public static void error(Logger logger, String message, Object... objects) {
        if (logger != null) {
            logger.error(resolveString(message, objects));
        } else {
            System.err.println("[ERROR] " + resolveString(message, objects));

            if (objects.length > 0) {
                Object lastObj = objects[objects.length - 1];
                if (lastObj instanceof Throwable) {
                    ((Throwable) lastObj).printStackTrace();
                }
            }
        }
    }

    private static String resolveString(String message, Object... objects) {
        String msg = message;
        for (int i = 0; i < objects.length; i++) {
            int idx = message.indexOf("{}");
            if (idx >= 0) {
                msg = msg.substring(idx) + objects[i] + msg.substring(idx + 2);
            }
        }
        return msg;
    }

}
