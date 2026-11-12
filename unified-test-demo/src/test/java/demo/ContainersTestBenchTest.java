package demo;

import com.vaadin.flow.component.button.testbench.ButtonElement;
import com.vaadin.flow.component.dialog.testbench.DialogElement;
import com.vaadin.flow.component.notification.testbench.NotificationElement;
import com.vaadin.flow.component.tabs.testbench.TabSheetElement;
import com.vaadin.flow.component.textfield.testbench.TextFieldElement;
import com.vaadin.testbench.BrowserTest;
import com.vaadin.testbench.BrowserTestBase;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/** Golden migration of Dialog and TabSheet: unified-test-rewrite turns ContainersTestBenchTest into ContainersMigratedTest. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnabledIfSystemProperty(named = "vaadin.testing.tool", matches = "selenium")
class ContainersTestBenchTest extends BrowserTestBase {

    @LocalServerPort
    int port;

    @BrowserTest
    void changesTheEmailInTheDialog() {
        getDriver().get("http://localhost:" + port + "/profile");

        $(ButtonElement.class).withText("Change email").single().click();
        DialogElement dialog = $(DialogElement.class).waitForFirst();
        Assertions.assertEquals("Change email", dialog.getHeaderTitle());
        dialog.$(TextFieldElement.class).withLabel("Email").single().setValue("bob@example.com");
        dialog.$(ButtonElement.class).withText("Save").single().click();

        Assertions.assertEquals("Email changed to bob@example.com", $(NotificationElement.class).waitForFirst().getText());
    }

    @BrowserTest
    void changesThePasswordInATab() {
        getDriver().get("http://localhost:" + port + "/account");

        TabSheetElement tabs = $(TabSheetElement.class).single();
        Assertions.assertEquals(List.of("Profile", "Security", "Billing"), tabs.getTabLabels());
        tabs.setSelectedTabIndex(tabs.getTab("Security"));
        tabs.getContent().$(ButtonElement.class).withText("Change password").single().click();

        Assertions.assertEquals("Security", tabs.getSelectedTabElement().getLabel());
    }
}
