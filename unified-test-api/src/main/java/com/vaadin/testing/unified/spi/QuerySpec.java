package com.vaadin.testing.unified.spi;

import com.vaadin.flow.component.Component;
import com.vaadin.testing.unified.UnifiedElement;
import java.util.List;

/**
 * What to find. {@code parent} is the adapter's own element to search inside, or null for the whole page.
 * Property and predicate filters are applied by the core afterwards, on {@link UnifiedElement}.
 */
public record QuerySpec(Class<? extends Component> componentType, String tag, List<Filter> filters,
        UnifiedElement parent) {}
