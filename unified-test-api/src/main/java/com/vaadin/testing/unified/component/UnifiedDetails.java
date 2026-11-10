package com.vaadin.testing.unified.component;

import com.vaadin.flow.component.details.Details;
import com.vaadin.testing.unified.Findable;
import com.vaadin.testing.unified.Scope;
import com.vaadin.testing.unified.UnifiedComponent;
import com.vaadin.testing.unified.UnifiedElement;
import com.vaadin.testing.unified.UnifiedWrapper;
import com.vaadin.testing.unified.internal.ElementResolver;
import com.vaadin.testing.unified.internal.LazyElement;
import com.vaadin.testing.unified.internal.LazyScope;
import com.vaadin.testing.unified.internal.Wrappers;
import com.vaadin.testing.unified.query.Query;
import com.vaadin.testing.unified.spi.DetailsAccess;
import java.util.List;

/**
 * A {@link Details}: a summary that opens and closes its content. A Scope: {@code details.find(..)} searches
 * the content.
 */
@UnifiedComponent(Details.class)
public interface UnifiedDetails extends UnifiedWrapper, Scope, Findable<UnifiedDetails.DetailsQuery> {

    interface DetailsQuery extends Query<UnifiedDetails, DetailsQuery> {
        /** Details whose summary is this text, exactly. */
        default DetailsQuery withSummaryText(String text) {
            return where(e -> text.equals(e.unwrap(DetailsAccess.class).summaryText()));
        }
    }

    // actions (Auto-Wait); clicking the summary, as a user would
    default void open() { if (!isOpen()) { toggle(); } }
    default void close() { if (isOpen()) { toggle(); } }
    default void toggle() { Wrappers.lazyElementOf(this).act("toggle", e -> e.unwrap(DetailsAccess.class).toggle()); }

    // getters (immediate)
    default String getSummaryText() { return element().unwrap(DetailsAccess.class).summaryText(); }
    default boolean isOpen() { return element().unwrap(DetailsAccess.class).isOpen(); }

    /** The content as a Scope, found again on every use. */
    default Scope content() {
        LazyElement details = Wrappers.lazyElementOf(this);
        return new LazyScope(new LazyElement(new ElementResolver() {
            @Override
            public List<UnifiedElement> resolve() {
                return List.of(details.concrete().unwrap(DetailsAccess.class).content());
            }

            @Override
            public String describe() {
                return details.describe() + ".content()";
            }
        }, -1, details.run()));
    }
}
