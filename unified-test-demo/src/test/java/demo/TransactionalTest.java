package demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

import com.vaadin.testing.unified.UnifiedPage;
import com.vaadin.testing.unified.spring.UnifiedSpringBootTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;
import org.springframework.transaction.annotation.Transactional;

/** A @Transactional Unified UI Test fails on a browser Testing Tool, before it starts, with the reason. */
class TransactionalTest {

    @UnifiedSpringBootTest
    @Transactional
    @Tag("launched-only")
    static class TransactionalUiTest {
        @Test
        void usesTheDatabase(UnifiedPage page) {
            page.navigate(HomeView.class);
        }
    }

    @Test
    void failsOnABrowserTestingTool() {
        Launcher launcher = LauncherFactory.create();
        SummaryGeneratingListener listener = new SummaryGeneratingListener();
        launcher.execute(LauncherDiscoveryRequestBuilder.request().selectors(selectClass(TransactionalUiTest.class))
                .configurationParameter("vaadin.testing.tool", "playwright").build(), listener);
        TestExecutionSummary summary = listener.getSummary();

        assertEquals(1, summary.getTotalFailureCount());
        Throwable failure = summary.getFailures().get(0).getException();
        String messages = "";
        for (Throwable t = failure; t != null; t = t.getCause()) {
            messages += t.getMessage() + "\n";
        }
        assertTrue(messages.contains("@Transactional on TransactionalUiTest doesn't work on PLAYWRIGHT"), messages);
    }
}
