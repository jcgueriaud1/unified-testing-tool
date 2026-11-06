package demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import demo.unified.UnifiedPage;
import demo.unified.UnifiedSpringBootTest;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@UnifiedSpringBootTest
class MockedGreetingViewTest {

    @MockitoBean GreetingService service;   // point 4: works on every Testing Tool

    @Test
    void usesMockedService(UnifiedPage page) {
        when(service.greet(anyString())).thenReturn("Mocked greeting");
        System.out.printf("UNIFIED-PROOF %s tool=%s%n", getClass().getSimpleName(), page.testingTool());

        page.navigate(MainView.class);
        page.setTextField("Name", "Alice");
        page.clickButton("Greet");
        assertThat(page.textOf("greeting")).isEqualTo("Mocked greeting");
    }
}
