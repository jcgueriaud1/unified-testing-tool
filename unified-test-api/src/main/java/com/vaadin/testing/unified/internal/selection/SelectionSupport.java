package com.vaadin.testing.unified.internal.selection;

import com.vaadin.testing.unified.UnifiedWrapper;
import com.vaadin.testing.unified.internal.LazyElement;
import com.vaadin.testing.unified.internal.Wrappers;
import com.vaadin.testing.unified.spi.SelectionAccess;
import java.util.List;

/** Selection field behaviour shared by the selection wrappers: Auto-Wait, then verify the result. */
public final class SelectionSupport {
    private SelectionSupport() {}

    public static SelectionAccess access(UnifiedWrapper field) {
        return field.element().unwrap(SelectionAccess.class);
    }

    public static void select(UnifiedWrapper field, String label) {
        LazyElement element = Wrappers.lazyElementOf(field);
        element.act("select \"" + label + "\" in", e -> {
            SelectionAccess access = e.unwrap(SelectionAccess.class);
            List<String> items = access.items();
            if (!items.contains(label)) {
                throw new IllegalArgumentException("No item \"" + label + "\" in " + element.describe()
                        + "; items: " + items);
            }
            access.select(label);
            if (!access.selectedItems().contains(label)) {
                throw new IllegalStateException("\"" + label + "\" could not be selected in " + element.describe());
            }
        });
    }

    public static void deselect(UnifiedWrapper field, String label) {
        LazyElement element = Wrappers.lazyElementOf(field);
        element.act("deselect \"" + label + "\" in", e -> {
            SelectionAccess access = e.unwrap(SelectionAccess.class);
            if (!access.selectedItems().contains(label)) {
                return;
            }
            access.deselect(label);
            if (access.selectedItems().contains(label)) {
                throw new IllegalStateException("\"" + label + "\" could not be deselected in " + element.describe());
            }
        });
    }

    public static void clear(UnifiedWrapper field) {
        LazyElement element = Wrappers.lazyElementOf(field);
        element.act("clear", e -> {
            SelectionAccess access = e.unwrap(SelectionAccess.class);
            access.clear();
            if (!access.selectedItems().isEmpty()) {
                throw new IllegalStateException("Selection could not be cleared in " + element.describe());
            }
        });
    }

    /** The single selected label, or null. */
    public static String selected(UnifiedWrapper field) {
        List<String> selected = access(field).selectedItems();
        return selected.isEmpty() ? null : selected.get(0);
    }
}
