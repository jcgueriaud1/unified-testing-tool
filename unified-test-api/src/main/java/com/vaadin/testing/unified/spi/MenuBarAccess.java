package com.vaadin.testing.unified.spi;

import java.util.List;

/**
 * Adapter access to a MenuBar, reached by the core with {@code element.unwrap(MenuBarAccess.class)}. A path
 * is the items' texts from the top level down: {@code ["File", "Export", "PDF"]}.
 */
public interface MenuBarAccess {

    /** Texts of the items under {@code path} (the top-level items for an empty path), in order. */
    List<String> items(List<String> path);

    /** Opens the menus along the path and clicks its last item, as a user would. */
    void click(List<String> path);
}
