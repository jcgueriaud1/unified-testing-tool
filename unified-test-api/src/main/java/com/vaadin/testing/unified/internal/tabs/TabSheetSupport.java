package com.vaadin.testing.unified.internal.tabs;

import com.vaadin.testing.unified.Scope;
import com.vaadin.testing.unified.UnifiedElement;
import com.vaadin.testing.unified.UnifiedWrapper;
import com.vaadin.testing.unified.internal.ElementResolver;
import com.vaadin.testing.unified.internal.LazyElement;
import com.vaadin.testing.unified.internal.LazyScope;
import com.vaadin.testing.unified.internal.Wrappers;
import com.vaadin.testing.unified.spi.TabSheetAccess;
import java.util.List;

/** TabSheet behaviour: Auto-Wait and verified tab selection, the selected content as a lazy Scope. */
public final class TabSheetSupport {
    private TabSheetSupport() {}

    public static TabSheetAccess access(UnifiedWrapper tabSheet) {
        return tabSheet.element().unwrap(TabSheetAccess.class);
    }

    public static void select(UnifiedWrapper tabSheet, String label) {
        LazyElement element = Wrappers.lazyElementOf(tabSheet);
        element.act("select tab \"" + label + "\" in", e -> {
            TabSheetAccess access = e.unwrap(TabSheetAccess.class);
            List<String> tabs = access.tabs();
            if (!tabs.contains(label)) {
                throw new IllegalArgumentException("No tab \"" + label + "\" in " + element.describe() + "; tabs: " + tabs);
            }
            // Auto-Wait for the tab itself
            element.run().poll(() -> e.unwrap(TabSheetAccess.class).isEnabled(label) ? Boolean.TRUE : null,
                    () -> "Can't select tab \"" + label + "\" in " + element.describe() + ": the tab is disabled");
            access.select(label);
            if (!label.equals(access.selectedTab())) {
                throw new IllegalStateException("Tab \"" + label + "\" could not be selected in " + element.describe());
            }
        });
    }

    /** The selected tab's content, resolved again on every use: it follows tab switches. */
    public static Scope content(UnifiedWrapper tabSheet) {
        LazyElement sheet = Wrappers.lazyElementOf(tabSheet);
        ElementResolver resolver = new ElementResolver() {
            @Override
            public List<UnifiedElement> resolve() {
                return List.of(access(tabSheet).content());
            }

            @Override
            public String describe() {
                return sheet.describe() + ".content()";
            }
        };
        return new LazyScope(new LazyElement(resolver, -1, sheet.run()));
    }
}
