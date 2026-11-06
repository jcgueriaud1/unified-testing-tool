// PROTOTYPE: what every Unified API wrapper shares, identical in all variants.
package proto.common;

import java.time.Duration;
import java.util.Optional;

public interface Wrapper {
    /** Escape Hatch (decided in #10). Fails if the current Testing Tool can't provide the type. */
    <T> T unwrap(Class<T> type);

    /** Escape Hatch that returns empty on other Testing Tools, so shared tests stay shared. */
    <T> Optional<T> tryUnwrap(Class<T> type);

    /** Per-call timeout override (decided in #8); returns a view of this wrapper with that timeout. */
    Wrapper withTimeout(Duration timeout);

    // Getters below return immediately (after the round-trip) and never retry (decided in #8).
    boolean isVisible();
    boolean isEnabled();
}
