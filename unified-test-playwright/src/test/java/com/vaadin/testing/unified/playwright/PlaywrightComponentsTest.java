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
        assertEquals(org.vaadin.addons.dramafinder.element.DialogElement.class,
                PlaywrightComponents.of(com.vaadin.flow.component.dialog.Dialog.class).dramafinderElement());
        // Dramafinder's query finds the open notification cards
        assertEquals(org.vaadin.addons.dramafinder.element.NotificationElement.class,
                PlaywrightComponents.of(Notification.class).dramafinderElement());
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
