package com.vaadin.testing.unified.browserless;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.notification.Notification;
import org.junit.jupiter.api.Test;

class BrowserlessComponentsTest {

    static class MyNotification extends Notification {}

    @Test
    void notificationIsRegistered() {
        assertFalse(BrowserlessComponents.of(Notification.class).queryFindsText());
    }

    @Test
    void subclassesUseTheirSupertypesRegistration() {
        assertSame(BrowserlessComponents.of(Notification.class), BrowserlessComponents.of(MyNotification.class));
    }

    @Test
    void unregisteredComponentsGetTheGenericDefault() {
        assertTrue(BrowserlessComponents.of(Button.class).queryFindsText());
        assertNotSame(BrowserlessComponents.of(Notification.class), BrowserlessComponents.of(Button.class));
    }
}
