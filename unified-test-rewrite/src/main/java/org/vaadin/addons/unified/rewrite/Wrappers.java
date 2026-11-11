package org.vaadin.addons.unified.rewrite;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.openrewrite.Cursor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

/**
 * Which Wrapper replaces which TestBench element ({@code docs/spec/appendix-mapping.md}), and the TestBench
 * calls the Wrapper has with the same name and meaning. A lookup is converted only when every call made on it
 * is one of these; otherwise it stays TestBench, behind the Escape Hatch.
 */
final class Wrappers {

    /** A Wrapper and the TestBench calls it shares: query filters, and methods on the element found. */
    record Wrapper(String fqn, Set<String> filters, Set<String> methods, Conversion conversion) {

        Wrapper(String fqn, Set<String> filters, Set<String> methods) {
            this(fqn, filters, methods, (call, cursor) -> false);
        }

        /** Whether a call on the element is one the Wrapper has, by name or through a recipe's conversion. */
        boolean supports(J.MethodInvocation call, Cursor callCursor) {
            return methods.contains(call.getSimpleName()) || conversion.converts(call, callCursor);
        }

        String simpleName() {
            return fqn.substring(fqn.lastIndexOf('.') + 1);
        }
    }

    /** A TestBench call a recipe converts to a differently named Wrapper call, such as {@code getCell(..)}. */
    interface Conversion {
        boolean converts(J.MethodInvocation call, Cursor callCursor);
    }

    /** TestBench query terminals; {@code first()} and {@code waitForFirst()} become {@code single()}. */
    static final Set<String> TERMINALS = Set.of("single", "first", "waitForFirst");

    private static final Set<String> COMMON_FILTERS = Set.of("withId", "withClassName", "withTheme");

    private static final Map<String, Wrapper> BY_ELEMENT = Map.of(
            "com.vaadin.flow.component.textfield.testbench.TextFieldElement",
            new Wrapper("com.vaadin.testing.unified.component.UnifiedTextField",
                    with(COMMON_FILTERS, "withLabel"),
                    Set.of("setValue", "getValue", "clear", "getLabel", "getPlaceholder", "getHelperText",
                            "getErrorMessage", "isEnabled")),
            "com.vaadin.flow.component.button.testbench.ButtonElement",
            new Wrapper("com.vaadin.testing.unified.component.UnifiedButton",
                    with(COMMON_FILTERS, "withText"),
                    Set.of("click", "getText", "isEnabled")),
            "com.vaadin.flow.component.notification.testbench.NotificationElement",
            new Wrapper("com.vaadin.testing.unified.component.UnifiedNotification",
                    with(COMMON_FILTERS, "withText"),
                    Set.of("getText", "isOpen")),
            "com.vaadin.flow.component.grid.testbench.GridElement",
            new Wrapper("com.vaadin.testing.unified.component.UnifiedGrid",
                    COMMON_FILTERS,
                    // row and rows are what GridCells writes for getCell(..)
                    Set.of("getRowCount", "row", "rows"),
                    GridCells::converts));

    private Wrappers() {
    }

    /** The Wrapper for the element, or {@code null} when it has none yet. */
    static Wrapper forElement(String elementFqn) {
        return BY_ELEMENT.get(elementFqn);
    }

    /**
     * The Wrapper an element expression converts to, when it is a lookup the Wrapper can express:
     * {@code $(XElement.class)}, then only the Wrapper's filters, then a terminal. Else {@code null}.
     */
    static Wrapper ofLookup(Expression element) {
        if (!(element instanceof J.MethodInvocation terminal) || !TERMINALS.contains(terminal.getSimpleName())) {
            return null;
        }
        Expression e = terminal.getSelect();
        List<String> filters = new ArrayList<>();
        while (e instanceof J.MethodInvocation call && !call.getSimpleName().equals("$")) {
            filters.add(call.getSimpleName());
            e = call.getSelect();
        }
        if (!(e instanceof J.MethodInvocation dollar) || dollar.getSelect() != null
                || !(dollar.getArguments().get(0) instanceof J.FieldAccess classLiteral)
                || !(classLiteral.getTarget().getType() instanceof JavaType.FullyQualified type)) {
            return null;
        }
        Wrapper wrapper = forElement(type.getFullyQualifiedName());
        return wrapper != null && wrapper.filters().containsAll(filters) ? wrapper : null;
    }

    private static Set<String> with(Set<String> common, String... more) {
        return java.util.stream.Stream.concat(common.stream(), Set.of(more).stream())
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }
}
