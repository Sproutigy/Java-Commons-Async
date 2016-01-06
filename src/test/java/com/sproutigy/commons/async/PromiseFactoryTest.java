package com.sproutigy.commons.async;

import com.sproutigy.commons.async.promises.Promise;
import com.sproutigy.commons.async.promises.PromiseFactory;
import org.junit.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author LukeAheadNET
 */
public class PromiseFactoryTest {

    @Test
    public void testPromisesAll() throws InterruptedException {
        PromiseFactory factory = PromiseFactory.DEFAULT;
        AtomicBoolean ab1 = new AtomicBoolean(false);
        AtomicBoolean ab2 = new AtomicBoolean(false);
        AtomicBoolean ab3 = new AtomicBoolean(false);
        Promise p1 = factory.async(() -> ab1.set(true));
        Promise p2 = factory.async(() -> {
            Sleep.unchecked(100);
            ab2.set(true);
        });
        Promise p3 = factory.async(() -> {
            Sleep.unchecked(200);
            ab3.set(true);
        });
        Promise pAll = factory.all(p1, p2, p3);
        pAll.await();
        assertTrue(ab1.get());
        assertTrue(ab2.get());
        assertTrue(ab3.get());
    }

    @Test
    public void testPromisesAny() throws InterruptedException {
        PromiseFactory factory = PromiseFactory.DEFAULT;
        AtomicBoolean ab1 = new AtomicBoolean(false);
        AtomicBoolean ab2 = new AtomicBoolean(false);
        AtomicBoolean ab3 = new AtomicBoolean(false);
        Promise p1 = factory.async(() -> ab1.set(true));
        Promise p2 = factory.async(() -> {
            Sleep.unchecked(100);
            ab2.set(true);
        });
        Promise p3 = factory.async(() -> {
            Sleep.unchecked(200);
            ab3.set(true);
        });
        Promise pAll = factory.any(p1, p2, p3);
        pAll.await();
        assertTrue(ab1.get());
        assertFalse(ab2.get());
        assertFalse(ab3.get());
    }

    @Test
    public void testCustomExecutor() {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        PromiseFactory factory = new PromiseFactory(executorService);
        AtomicBoolean ab1 = new AtomicBoolean(false);
        AtomicBoolean ab2 = new AtomicBoolean(false);
        factory.async(() -> {
            ab1.set(true);
            Sleep.unchecked(1000);
        });
        factory.async(() -> ab2.set(true));
        Sleep.unchecked(100);
        assertTrue(ab1.get());
        assertFalse(ab2.get());
    }
}
