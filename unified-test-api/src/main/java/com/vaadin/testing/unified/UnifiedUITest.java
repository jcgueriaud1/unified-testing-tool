package com.vaadin.testing.unified;

import com.vaadin.testing.unified.internal.UnifiedExtension;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Marks a Unified UI Test. Registers the JUnit 6 extension that injects {@link UnifiedPage} as a test
 * method or constructor parameter. No base class.
 * <p>
 * The Testing Tool comes from the JUnit configuration parameter {@code vaadin.testing.tool}
 * ({@code browserless} by default, or {@code playwright}).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@ExtendWith(UnifiedExtension.class)
public @interface UnifiedUITest {}
