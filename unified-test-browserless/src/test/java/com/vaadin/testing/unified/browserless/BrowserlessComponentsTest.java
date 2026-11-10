package com.vaadin.testing.unified.browserless;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.notification.Notification;
import org.junit.jupiter.api.Test;

class BrowserlessComponentsTest {

    static class MyNotification extends Notification {}

    @Test
    void notificationIsRegistered() {
        assertNotSame(BrowserlessComponents.of(Button.class), BrowserlessComponents.of(Notification.class));
    }

    @Test
    void dialogIsRegistered() {
        assertNotSame(BrowserlessComponents.of(com.vaadin.flow.component.button.Button.class),
                BrowserlessComponents.of(com.vaadin.flow.component.dialog.Dialog.class));
    }

    @Test
    void subclassesUseTheirSupertypesRegistration() {
        assertSame(BrowserlessComponents.of(Notification.class), BrowserlessComponents.of(MyNotification.class));
    }

    @Test
    void unregisteredComponentsGetTheGenericDefault() {
        assertNotSame(BrowserlessComponents.of(Notification.class), BrowserlessComponents.of(Button.class));
    }
}
