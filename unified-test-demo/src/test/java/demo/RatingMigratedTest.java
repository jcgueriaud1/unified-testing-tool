package demo;

import com.vaadin.testing.unified.RequiresTestingTool;
import com.vaadin.testing.unified.TestingTool;
import com.vaadin.testing.unified.UnifiedPage;
import com.vaadin.testing.unified.selenium.TestBenchEscapeHatch;
import com.vaadin.testing.unified.spring.UnifiedSpringBootTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** Golden migration of a test using a custom TestBench element; its twin is an input of the skill evaluation. */
@UnifiedSpringBootTest
class RatingMigratedTest {

    @Test
    @RequiresTestingTool(TestingTool.SELENIUM)
    void ratesTheFood(UnifiedPage page) {
        TestBenchEscapeHatch testBench = TestBenchEscapeHatch.of(page);
        page.navigate(AddonView.class);

        // TODO unified: TestBench Escape Hatch: $(RatingElement.class), a custom element: write a Wrapper for it (ADR 0003)
        RatingElement food = testBench.$(RatingElement.class).first();
        food.rate(3);

        Assertions.assertEquals(3, food.getValue());
    }
}
