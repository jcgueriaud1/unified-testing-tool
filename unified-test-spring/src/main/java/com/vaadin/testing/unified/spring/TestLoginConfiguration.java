package com.vaadin.testing.unified.spring;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;

/** Imported by {@link UnifiedSpringBootTest}: the test login endpoint, when Spring Security is present. */
@Configuration(proxyBeanMethods = false)
@Conditional(TestLoginConfiguration.SpringSecurityPresent.class)
class TestLoginConfiguration {

    @Bean
    TestLoginFilter unifiedTestLoginFilter() {
        return new TestLoginFilter();
    }

    static final class SpringSecurityPresent implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            return TestLogin.isAvailable() && org.springframework.util.ClassUtils.isPresent(
                    "jakarta.servlet.Filter", context.getClassLoader());
        }
    }
}
