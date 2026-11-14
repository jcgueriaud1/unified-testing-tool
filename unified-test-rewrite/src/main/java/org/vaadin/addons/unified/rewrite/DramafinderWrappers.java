package org.vaadin.addons.unified.rewrite;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

/**
 * Which Wrapper replaces which Dramafinder element ({@code docs/spec/appendix-mapping.md}), the Dramafinder lookups
 * it can express, and the element methods and assertions it has (ADR 0006). A lookup is converted only when every
 * call made on what it finds is one of these; otherwise it stays Dramafinder, behind the Escape Hatch.
 */
final class DramafinderWrappers {

    private static final String ELEMENT = "org.vaadin.addons.dramafinder.element.";
    private static final String COMPONENT = "com.vaadin.testing.unified.component.";
    static final String QUERIES = "org.vaadin.addons.dramafinder.element.query.ElementQueries";

    /** A Wrapper: its filters, the element methods it has (Dramafinder name to Unified name) and assertions. */
    record Wrapper(String fqn, Set<String> filters, Map<String, String> methods, Map<String, String> assertions) {

        /** The name a declaration uses, {@code UnifiedGrid.UnifiedGridRow} for a nested type. */
        String simpleName() {
            return fqn.substring(fqn.lastIndexOf('.') + 1).replace('$', '.');
        }

        /** The type to import: the outer type of a nested one. */
        String importFqn() {
            int nested = fqn.indexOf('$');
            return nested < 0 ? fqn : fqn.substring(0, nested);
        }

        boolean supports(String method) {
            return methods.containsKey(method) || assertions.containsKey(method);
        }
    }

    /** One filter of a lookup, in Unified terms. */
    record Filter(String name, List<Expression> arguments) {
    }

    /**
     * A Dramafinder lookup the Wrapper can express: {@code page.find(W.class)}, the filters, then {@code single()},
     * or {@code all().get(index)} when {@code index} is set. {@code marker} is the Migration Marker it needs, if any.
     */
    record Lookup(Wrapper wrapper, List<Filter> filters, Expression index, String marker) {
    }

    private static final Set<String> COMMON_FILTERS = Set.of("withAccessibleName", "withAccessibleNameContaining",
            "withTestId", "withId", "withClassName");
    private static final Set<String> LABEL_FILTERS = Set.of("withLabel", "withLabelContaining");
    private static final Set<String> TEXT_FILTERS = Set.of("withText", "withTextContaining");

    private static final Map<String, String> VISIBILITY = Map.of("assertVisible", "isVisible", "assertHidden", "isHidden",
            "assertEnabled", "isEnabled", "assertDisabled", "isDisabled");
    private static final Map<String, String> FIELD_ASSERTIONS = with(VISIBILITY, Map.of("assertValue", "hasValue",
            "assertLabel", "hasLabel", "assertValid", "isValid", "assertInvalid", "isInvalid",
            "assertErrorMessage", "hasErrorMessage"));
    private static final Map<String, String> TEXT_FIELD_METHODS = same("setValue", "getValue", "clear", "getLabel",
            "getPlaceholder", "getHelperText", "isEnabled", "isVisible");

    private static final Map<String, Wrapper> BY_ELEMENT = Map.of(
            ELEMENT + "TextFieldElement", new Wrapper(COMPONENT + "UnifiedTextField",
                    union(COMMON_FILTERS, LABEL_FILTERS, Set.of("withPlaceholder")), TEXT_FIELD_METHODS, FIELD_ASSERTIONS),
            ELEMENT + "TextAreaElement", new Wrapper(COMPONENT + "UnifiedTextArea",
                    union(COMMON_FILTERS, LABEL_FILTERS, Set.of("withPlaceholder")), TEXT_FIELD_METHODS, FIELD_ASSERTIONS),
            ELEMENT + "ButtonElement", new Wrapper(COMPONENT + "UnifiedButton", union(COMMON_FILTERS, TEXT_FILTERS),
                    same("click", "getText", "isEnabled", "isVisible"), with(VISIBILITY, Map.of("assertText", "hasText"))),
            ELEMENT + "NotificationElement", new Wrapper(COMPONENT + "UnifiedNotification", union(COMMON_FILTERS, TEXT_FILTERS),
                    same("isOpen"), Map.of("assertOpen", "isOpen", "assertContent", "hasTextContaining")),
            ELEMENT + "CheckboxElement", new Wrapper(COMPONENT + "UnifiedCheckbox", union(COMMON_FILTERS, LABEL_FILTERS),
                    same("check", "uncheck", "setChecked", "isChecked", "getLabel", "getHelperText", "isEnabled", "isVisible"),
                    with(VISIBILITY, Map.of("assertChecked", "isChecked", "assertNotChecked", "isNotChecked",
                            "assertLabel", "hasLabel", "assertIndeterminate", "isIndeterminate"))),
            ELEMENT + "DialogElement", new Wrapper(COMPONENT + "UnifiedDialog", union(COMMON_FILTERS, Set.of("withHeaderTitle")),
                    Map.of("isOpen", "isOpen", "getHeaderText", "getHeaderTitle", "closeWithEscape", "close"),
                    Map.of("assertOpen", "isOpen", "assertClosed", "isClosed", "assertHeaderText", "hasHeaderTitle")),
            ELEMENT + "GridElement", new Wrapper(COMPONENT + "UnifiedGrid", COMMON_FILTERS,
                    Map.of("getTotalRowCount", "getRowCount"), Map.of("assertRowCount", "hasRowCount")));

