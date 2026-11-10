package com.vaadin.testing.unified.component;

import com.vaadin.flow.component.html.Footer;
import com.vaadin.testing.unified.Findable;
import com.vaadin.testing.unified.Scope;
import com.vaadin.testing.unified.UnifiedComponent;
import com.vaadin.testing.unified.UnifiedWrapper;
import com.vaadin.testing.unified.query.Query;
import com.vaadin.testing.unified.query.TextFilter;

/**
 * A {@code <footer>} ({@link Footer}), to read its text or search inside it. Bound to its tag, so a browser
 * finds it with one query, unlike {@link UnifiedContainer} (any tag).
 */
@UnifiedComponent(Footer.class)
public interface UnifiedFooter extends UnifiedWrapper, Scope, Findable<UnifiedFooter.FooterQuery> {

    interface FooterQuery extends Query<UnifiedFooter, FooterQuery>, TextFilter<FooterQuery> {}

    /** {@link com.vaadin.testing.unified.UnifiedElement#getText()}: the DOM text, the same on every tool. */
    default String getText() { return element().getText(); }
}
