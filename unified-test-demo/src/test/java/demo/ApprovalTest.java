package demo;

import static com.vaadin.testing.unified.assertion.UnifiedAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.flow.component.html.Span;
import com.vaadin.testing.unified.UnifiedPage;
import com.vaadin.testing.unified.component.UnifiedButton;
import com.vaadin.testing.unified.spring.UnifiedSpringBootTest;
import org.junit.jupiter.api.Test;

/** Hidden components: queries include them on every Testing Tool; visible() leaves them out. */
@UnifiedSpringBootTest
class ApprovalTest {

    @Test
    void aHiddenButtonIsStillFoundButNotVisible(UnifiedPage page) {
        page.navigate(ApprovalView.class);
        UnifiedButton approve = page.find(UnifiedButton.class).withText("Approve").single();

        approve.click();

        assertThat(page.findComponent(Span.class).withId("approval-status").single()).hasText("Approved");
        assertThat(approve).isHidden();
        assertTrue(page.find(UnifiedButton.class).withText("Approve").exists());
        assertFalse(page.find(UnifiedButton.class).withText("Approve").visible().exists());
    }
}
