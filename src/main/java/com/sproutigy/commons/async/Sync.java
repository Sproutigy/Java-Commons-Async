package com.sproutigy.commons.async;

import java.util.concurrent.Executor;

/**
 * @author LukeAheadNET
 */
public class Sync {
    public static final Executor EXECUTOR = command -> command.run();

    private Sync() {
    }

}
