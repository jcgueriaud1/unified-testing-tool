package com.vaadin.testing.unified.spring;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.ClassUtils;

/** Imported by {@link UnifiedSpringBootTest}: the test-only current-view endpoint for browser Testing Tools. */
@Configuration(proxyBeanMethods = false)
@Conditional(CurrentViewConfiguration.VaadinServletApp.class)
class CurrentViewConfiguration {

    @Bean
    CurrentViewFilter unifiedCurrentViewFilter() {
        return new CurrentViewFilter();
    }

    static final class VaadinServletApp implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            return ClassUtils.isPresent("jakarta.servlet.Filter", context.getClassLoader())
                    && ClassUtils.isPresent("com.vaadin.flow.server.VaadinSession", context.getClassLoader());
        }
    }
}
