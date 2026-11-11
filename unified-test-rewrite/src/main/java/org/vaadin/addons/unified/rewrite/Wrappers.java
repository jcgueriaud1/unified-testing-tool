package org.vaadin.addons.unified.rewrite;

import java.util.Map;

/** Which Wrapper replaces which TestBench element ({@code docs/spec/appendix-mapping.md}). */
final class Wrappers {

    private static final Map<String, String> BY_ELEMENT = Map.of(
            "com.vaadin.flow.component.textfield.testbench.TextFieldElement",
            "com.vaadin.testing.unified.component.UnifiedTextField",
            "com.vaadin.flow.component.button.testbench.ButtonElement",
            "com.vaadin.testing.unified.component.UnifiedButton");

    private Wrappers() {
    }

    /** The Wrapper's fully qualified name, or {@code null} when the element has none yet. */
    static String forElement(String elementFqn) {
        return BY_ELEMENT.get(elementFqn);
    }
}
