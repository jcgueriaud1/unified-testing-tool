package com.vaadin.testing.unified.spi;

/**
 * Thrown by an adapter when the element it was reading or acting on went away in the meantime, typically
 * because Vaadin re-rendered it. The core finds the element again and retries, within the timeout.
 */
public class StaleElementException extends RuntimeException {

    public StaleElementException(String message, Throwable cause) {
        super(message, cause);
    }
}
