package com.vaadin.testing.unified.component;

import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.testing.unified.Findable;
import com.vaadin.testing.unified.UnifiedComponent;
import com.vaadin.testing.unified.UnifiedWrapper;
import com.vaadin.testing.unified.internal.Wrappers;
import com.vaadin.testing.unified.query.Query;
import com.vaadin.testing.unified.spi.MenuBarAccess;
import java.util.List;

/**
 * A {@link MenuBar}. Items are identified by their text, from the top level down:
 * {@code menu.clickItem("File", "Export", "PDF")} opens File, then Export, and clicks PDF.
 */
@UnifiedComponent(MenuBar.class)
public interface UnifiedMenuBar extends UnifiedWrapper, Findable<UnifiedMenuBar.MenuBarQuery> {

    interface MenuBarQuery extends Query<UnifiedMenuBar, MenuBarQuery> {
        /** Menu bars with a top-level item of this text. */
        default MenuBarQuery withItem(String text) {
            return where(e -> e.unwrap(MenuBarAccess.class).items(List.of()).contains(text));
        }
    }

    // actions (Auto-Wait): opens the menus along the path, as a user would
    default void clickItem(String topLevel, String... nested) {
        List<String> path = path(topLevel, nested);
        Wrappers.lazyElementOf(this).act("click " + path + " in", e -> e.unwrap(MenuBarAccess.class).click(path));
    }

    // getters (immediate)
    /** The top-level items' texts. */
    default List<String> getItems() { return element().unwrap(MenuBarAccess.class).items(List.of()); }

    /** The texts of the items in the sub-menu at this path; opens the menus in a browser. */
    default List<String> getItems(String topLevel, String... nested) {
        return element().unwrap(MenuBarAccess.class).items(path(topLevel, nested));
    }

    private static List<String> path(String first, String... rest) {
        java.util.List<String> path = new java.util.ArrayList<>();
        path.add(first);
        path.addAll(List.of(rest));
        return List.copyOf(path);
    }
}
