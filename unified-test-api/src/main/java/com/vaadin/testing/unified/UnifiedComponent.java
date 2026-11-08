package com.vaadin.testing.unified;

import com.vaadin.flow.component.Component;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Declares the Flow component a wrapper interface wraps (ADR 0003). */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface UnifiedComponent {
    /** The Flow component class. Browserless finds components by it. */
    Class<? extends Component> value();

    /** Web component tag for browser Testing Tools. Empty: read from the component's {@code @Tag}. */
    String tag() default "";

    /** Per-Testing-Tool implementations, for what the Unified Element can't express. */
    ToolImplementation[] overrides() default {};
}
