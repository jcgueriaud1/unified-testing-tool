package com.vaadin.testing.unified.spi;

import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.extension.ExtensionContext;

/** What an adapter gets when a test starts. */
public interface AdapterContext {
    ExtensionContext extensionContext();

    /** A JUnit configuration parameter (also read from system properties and junit-platform.properties). */
    Optional<String> configuration(String key);

    Duration timeout();
}
