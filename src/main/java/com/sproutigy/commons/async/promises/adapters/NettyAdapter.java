package com.sproutigy.commons.async.promises.adapters;

import com.sproutigy.commons.async.promises.Deferred;
import com.sproutigy.commons.async.promises.Promise;
import com.sproutigy.commons.async.promises.PromiseFactory;
import io.netty.util.concurrent.FutureListener;

/**
 * @author LukeAheadNET
 */
public class NettyAdapter {

    public static <V> Promise<V> from(io.netty.util.concurrent.Future<V> nettyFuture) {
        return from(nettyFuture, PromiseFactory.DEFAULT);
    }

    public static <V> Promise<V> from(io.netty.util.concurrent.Future<V> nettyFuture, PromiseFactory promiseFactory) {
        Deferred<V> deferred = promiseFactory.defer();
        deferred.pending();
        nettyFuture.addListener(new FutureListener<V>() {
            @Override
            public void operationComplete(io.netty.util.concurrent.Future<V> vFuture) throws Exception {
                if (vFuture.isSuccess()) {
                    deferred.success(vFuture.getNow());
                } else {
                    deferred.failure(vFuture.cause());
                }
            }
        });
        return deferred.promise();
    }

}
