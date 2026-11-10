package com.vaadin.testing.unified.component;

import com.vaadin.testing.unified.Scope;
import com.vaadin.testing.unified.UnifiedWrapper;

/**
 * Wrapper for any component found with {@code findComponent(..)}: Unified Element operations, and a Scope to
 * search inside it ({@code page.findComponent(FormLayout.class).single().find(UnifiedTextField.class)}).
 */
public interface UnifiedGenericComponent extends UnifiedWrapper, Scope {

    /** {@link com.vaadin.testing.unified.UnifiedElement#getText()}: the DOM text, the same on every tool. */
    default String getText() { return element().getText(); }
}
