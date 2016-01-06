package com.sproutigy.commons.async.promises;

/**
 * @author LukeAheadNET
 */
public enum PromiseState {
    Waiting(false), Pending(false), Succeeded(true), Failed(true), Cancelled(true);

    private boolean done;

    PromiseState(boolean done) {
        this.done = done;
    }

    public boolean isDone() {
        return done;
    }
}
