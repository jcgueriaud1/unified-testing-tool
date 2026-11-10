package com.vaadin.testing.unified.spring;

import com.vaadin.testing.unified.UnifiedUITest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AliasFor;

/**
 * A Unified UI Test of a Spring Boot application. Always starts the application on a random port, on
 * every Testing Tool; the base URL for browser Testing Tools comes from that port. With Spring Security, a
 * test-only login endpoint lets browser Testing Tools log in as the test's user ({@link TestLogin}).
 * <p>
 * {@code @Transactional} on the test fails on browser Testing Tools: the views run outside the test's
 * transaction there ({@link TransactionalCheck}).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({ TestLoginConfiguration.class, CurrentViewConfiguration.class })
@UnifiedUITest
@ExtendWith(TransactionalCheck.class)
public @interface UnifiedSpringBootTest {

    @AliasFor(annotation = SpringBootTest.class)
    String[] properties() default {};

    @AliasFor(annotation = SpringBootTest.class)
    Class<?>[] classes() default {};

    @AliasFor(annotation = SpringBootTest.class)
    String[] args() default {};
}
