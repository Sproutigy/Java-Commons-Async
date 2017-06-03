package com.sproutigy.commons.async;

import com.google.common.util.concurrent.ListenableFuture;
import io.netty.util.concurrent.FutureListener;

import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.*;

public final class FutureWatch {
    private FutureWatch() {
    }

    private static class Entry<V> {
        Future<V> future;
        ExecutionCallback<V> callback;
        boolean needWait;

        public Entry(Future<V> future, ExecutionCallback<V> callback, boolean needWait) {
            this.future = future;
            this.callback = callback;
            this.needWait = needWait;
        }
    }

    private static final ExecutorService CALLBACK_EXECUTOR = Executors.newCachedThreadPool();

    private static final Collection<Entry> ENTRIES = new LinkedList<>();

    private static final int WAIT_INITIAL_TIME_MILLIS = 30;
    private static final int WAIT_LOOP_TIME_MILLIS = 15;

    private static boolean isGuava = false;
    private static boolean isJava8 = false;
    private static boolean isNetty = false;

    static {
        isGuava = isClassAvailable("com.google.common.util.concurrent.ListenableFuture");
        isJava8 = isClassAvailable("java.util.concurrent.CompletionStage");
        isNetty = isClassAvailable("io.netty.util.concurrent.Future");
    }


    static boolean isClassAvailable(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException ignore) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private static Thread thread = new Thread(new Runnable() {
        @Override
        public void run() {
            try {
                while (true) {
                    Entry[] entries = null;
                    synchronized (ENTRIES) {
                        if (ENTRIES.size() > 0) {
                            entries = new Entry[ENTRIES.size()];
                            ENTRIES.toArray(entries);
                        } else {
                            ENTRIES.wait();
                        }
                    }

                    if (entries != null) {
                        for (int i = 0; i < entries.length; i++) {
                            Entry entry = entries[i];
                            if (entry.needWait) {
                                if (tryHandle(WAIT_LOOP_TIME_MILLIS, entry.future, entry.callback)) {
                                    synchronized (ENTRIES) {
                                        ENTRIES.remove(entry);
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (InterruptedException ignored) {
            }
        }
    });

    static {
        thread.setName("future-callback");
        thread.setDaemon(true);
        thread.start();
    }

    public static <V> void listen(final Future<V> future, final ExecutionCallback<V> callback) {
        if (future instanceof Promise) {
            ((Promise<V>) future).onFulfill(new Callback<V>() {
                @Override
                public void onCallback(V value) throws Exception {
                    callback.callback(ExecutionCallback.Result.Success, value, null);
                }
            });
            ((Promise<V>) future).onReject(new Callback<Throwable>() {
                @Override
                public void onCallback(Throwable cause) throws Exception {
                    callback.callback(ExecutionCallback.Result.Failure, null, cause);
                }
            });
            //TODO: unlisten support
            return;
        }

        if (isGuava) {
            if (future instanceof ListenableFuture) {
                final Entry entry = new Entry<>(future, callback, false);
                registerEntry(entry);
                ((ListenableFuture) future).addListener(
                        createCompletionHandler(entry, future, callback), CALLBACK_EXECUTOR
                );
                return;
            }
        }

        if (isJava8) {
            if (future instanceof CompletionStage) {
                final Entry entry = new Entry<>(future, callback, false);
                registerEntry(entry);
                ((CompletionStage) future).thenRun(
                        createCompletionHandler(entry, future, callback)
                );
                return;
            }
        }

        if (isNetty) {
            if (future instanceof io.netty.util.concurrent.Future) {
                final Entry entry = new Entry<>(future, callback, false);
                registerEntry(entry);
                ((io.netty.util.concurrent.Future) future).addListener(new FutureListener() {
                    @Override
                    public void operationComplete(final io.netty.util.concurrent.Future future) throws Exception {
                        createCompletionHandler(entry, future, callback);
                    }
                });
                return;
            }
        }

        if (!tryHandle(WAIT_INITIAL_TIME_MILLIS, future, callback)) {
            synchronized (ENTRIES) {
                ENTRIES.add(new Entry<>(future, callback, true));
                ENTRIES.notifyAll();
            }
        }
    }

    private static void registerEntry(Entry entry) {
        synchronized (ENTRIES) {
            ENTRIES.add(entry);
        }
    }

    private static <V> Runnable createCompletionHandler(final Entry entry, final Future<V> future, final ExecutionCallback<V> callback) {
        return new Runnable() {
            @Override
            public void run() {
                synchronized (ENTRIES) {
                    if (!ENTRIES.contains(entry)) {
                        return;
                    }
                    ENTRIES.remove(entry);
                }
                tryHandle(WAIT_INITIAL_TIME_MILLIS, future, callback);
            }
        };
    }

    public static <V> void unlisten(Future<V> future, ExecutionCallback<V> callback) {
        synchronized (ENTRIES) {
            Entry found = null;
            for (Entry entry : ENTRIES) {
                if (entry.future == future && entry.callback == callback) {
                    found = entry;
                    break;
                }
            }

            if (found != null) {
                ENTRIES.remove(found);
            }
        }
    }

    private static <V> boolean tryHandle(int waitTimeMillis, Future<V> future, ExecutionCallback<V> callback) {
        try {
            V value = future.get(waitTimeMillis, TimeUnit.MILLISECONDS);
            callCallback(callback, ExecutionCallback.Result.Success, value, null);
            return true;
        } catch (CancellationException cancelled) {
            callCallback(callback, ExecutionCallback.Result.Cancelled, null, cancelled);
            return true;
        } catch (ExecutionException cause) {
            callCallback(callback, ExecutionCallback.Result.Failure, null, cause);
            return true;
        } catch (InterruptedException | TimeoutException ignore) {
        }

        return false;
    }

    private static <V> void callCallback(final ExecutionCallback<V> callback, final ExecutionCallback.Result result, final V value, final Throwable cause) {
        CALLBACK_EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                callback.callback(result, value, cause);
            }
        });
    }
}
