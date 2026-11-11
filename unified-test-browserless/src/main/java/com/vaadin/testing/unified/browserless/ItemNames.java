package com.vaadin.testing.unified.browserless;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.dom.Element;
import com.vaadin.testing.unified.internal.BrowserQueries;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The Item Name of a tab or a menu item on Browserless, as {@link BrowserQueries#ITEM_NAME} reads it in a
 * browser: its text (whitespace collapsed, children in a named slot such as a tooltip left out), or its
 * {@code aria-label} when it shows no text.
 */
final class ItemNames {
    private ItemNames() {}

    static String of(Component item) {
        return of(item.getElement());
    }

    static String of(Element item) {
        String text = BrowserQueries.normalize(text(item));
        if (!text.isEmpty()) {
            return text;
        }
        String ariaLabel = item.getAttribute("aria-label");
        return ariaLabel == null ? "" : ariaLabel.strip();
    }

    private static String text(Element e) {
        if (e.isTextNode()) {
            return e.getText();
        }
        return e.getChildren().filter(c -> c.isTextNode() || !c.hasAttribute("slot")).map(ItemNames::text)
                .collect(Collectors.joining());
    }

    /**
     * The first visible item of this name, else the first hidden one (whose use then fails as not usable).
     */
    static <T extends Component> T find(List<T> items, String name, String what) {
        List<T> named = items.stream().filter(i -> name.equals(of(i))).toList();
        return named.stream().filter(Component::isVisible).findFirst()
                .or(() -> named.stream().findFirst())
                .orElseThrow(() -> new IllegalArgumentException("No " + what + "; there are "
                        + items.stream().map(ItemNames::of).toList()));
    }

    /** The position of {@code item} among the visible items, as Browserless Test's testers count. */
    static int visiblePosition(List<? extends Component> items, Component item, String what) {
        if (!item.isVisible()) {
            throw new IllegalStateException(what + " \"" + of(item) + "\" is not visible");
        }
        return items.stream().filter(Component::isVisible).toList().indexOf(item);
    }
}
