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

    /**
     * What tells this component apart from others with the same tag, applied by every Testing Tool, e.g.
     * {@code ".metric-card"} for a {@code MetricCard extends VerticalLayout}: Browserless finds components by
     * class, but a browser sees only {@code <vaadin-vertical-layout>}. A compound CSS selector of
     * {@code .class}, {@code #id}, {@code [attribute="value"]} and {@code [theme~="value"]}, without spaces.
     */
    String selector() default "";

    /** Per-Testing-Tool implementations, for what the Unified Element can't express. */
    ToolImplementation[] overrides() default {};
}
