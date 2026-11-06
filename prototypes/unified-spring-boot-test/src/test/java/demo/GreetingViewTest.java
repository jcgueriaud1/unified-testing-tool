package demo;

import static org.assertj.core.api.Assertions.assertThat;

import demo.unified.TestingTool;
import demo.unified.UnifiedPage;
import demo.unified.UnifiedSpringBootTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.ApplicationContext;

@UnifiedSpringBootTest
class GreetingViewTest {

    @Autowired ApplicationContext spring;

    @Test
    void greets(UnifiedPage page) {
        System.out.printf("UNIFIED-PROOF %s tool=%s context=%s server=%s baseUrl=%s services=%d%n",
                getClass().getSimpleName(), page.testingTool(), System.identityHashCode(spring),
                spring instanceof WebServerApplicationContext, page.baseUrl(), GreetingService.INSTANCES.get());

        page.navigate(MainView.class);
        page.setTextField("Name", "Alice");
        page.clickButton("Greet");
        assertThat(page.textOf("greeting")).isEqualTo("Hello Alice");

        // point 1 + 3: web environment and base URL follow the Testing Tool
        boolean expectServer = page.testingTool() != TestingTool.BROWSERLESS || Boolean.getBoolean("unified.alwaysRandomPort");
        assertThat(spring instanceof WebServerApplicationContext).isEqualTo(expectServer);
        if (page.testingTool() == TestingTool.PLAYWRIGHT) {
            assertThat(page.baseUrl()).matches("http://localhost:\\d+").isNotEqualTo("http://localhost:8080");
        }
    }
}
