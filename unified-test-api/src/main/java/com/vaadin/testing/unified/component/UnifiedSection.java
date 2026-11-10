package com.vaadin.testing.unified.component;

import com.vaadin.flow.component.html.Section;
import com.vaadin.testing.unified.Findable;
import com.vaadin.testing.unified.Scope;
import com.vaadin.testing.unified.UnifiedComponent;
import com.vaadin.testing.unified.UnifiedWrapper;
import com.vaadin.testing.unified.query.Query;
import com.vaadin.testing.unified.query.TextFilter;

/**
 * A {@code <section>} ({@link Section}), to read its text or search inside it. Bound to its tag, so a browser
 * finds it with one query, unlike {@link UnifiedContainer} (any tag).
 */
@UnifiedComponent(Section.class)
public interface UnifiedSection extends UnifiedWrapper, Scope, Findable<UnifiedSection.SectionQuery> {

    interface SectionQuery extends Query<UnifiedSection, SectionQuery>, TextFilter<SectionQuery> {}

    /** {@link com.vaadin.testing.unified.UnifiedElement#getText()}: the DOM text, the same on every tool. */
    default String getText() { return element().getText(); }
}
