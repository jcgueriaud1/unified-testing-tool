package demo;

import static com.vaadin.testing.unified.assertion.UnifiedAssertions.assertThat;

import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.Span;
import com.vaadin.testing.unified.UnifiedPage;
import com.vaadin.testing.unified.component.UnifiedContainer;
import com.vaadin.testing.unified.spring.UnifiedSpringBootTest;
import org.junit.jupiter.api.Test;

/** setValueAsUser on a native input: the server sees the value on every Testing Tool. */
@UnifiedSpringBootTest
class CodeTest {

    @Test
    void typesIntoFlowInput(UnifiedPage page) {
        page.navigate(CodeView.class);

        page.findComponent(Input.class).withTestId("code").single().element().setValueAsUser("ABC");

        assertThat(page.findComponent(Span.class).withId("code-value").single()).hasText("ABC");
    }

    @Test
    void typesIntoBareInputElement(UnifiedPage page) {
        page.navigate(CodeView.class);
        UnifiedContainer holder = page.find(UnifiedContainer.class).withTestId("raw").single();

        holder.element().findChildren("input").get(0).setValueAsUser("XYZ");

        assertThat(page.findComponent(Span.class).withId("raw-value").single()).hasText("XYZ");
    }
}
