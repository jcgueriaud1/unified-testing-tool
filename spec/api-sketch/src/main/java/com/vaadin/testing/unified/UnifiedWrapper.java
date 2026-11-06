package com.vaadin.testing.unified;

import java.time.Duration;

/**
 * Base of every wrapper. A wrapper is an interface with default methods over {@link #element()}; the
 * Unified API implements it at runtime. Wrappers are lazy: each call finds the component again.
 */
public interface UnifiedWrapper extends HasEscapeHatch {
    UnifiedElement element();

    /** Returns a view of this wrapper with another timeout for Auto-Wait and assertions. */
    UnifiedWrapper withTimeout(Duration timeout);

    // getters: immediate, no retry
    default boolean isVisible() { return element().getPropertyBoolean("hidden") != Boolean.TRUE; }
    boolean isEnabled();
}
