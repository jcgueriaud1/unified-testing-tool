package demo;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

import com.vaadin.testing.unified.TestingTool;
import com.vaadin.testing.unified.UnifiedPage;
import com.vaadin.testing.unified.component.UnifiedTextField;
import com.vaadin.testing.unified.spring.UnifiedSpringBootTest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

/** Runs a failing Unified UI Test and checks it left evidence: a screenshot or the component tree. */
class FailureEvidenceTest {

    /** Fails on purpose; tagged so Surefire doesn't run it on its own. */
    @UnifiedSpringBootTest
    @Tag("launched-only")
    static class Failing {
        @Test
        void fails(UnifiedPage page) {
            page.navigate(ProfileView.class);
            page.find(UnifiedTextField.class).withLabel("Name").single().setValue("Evidence");
            throw new AssertionError("failing on purpose");
        }
    }

    @Test
    void writesEvidenceOnFailure(@TempDir Path dir) throws Exception {
        TestingTool tool = TestingTool.parse(System.getProperty("vaadin.testing.tool", "browserless"));
        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(selectClass(Failing.class))
                .configurationParameter("vaadin.testing.tool", tool.name().toLowerCase())
                .configurationParameter("vaadin.testing.evidence-dir", dir.toString())
                .build();
        Launcher launcher = LauncherFactory.create();
        SummaryGeneratingListener listener = new SummaryGeneratingListener();
        launcher.execute(request, listener);
        TestExecutionSummary summary = listener.getSummary();

        assertEquals(1, summary.getTotalFailureCount());
        Throwable failure = summary.getFailures().get(0).getException();
        assertEquals("failing on purpose", failure.getMessage());
        String name = Failing.class.getName() + ".fails";
        assertTrue(Arrays.stream(failure.getSuppressed()).anyMatch(s -> s.getMessage().contains(name)),
                "the failure names its evidence: " + Arrays.toString(failure.getSuppressed()));

        if (tool.hasRealBrowser()) {
            byte[] png = Files.readAllBytes(dir.resolve(name + ".png"));
            assertArrayEquals(new byte[] { (byte) 0x89, 'P', 'N', 'G' }, Arrays.copyOf(png, 4));
        } else {
            String tree = Files.readString(dir.resolve(name + ".tree.txt"));
            assertTrue(tree.startsWith("Location: /profile"), tree);
            assertTrue(tree.contains("TextField") && tree.contains("Evidence"), tree);
        }
    }
}
