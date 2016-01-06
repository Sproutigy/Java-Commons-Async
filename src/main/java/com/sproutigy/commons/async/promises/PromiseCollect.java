package com.sproutigy.commons.async.promises;

import com.sproutigy.commons.async.Transformer;
import com.sproutigy.commons.async.promises.listeners.ElementListener;
import org.reactivestreams.Publisher;

import java.util.Collection;

/**
 * @author LukeAheadNET
 */
public interface PromiseCollect<E> extends Promise<Collection<E>>, Publisher<E>, Iterable<E> {

    PromiseCollect<E> addElementListener(ElementListener<E> elementListener);

    PromiseCollect<E> removeElementListener(ElementListener<E> elementListener);

    PromiseCollect<E> onEveryBlocking(ElementListener<E> elementListener);

    PromiseCollect<E> onEveryAsync(ElementListener<E> elementListener);

    <OE> PromiseCollect<OE> mapBlocking(Transformer<E, OE> mapper);

    <OE> PromiseCollect<OE> mapAsync(Transformer<E, OE> mapper);

    int count() throws Exception;

    E first() throws Exception;

    E last() throws Exception;
}
