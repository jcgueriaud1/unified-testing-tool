package com.vaadin.testing.unified;

import java.util.Optional;

/** Escape Hatch: leave the Unified API for the underlying Testing Tool. No Unified API guarantees inside. */
public interface HasEscapeHatch {

    /**
     * Returns the Testing Tool's own object of the given type. Fails, naming the missing
     * {@link RequiresTestingTool} annotation, if the current Testing Tool can't provide it.
     */
    <T> T unwrap(Class<T> type);

    /** Like {@link #unwrap(Class)}, but empty on Testing Tools that can't provide the type. */
    <T> Optional<T> tryUnwrap(Class<T> type);
}
