// PROTOTYPE: picks the web environment per run: mock for Browserless, random port for browsers.
package demo.unified;

import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.SpringBootTestContextBootstrapper;
import org.springframework.boot.test.context.UnifiedWebEnvironmentAccess;
import org.springframework.test.context.MergedContextConfiguration;

public class UnifiedSpringBootTestContextBootstrapper extends SpringBootTestContextBootstrapper {

    private static WebEnvironment forThisRun() {
        boolean force = Boolean.getBoolean("unified.alwaysRandomPort"); // fallback variant
        return (force || TestingTool.current().needsServer()) ? WebEnvironment.RANDOM_PORT : WebEnvironment.MOCK;
    }

    @Override
    protected WebEnvironment getWebEnvironment(Class<?> testClass) {
        return forThisRun();   // drives server.port=0 and WebMergedContextConfiguration
    }

    @Override
    protected MergedContextConfiguration processMergedContextConfiguration(MergedContextConfiguration mergedConfig) {
        MergedContextConfiguration processed = super.processMergedContextConfiguration(mergedConfig);
        // drives the loader: embedded server vs mock servlet context
        return UnifiedWebEnvironmentAccess.withWebEnvironment(processed, forThisRun(),
                getCacheAwareContextLoaderDelegate());
    }
}
