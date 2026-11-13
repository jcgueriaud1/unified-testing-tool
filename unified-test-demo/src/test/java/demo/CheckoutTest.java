package demo;

import static com.vaadin.testing.unified.assertion.UnifiedAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.vaadin.testing.unified.UnifiedPage;
import com.vaadin.testing.unified.component.UnifiedButton;
import com.vaadin.testing.unified.component.UnifiedContainer;
import com.vaadin.testing.unified.component.UnifiedNotification;
import com.vaadin.testing.unified.component.UnifiedTextField;
import com.vaadin.testing.unified.spring.UnifiedSpringBootTest;
import org.junit.jupiter.api.Test;

/** Narrowing a search to a layout; written once, runs on Browserless and Playwright. */
@UnifiedSpringBootTest
class CheckoutTest {

    // --8<-- [start:picksTheFieldThroughItsContainer]
    @Test
    void picksTheFieldThroughItsContainer(UnifiedPage page) {
        page.navigate(CheckoutView.class);
        UnifiedContainer billing = page.find(UnifiedContainer.class).withTestId("billing").single();
        UnifiedContainer shipping = page.find(UnifiedContainer.class).withId("shipping").single();

        billing.find(UnifiedTextField.class).withLabel("City").single().setValue("Turku");
        shipping.find(UnifiedTextField.class).withLabel("City").single().setValue("Berlin");
        page.find(UnifiedButton.class).withText("Save").single().click();

        assertThat(page.find(UnifiedNotification.class).single()).hasText("Billing: Turku, shipping: Berlin");
        assertThat(billing.find(UnifiedTextField.class).withLabel("City").single()).hasValue("Turku");
    }
    // --8<-- [end:picksTheFieldThroughItsContainer]

    @Test
    void sameLabelOutsideAContainerIsAmbiguous(UnifiedPage page) {
        page.navigate(CheckoutView.class);

        assertEquals(2, page.find(UnifiedTextField.class).withLabel("City").all().size());
        assertEquals(2, page.find(UnifiedContainer.class).withTestId("billing").single()
                .find(UnifiedTextField.class).all().size());
    }

    // --8<-- [start:containersNest]
    @Test
    void containersNest(UnifiedPage page) {
        page.navigate(CheckoutView.class);

        UnifiedContainer buttons = page.find(UnifiedContainer.class).withTestId("footer").single()
                .find(UnifiedContainer.class).withTestId("buttons").single();

        assertEquals(2, buttons.find(UnifiedButton.class).all().size());
        assertThat(buttons).isVisible();
        assertEquals(0, page.find(UnifiedContainer.class).withTestId("billing").single()
                .find(UnifiedButton.class).all().size());
    }
    // --8<-- [end:containersNest]
}
