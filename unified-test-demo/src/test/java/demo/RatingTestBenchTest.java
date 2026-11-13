package demo;

import com.vaadin.testbench.BrowserTest;
import com.vaadin.testbench.BrowserTestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/** Golden migration of a test using a custom TestBench element; its twin is an input of the skill evaluation. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnabledIfSystemProperty(named = "vaadin.testing.tool", matches = "selenium")
class RatingTestBenchTest extends BrowserTestBase {

    @LocalServerPort
    int port;

    @BrowserTest
    void ratesTheFood() {
        getDriver().get("http://localhost:" + port + "/addon");

        RatingElement food = $(RatingElement.class).first();
        food.rate(3);

        Assertions.assertEquals(3, food.getValue());
    }
}
