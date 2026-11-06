package com.vaadin.testing.unified;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Marks a Unified UI Test. Registers the JUnit 6 extension that injects {@link UnifiedPage} as a test
 * method or constructor parameter. No base class.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@ExtendWith(UnifiedUITest.Extension.class)
public @interface UnifiedUITest {

    /** Sketch placeholder for the extension class. */
    abstract class Extension implements org.junit.jupiter.api.extension.ParameterResolver {}
}
