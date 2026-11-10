package com.vaadin.testing.unified.component;

import com.vaadin.flow.component.html.Div;
import com.vaadin.testing.unified.Findable;
import com.vaadin.testing.unified.Scope;
import com.vaadin.testing.unified.UnifiedComponent;
import com.vaadin.testing.unified.UnifiedWrapper;
import com.vaadin.testing.unified.query.Query;
import com.vaadin.testing.unified.query.TextFilter;

/**
 * A {@code <div>} ({@link Div}), to read its text or search inside it. Bound to its tag, so a browser
 * finds it with one query, unlike {@link UnifiedContainer} (any tag).
 */
@UnifiedComponent(Div.class)
public interface UnifiedDiv extends UnifiedWrapper, Scope, Findable<UnifiedDiv.DivQuery> {

    interface DivQuery extends Query<UnifiedDiv, DivQuery>, TextFilter<DivQuery> {}

    /** {@link com.vaadin.testing.unified.UnifiedElement#getText()}: the DOM text, the same on every tool. */
    default String getText() { return element().getText(); }
}
