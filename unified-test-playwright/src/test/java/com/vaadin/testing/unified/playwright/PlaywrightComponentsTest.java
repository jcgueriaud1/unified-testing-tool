package com.vaadin.testing.unified.playwright;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.TextField;
import org.junit.jupiter.api.Test;
import org.vaadin.addons.dramafinder.element.ButtonElement;
import org.vaadin.addons.dramafinder.element.TextFieldElement;

class PlaywrightComponentsTest {

    static class MyTextField extends TextField {}

    @Test
    void coreSetComponentsAreRegistered() {
        assertEquals(TextFieldElement.class, PlaywrightComponents.of(TextField.class).dramafinderElement());
        assertEquals(ButtonElement.class, PlaywrightComponents.of(Button.class).dramafinderElement());
        assertEquals("vaadin-notification-card[slot]",
                PlaywrightComponents.of(Notification.class).selector("vaadin-notification"));
    }

    @Test
    void subclassesUseTheirSupertypesRegistration() {
        assertEquals(TextFieldElement.class, PlaywrightComponents.of(MyTextField.class).dramafinderElement());
    }

    @Test
    void unregisteredComponentsGetTheGenericDefault() {
        assertSame(PlaywrightComponents.DEFAULT, PlaywrightComponents.of(Span.class));
        assertNull(PlaywrightComponents.of(Span.class).dramafinderElement());
        assertEquals("span", PlaywrightComponents.of(Span.class).selector("span"));
    }
}
