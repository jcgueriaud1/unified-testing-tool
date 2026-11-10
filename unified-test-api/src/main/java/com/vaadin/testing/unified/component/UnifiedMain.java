package com.vaadin.testing.unified.component;

import com.vaadin.flow.component.html.Main;
import com.vaadin.testing.unified.Findable;
import com.vaadin.testing.unified.Scope;
import com.vaadin.testing.unified.UnifiedComponent;
import com.vaadin.testing.unified.UnifiedWrapper;
import com.vaadin.testing.unified.query.Query;
import com.vaadin.testing.unified.query.TextFilter;

/**
 * A {@code <main>} ({@link Main}), to read its text or search inside it. Bound to its tag, so a browser
 * finds it with one query, unlike {@link UnifiedContainer} (any tag).
 */
@UnifiedComponent(Main.class)
public interface UnifiedMain extends UnifiedWrapper, Scope, Findable<UnifiedMain.MainQuery> {

    interface MainQuery extends Query<UnifiedMain, MainQuery>, TextFilter<MainQuery> {}

    /** {@link com.vaadin.testing.unified.UnifiedElement#getText()}: the DOM text, the same on every tool. */
    default String getText() { return element().getText(); }
}
