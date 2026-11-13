package demo;

import static com.vaadin.testing.unified.assertion.UnifiedAssertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.vaadin.testing.unified.UnifiedPage;
import com.vaadin.testing.unified.component.UnifiedButton;
import com.vaadin.testing.unified.component.UnifiedNotification;
import com.vaadin.testing.unified.component.UnifiedTextField;
import com.vaadin.testing.unified.spring.UnifiedSpringBootTest;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** @MockitoBean works on every Testing Tool: the server runs in the test JVM. */
@UnifiedSpringBootTest
class MockedServiceTest {

    @MockitoBean
    ProfileService service;

    // --8<-- [start:usesTheMock]
    @Test
    void usesTheMock(UnifiedPage page) {
        when(service.save(anyString(), anyString())).thenReturn("Saved by the mock");
        page.navigate(ProfileView.class);

        page.find(UnifiedTextField.class).withLabel("Email").single().setValue("a@b.c");
        page.find(UnifiedButton.class).withText("Save").single().click();

        assertThat(page.find(UnifiedNotification.class).single()).hasText("Saved by the mock");
    }
    // --8<-- [end:usesTheMock]
}
