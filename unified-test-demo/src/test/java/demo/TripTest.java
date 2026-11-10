package demo;

import static com.vaadin.testing.unified.assertion.UnifiedAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.testing.unified.RequiresBrowser;
import com.vaadin.testing.unified.UnifiedPage;
import com.vaadin.testing.unified.component.UnifiedContainer;
import com.vaadin.testing.unified.spring.UnifiedSpringBootTest;
import org.junit.jupiter.api.Test;

@UnifiedSpringBootTest
class TripTest {

    /** getText() is the DOM text: CSS doesn't change it, so it is the same on every Testing Tool. */
    @Test
    void readsTheSameTextOnEveryTool(UnifiedPage page) {
        page.navigate(TripView.class);

        assertThat(page.findComponent(Span.class).withId("attention").single()).hasText("Needs your attention");
        assertThat(page.findComponent(HorizontalLayout.class).withId("total").single()).hasText("Trip total€54.00");
    }

    /** getVisibleText() is what the browser renders. */
    @Test
    @RequiresBrowser
    void readsTheRenderedText(UnifiedPage page) {
        page.navigate(TripView.class);

        assertEquals("NEEDS YOUR ATTENTION",
                page.findComponent(Span.class).withId("attention").single().element().getVisibleText());
        assertEquals("Trip total\n€54.00",
                page.findComponent(HorizontalLayout.class).withId("total").single().element().getVisibleText());
    }

    @Test
    void readsInlineStylesAsDeclared(UnifiedPage page) {
        page.navigate(TripView.class);
        var total = page.findComponent(HorizontalLayout.class).withId("total").single().element();

        assertEquals("var(--lumo-space-m)", total.getInlineStyle("gap"));
        assertEquals("180px", total.getInlineStyle("width"));
        assertEquals(null, total.getInlineStyle("height"));
    }

    /** A class filter on the any-tag container: one selector query, not a check per element of the page. */
    @Test
    void filtersByClassName(UnifiedPage page) {
        page.navigate(TripView.class);

        assertEquals(60, page.find(UnifiedContainer.class).withClassName("trip-card").all().size());
        assertThat(page.find(UnifiedContainer.class).withClassName("selected").single()).hasText("Trip 42");
        assertEquals(1, page.findComponent(Div.class).withClassName("trip-card").withText("Trip 7").all().size());
    }
}
