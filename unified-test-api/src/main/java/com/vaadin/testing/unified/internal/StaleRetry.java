package com.vaadin.testing.unified.internal;

import com.vaadin.testing.unified.spi.StaleElementException;
import java.util.function.Supplier;

/** Runs again what failed because an element went stale (re-rendered by Vaadin), within the timeout. */
final class StaleRetry {
    private StaleRetry() {}

    static <T> T call(TestRun run, Supplier<T> body) {
        long deadline = System.nanoTime() + run.timeout().toNanos();
        while (true) {
            try {
                return body.get();
            } catch (StaleElementException e) {
                if (System.nanoTime() > deadline) {
                    throw e;
                }
                run.session().sync();
            }
        }
    }
}
