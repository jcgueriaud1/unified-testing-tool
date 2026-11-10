package com.vaadin.testing.unified.component;

import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.testing.unified.Findable;
import com.vaadin.testing.unified.Scope;
import com.vaadin.testing.unified.UnifiedComponent;
import com.vaadin.testing.unified.UnifiedWrapper;
import com.vaadin.testing.unified.query.Query;
import com.vaadin.testing.unified.query.TextFilter;

/**
 * A {@code <p>} ({@link Paragraph}), to read its text or search inside it. Bound to its tag, so a browser
 * finds it with one query, unlike {@link UnifiedContainer} (any tag).
 */
@UnifiedComponent(Paragraph.class)
public interface UnifiedParagraph extends UnifiedWrapper, Scope, Findable<UnifiedParagraph.ParagraphQuery> {

    interface ParagraphQuery extends Query<UnifiedParagraph, ParagraphQuery>, TextFilter<ParagraphQuery> {}

    /** {@link com.vaadin.testing.unified.UnifiedElement#getText()}: the DOM text, the same on every tool. */
    default String getText() { return element().getText(); }
}