    /** A grid row: what {@code grid.findRow(index).orElseThrow()} becomes, {@code grid.row(index)}. */
    static final Wrapper ROW = new Wrapper(COMPONENT + "UnifiedGrid$UnifiedGridRow", Set.of(),
            same("select", "deselect", "click", "doubleClick"), Map.of("assertSelected", "isSelected"));

    /** Static factories: the Unified filter each one is. {@code getByLabel} on a button is its accessible name. */
    private static final Map<String, String> FACTORIES = Map.of("getByLabel", "withLabel", "getByLabelContaining",
            "withLabelContaining", "getByText", "withText", "getByTextContaining", "withTextContaining", "getById",
            "withId", "getByTestId", "withTestId", "getByHeaderText", "withHeaderTitle");

    /** Dramafinder 1.x label and text factories, and the Unified filter that matches at least as much. */
    private static final Map<String, String> VERSION_1_FACTORIES = Map.of("getByLabel", "withAccessibleNameContaining",
            "getByText", "withTextContaining");
    /** Dramafinder 1.x substring factories with no substring filter in the Unified API. */
    private static final Set<String> VERSION_1_UNMAPPED = Set.of("getByHeaderText");

    /** Factories that match a substring and take the first match even in Dramafinder 2.x. */
    private static final Set<String> FIRST_MATCH = Set.of(ELEMENT + "NotificationElement#getByText");

    /** Query API filters with the same name and meaning in the Unified API. */
    private static final Set<String> QUERY_FILTERS = union(COMMON_FILTERS, LABEL_FILTERS, TEXT_FILTERS,
            Set.of("withPlaceholder"));

    private DramafinderWrappers() {
    }

    static Wrapper forElement(JavaType type) {
        JavaType.FullyQualified fq = TypeUtils.asFullyQualified(type);
        return fq == null ? null : BY_ELEMENT.get(fq.getFullyQualifiedName());
    }

    static String elementOf(String wrapperFqn) {
        return BY_ELEMENT.entrySet().stream().filter(e -> e.getValue().fqn().equals(wrapperFqn)).map(Map.Entry::getKey)
                .findFirst().orElse(null);
    }

    /**
     * The lookup the expression is, when the Wrapper can express it on the test's page; else {@code null}.
     * {@code version2}: the project uses Dramafinder 2.x, whose label and text factories are exact.
     */
    static Lookup ofLookup(Expression e, boolean version2) {
        if (!(e instanceof J.MethodInvocation call) || call.getMethodType() == null) {
            return null;
        }
        Lookup factory = ofFactory(call, version2);
        return factory != null ? factory : ofQuery(call);
    }

