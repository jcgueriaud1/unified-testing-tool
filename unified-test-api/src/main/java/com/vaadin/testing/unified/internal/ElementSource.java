package com.vaadin.testing.unified.internal;

import com.vaadin.flow.component.Component;
import com.vaadin.testing.unified.UnifiedElement;
import com.vaadin.testing.unified.spi.Filter;
import com.vaadin.testing.unified.spi.QuerySpec;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * An immutable description of what to find: component type, tag, adapter filters, core filters (property,
 * predicate) and the parent scope. {@link #resolve} returns the matching adapter elements right now.
 */
public record ElementSource(TestRun run, String wrapperName, Class<? extends Component> componentType, String tag,
        List<Filter> filters, List<Predicate<UnifiedElement>> coreFilters, List<String> coreFilterNames,
        LazyElement parent) implements ElementResolver {

    public ElementSource with(Filter filter) {
        List<Filter> copy = new ArrayList<>(filters);
        copy.add(filter);
        return new ElementSource(run, wrapperName, componentType, tag, List.copyOf(copy), coreFilters,
                coreFilterNames, parent);
    }

    public ElementSource with(Predicate<UnifiedElement> filter, String name) {
        List<Predicate<UnifiedElement>> copy = new ArrayList<>(coreFilters);
        copy.add(filter);
        List<String> names = new ArrayList<>(coreFilterNames);
        names.add(name);
        return new ElementSource(run, wrapperName, componentType, tag, filters, List.copyOf(copy),
                List.copyOf(names), parent);
    }

    /** Matching adapter elements now. Waits for the parent scope (if any) to exist. */
    public List<UnifiedElement> resolve() {
        UnifiedElement parentElement = parent == null ? null : parent.concrete();
        List<UnifiedElement> found = run.session().find(new QuerySpec(componentType, tag, filters, parentElement));
        if (coreFilters.isEmpty()) {
            return found;
        }
        return found.stream().filter(e -> coreFilters.stream().allMatch(f -> f.test(e))).toList();
    }

    public String describe() {
        StringBuilder sb = new StringBuilder(wrapperName);
        filters.forEach(f -> sb.append('.').append(f));
        coreFilterNames.forEach(n -> sb.append('.').append(n));
        if (parent != null) {
            sb.append(" inside ").append(parent.describe());
        }
        return sb.toString();
    }
}
