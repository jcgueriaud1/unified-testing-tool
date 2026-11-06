// PROTOTYPE: annotation stubs shared by all variants. No behaviour.
package proto.common;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public final class Annotations {
    private Annotations() {}

    /** JUnit 6 extension: injects the Unified API entry object (decided in #9). */
    @Retention(RetentionPolicy.RUNTIME) public @interface UnifiedUITest {}

    /** Stand-in for org.junit.jupiter.api.Test, to avoid a JUnit dependency in the prototype. */
    @Retention(RetentionPolicy.RUNTIME) public @interface Test {}

    /** Capability: needs a real browser (decided in #11). */
    @Retention(RetentionPolicy.RUNTIME) public @interface RequiresBrowser {}
}
