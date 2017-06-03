package com.sproutigy.commons.async;

import org.junit.Test;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

/**
 * @author LukeAheadNET
 */
public class PromiseTest {

    @Test
    public void testPromisesAll() throws InterruptedException {
        AtomicBoolean ab1 = new AtomicBoolean(false);
        AtomicBoolean ab2 = new AtomicBoolean(false);
        AtomicBoolean ab3 = new AtomicBoolean(false);
        Promise p1 = Promise.execute(() -> ab1.set(true));
        Promise p2 = Promise.execute(() -> {
            Sleep.unchecked(100);
            ab2.set(true);
        });

        Promise p3 = Promise.execute(() -> {
            Sleep.unchecked(200);
            ab3.set(true);
        });
        Promise pAll = Promise.all(p1, p2, p3);
        pAll.await();
        assertTrue(ab1.get());
        assertTrue(ab2.get());
        assertTrue(ab3.get());
    }

    @Test
    public void testPromisesRace() throws InterruptedException {
        AtomicBoolean ab1 = new AtomicBoolean(false);
        AtomicBoolean ab2 = new AtomicBoolean(false);
        AtomicBoolean ab3 = new AtomicBoolean(false);
        Promise p1 = Promise.execute((() -> ab1.set(true))).setName("p1");
        Promise p2 = Promise.execute(() -> {
            Sleep.unchecked(100);
            ab2.set(true);
        }).setName("p2");
        Promise p3 = Promise.execute(() -> {
            Sleep.unchecked(200);
            ab3.set(true);
        }).setName("p3");
        Promise pRace = Promise.race(p1, p2, p3).setName("pRace");
        pRace.await();
        assertTrue(ab1.get());
        assertFalse(ab2.get());
        assertFalse(ab3.get());

        p2.await();
        p3.await();
    }

    @Test
    public void testThen() throws Exception {
        ArrayList<Integer> order = new ArrayList<>();

        int result = Promise.execute(() -> order.add(1))
        .then(r -> order.add(2))
        .then(r -> { order.add(3); return 1; })
        .then(val -> { order.add(4); return val+2; })
        .thenAwait(in -> { order.add(5); return Promise.execute(in, x -> x+3); })
        .getResult();

        assertEquals(6, result);

        assertEquals(5, order.size());
        assertEquals(1, (int)order.get(0));
        assertEquals(2, (int)order.get(1));
        assertEquals(3, (int)order.get(2));
        assertEquals(4, (int)order.get(3));
        assertEquals(5, (int)order.get(4));
    }

    @Test
    public void testThenCatch() throws Exception {
        String result = Promise.resolve("start")
                .then(r -> { throw new IllegalStateException(); })
                .then(r -> { throw new IllegalArgumentException(); }) //should be skipped
                .catchHandle(cause -> {
                    if (cause instanceof IllegalStateException) {
                        return "OK";
                    } else {
                        return "FAIL";
                    }
                })
                .get();

        assertEquals("OK", result);
    }
}
