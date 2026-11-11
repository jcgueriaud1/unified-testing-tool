package com.vaadin.testing.unified.spi;

import java.util.List;

/**
 * Adapter access to a MenuBar, reached by the core with {@code element.unwrap(MenuBarAccess.class)}. A path
 * is the items' Item Names from the top level down: {@code ["File", "Export", "PDF"]}. An item's name is its
 * text, or its {@code aria-label} when it shows no text (an icon-only "⋮" item).
 */
public interface MenuBarAccess {

    /** Item Names of the items under {@code path} (the top-level items for an empty path), in order. */
    List<String> items(List<String> path);

    /** Opens the menus along the path and clicks its last item, as a user would. */
    void click(List<String> path);

    /** Whether the user can click the last item of the path; opens the menus along it in a browser. */
    boolean isEnabled(List<String> path);
}
