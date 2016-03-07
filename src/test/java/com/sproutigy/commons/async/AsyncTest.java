package com.sproutigy.commons.async;

import org.junit.Test;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

/**
 * @author LukeAheadNET
 */
public class AsyncTest {
    @Test
    public void testScheduler() {
        AtomicBoolean ab1 = new AtomicBoolean(false);
        AtomicBoolean ab2 = new AtomicBoolean(false);
        AtomicBoolean ab3 = new AtomicBoolean(false);
        ScheduledExecutorService executorService = Async.newScheduler();
        executorService.schedule(() -> ab1.set(true), 100, TimeUnit.MILLISECONDS);
        executorService.schedule(() -> ab2.set(true), 500, TimeUnit.MILLISECONDS);
        executorService.schedule(() -> ab3.set(true), 1500, TimeUnit.MILLISECONDS);
        Sleep.unchecked(250, TimeUnit.MILLISECONDS);
        assertTrue(ab1.get());
        assertFalse(ab2.get());
        Sleep.unchecked(700, TimeUnit.MILLISECONDS);
        assertTrue(ab1.get());
        assertTrue(ab2.get());
        assertEquals(1, executorService.shutdownNow().size());
        assertFalse(ab3.get());
    }
}
