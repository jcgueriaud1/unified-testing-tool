// PROTOTYPE HACK: Spring Boot decides "embedded server or mock" in SpringBootContextLoader from a
// package-private ContextCustomizer (SpringBootTestAnnotation) built from the test class's own
// @SpringBootTest. There is no public hook to choose it at runtime, so this helper lives in Spring's
// package to swap that customizer. A real implementation needs a proper hook (or upstream change).
package org.springframework.boot.test.context;

import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.CacheAwareContextLoaderDelegate;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.web.WebMergedContextConfiguration;

public final class UnifiedWebEnvironmentAccess {
    private UnifiedWebEnvironmentAccess() {}

    @SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT) static final class RandomPort {}
    @SpringBootTest(webEnvironment = WebEnvironment.MOCK) static final class Mock {}

    public static MergedContextConfiguration withWebEnvironment(MergedContextConfiguration c,
            WebEnvironment env, CacheAwareContextLoaderDelegate delegate) {
        Set<ContextCustomizer> customizers = new LinkedHashSet<>();
        for (ContextCustomizer x : c.getContextCustomizers()) {
            if (!(x instanceof SpringBootTestAnnotation)) {
                customizers.add(x);
            }
        }
        customizers.add(new SpringBootTestAnnotation(env.isEmbedded() ? RandomPort.class : Mock.class));
        MergedContextConfiguration rebuilt = new MergedContextConfiguration(c.getTestClass(), c.getLocations(),
                c.getClasses(), c.getContextInitializerClasses(), c.getActiveProfiles(),
                c.getPropertySourceDescriptors(), c.getPropertySourceProperties(), customizers,
                c.getContextLoader(), delegate, c.getParent());
        return (c instanceof WebMergedContextConfiguration web)
                ? new WebMergedContextConfiguration(rebuilt, web.getResourceBasePath())
                : rebuilt;
    }
}