    /** {@code XElement.getByLabel(page, "Name")} and the other static factories. */
    private static Lookup ofFactory(J.MethodInvocation call, boolean version2) {
        JavaType.FullyQualified declaring = call.getMethodType().getDeclaringType();
        Wrapper wrapper = BY_ELEMENT.get(declaring.getFullyQualifiedName());
        if (wrapper == null || !call.getMethodType().hasFlags(org.openrewrite.java.tree.Flag.Static)
                || call.getArguments().isEmpty() || !Dramafinder.isPageField(call.getArguments().get(0))) {
            return null;
        }
        String name = call.getSimpleName();
        if (name.equals("get") && call.getArguments().size() == 1 && declaring.getFullyQualifiedName().endsWith("GridElement")) {
            return new Lookup(wrapper, List.of(), null, null); // the first grid; a second one fails loudly
        }
        String filter = name.equals("getByLabel") && wrapper.simpleName().equals("UnifiedButton")
                ? "withAccessibleName" : FACTORIES.get(name);
        if (filter == null || call.getArguments().size() != 2
                || !TypeUtils.isString(call.getArguments().get(1).getType()) || !wrapper.filters().contains(filter)) {
            return null;
        }
        String marker = null;
        if (!version2 && VERSION_1_FACTORIES.containsKey(name)
                && !FIRST_MATCH.contains(declaring.getFullyQualifiedName() + "#" + name)) {
            // 1.x: a case-insensitive substring of the label, aria-label or placeholder, and the first match
            filter = VERSION_1_FACTORIES.get(name);
            if (!wrapper.filters().contains(filter)) {
                return null;
            }
            String exact = name.equals("getByText") ? "withText" : "withLabel";
            marker = "was 1.x " + name + "(..), a case-insensitive substring and first match; tighten it to "
                    + exact + "(..) once the test passes";
        } else if (!version2 && VERSION_1_UNMAPPED.contains(name)) {
            return null; // 1.x matched a substring, and the Unified filter is exact
        } else if (FIRST_MATCH.contains(declaring.getFullyQualifiedName() + "#" + name)) {
            filter = "withTextContaining";
            marker = "was " + name + "(..), a substring and first match; single() fails when several components match";
        }
        return new Lookup(wrapper, List.of(new Filter(filter, List.of(call.getArguments().get(1)))), null, marker);
    }

    /** {@code find(page, XElement.class).withLabel("Name").single()} and the other query terminals. */
    private static Lookup ofQuery(J.MethodInvocation terminal) {
        String name = terminal.getSimpleName();
        if (!Set.of("single", "first", "get").contains(name)) {
            return null;
        }
        List<Filter> filters = new ArrayList<>();
        Expression e = terminal.getSelect();
        while (e instanceof J.MethodInvocation call && !isFind(call)) {
            if (!QUERY_FILTERS.contains(call.getSimpleName())) {
                return null;
            }
            filters.add(0, new Filter(call.getSimpleName(), call.getArguments()));
            e = call.getSelect();
        }
        if (!(e instanceof J.MethodInvocation find) || !isFind(find) || find.getArguments().size() != 2
                || !Dramafinder.isPageField(find.getArguments().get(0))
                || !(find.getArguments().get(1) instanceof J.FieldAccess literal)) {
            return null;
        }
        Wrapper wrapper = forElement(literal.getTarget().getType());
        if (wrapper == null || filters.stream().anyMatch(f -> !wrapper.filters().contains(f.name()))) {
            return null;
        }
        return switch (name) {
            case "first" -> new Lookup(wrapper, filters, null, "was first(); single() fails when several components match");
            case "get" -> new Lookup(wrapper, filters, terminal.getArguments().get(0), null);
            default -> new Lookup(wrapper, filters, null, null);
        };
    }

    private static boolean isFind(J.MethodInvocation call) {
        return call.getSimpleName().equals("find") && call.getMethodType() != null
                && TypeUtils.isOfClassType(call.getMethodType().getDeclaringType(), QUERIES);
    }

    /** The lookup as template code on {@code #{any(UnifiedPage)}}, and the arguments for its placeholders. */
    static String code(Lookup lookup, List<Object> parameters) {
        StringBuilder code = new StringBuilder("#{any(" + TestBenchSetupToUnified.UNIFIED_PAGE + ")}.find("
                + lookup.wrapper().simpleName() + ".class)");
        parameters.add(Stubs.page());
        for (Filter f : lookup.filters()) {
            code.append('.').append(f.name()).append('(');
            code.append(String.join(", ", java.util.Collections.nCopies(f.arguments().size(), "#{any()}")));
            code.append(')');
            parameters.addAll(f.arguments());
        }
        if (lookup.index() != null) {
            code.append(".all().get(#{any(int)})");
            parameters.add(lookup.index());
        } else {
            code.append(".single()");
        }
        return code.toString();
    }

    @SafeVarargs
    private static Set<String> union(Set<String>... sets) {
        return Stream.of(sets).flatMap(Set::stream).collect(Collectors.toUnmodifiableSet());
    }

    private static Map<String, String> same(String... names) {
        return Stream.of(names).collect(Collectors.toUnmodifiableMap(n -> n, n -> n));
    }

    private static Map<String, String> with(Map<String, String> common, Map<String, String> more) {
        Map<String, String> all = new HashMap<>(common);
        all.putAll(more);
        return Map.copyOf(all);
    }
}
