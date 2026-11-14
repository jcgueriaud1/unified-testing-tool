package org.vaadin.addons.unified.rewrite;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.openrewrite.Cursor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

/**
 * What the Browserless Migration Recipe knows about Browserless Test (ADR 0005): its test base classes, which
 * Wrapper replaces which Flow component, and when a lookup, and the tester and component calls made on what it
 * finds, can all be said with the Unified API.
 */
final class Browserless {

    static final String SPRING_BROWSERLESS_TEST = "com.vaadin.browserless.SpringBrowserlessTest";
    static final String BROWSERLESS_TEST = "com.vaadin.browserless.BrowserlessTest";
    static final String ESCAPE_HATCH = "com.vaadin.testing.unified.browserless.BrowserlessEscapeHatch";
    static final String UI_CONTEXT = "com.vaadin.browserless.BrowserlessUIContext";
    static final String EXTENSION = "com.vaadin.browserless.BrowserlessExtension";
    static final String VARIABLE = "browserless";

    /**
     * The types whose methods a Browserless test calls without a receiver. TestBench UI Unit's are here too: the
     * UI Unit rename changes the types the test names, but a method's type can keep its UI Unit declaring type.
     */
    private static final Set<String> TEST_BASE_TYPES = Set.of(SPRING_BROWSERLESS_TEST, BROWSERLESS_TEST,
            "com.vaadin.browserless.BaseBrowserlessTest", "com.vaadin.browserless.TesterWrappers",
            "com.vaadin.testbench.unit.BaseUIUnitTest", "com.vaadin.testbench.unit.TesterWrappers",
            "com.vaadin.testbench.unit.UIUnitTest", "com.vaadin.testbench.unit.UIUnit4Test",
            "com.vaadin.testbench.unit.SpringUIUnitTest", "com.vaadin.testbench.unit.SpringUIUnit4Test");

    /** The test base's lookups: {@code $(X.class)} and friends, all starting a query on the current UI or view. */
    private static final Set<String> LOOKUPS = Set.of("$", "$view", "find", "findInView");

    /** Query terminals; all but {@code single()} become {@code single()} with a Migration Marker. */
    static final Set<String> TERMINALS = Set.of("single", "first", "last", "atIndex");

    /** A Wrapper, the Browserless filters it has (and their Unified names), tester calls and component getters. */
    record Wrapper(String fqn, Map<String, String> filters, Map<String, String> testerCalls,
            Map<String, String> getters) {

        String simpleName() {
            return fqn.substring(fqn.lastIndexOf('.') + 1);
        }

        boolean isButton() {
            return fqn.endsWith(".UnifiedButton");
        }
    }

    private static final Map<String, String> COMMON_FILTERS = Map.of("withId", "withId", "withClassName",
            "withClassName", "withTheme", "withTheme", "withTestId", "withTestId");
    private static final Map<String, String> FIELD_FILTERS = with(COMMON_FILTERS, Map.of("withLabel", "withLabel",
            "withCaption", "withLabel", "withPlaceholder", "withPlaceholder"));
    private static final Map<String, String> FIELD_GETTERS = Map.of("getValue", "getValue", "getLabel", "getLabel",
            "getPlaceholder", "getPlaceholder", "getHelperText", "getHelperText", "getErrorMessage",
            "getErrorMessage", "isInvalid", "isInvalid", "isEnabled", "isEnabled");
    private static final Map<String, String> TEXT_INPUT = Map.of("setValue", "setValue", "clear", "clear");

    private static final Map<String, Wrapper> BY_COMPONENT = Map.of(
            "com.vaadin.flow.component.textfield.TextField", new Wrapper("com.vaadin.testing.unified.component.UnifiedTextField",
                    FIELD_FILTERS, TEXT_INPUT, FIELD_GETTERS),
            "com.vaadin.flow.component.textfield.TextArea", new Wrapper("com.vaadin.testing.unified.component.UnifiedTextArea",
                    FIELD_FILTERS, TEXT_INPUT, FIELD_GETTERS),
            // withCaption on a button is its text (ADR 0001); the recipe leaves a Migration Marker
            "com.vaadin.flow.component.button.Button", new Wrapper("com.vaadin.testing.unified.component.UnifiedButton",
                    with(COMMON_FILTERS, Map.of("withText", "withText", "withCaption", "withText")),
                    Map.of("click", "click"), Map.of("getText", "getText", "isEnabled", "isEnabled")),
            "com.vaadin.flow.component.notification.Notification", new Wrapper("com.vaadin.testing.unified.component.UnifiedNotification",
                    with(COMMON_FILTERS, Map.of("withText", "withText")),
                    Map.of("getText", "getText"), Map.of("isOpened", "isOpen")),
            "com.vaadin.flow.component.checkbox.Checkbox", new Wrapper("com.vaadin.testing.unified.component.UnifiedCheckbox",
                    with(COMMON_FILTERS, Map.of("withLabel", "withLabel", "withCaption", "withLabel")),
                    Map.of("check", "check", "uncheck", "uncheck", "isChecked", "isChecked"),
                    Map.of("getValue", "isChecked", "getLabel", "getLabel", "isEnabled", "isEnabled")),
            "com.vaadin.flow.component.dialog.Dialog", new Wrapper("com.vaadin.testing.unified.component.UnifiedDialog",
                    COMMON_FILTERS, Map.of(), Map.of("isOpened", "isOpen", "getHeaderTitle", "getHeaderTitle")));

    private Browserless() {
    }

    /**
     * A Browserless test class: one that extends a Browserless Test test base class, directly or through the
     * project's own, or one that holds a Browserless context or extension in a field (the context API).
     */
    static boolean isBrowserlessClass(J.ClassDeclaration c) {
        return c.getType() != null && (TypeUtils.isAssignableTo(SPRING_BROWSERLESS_TEST, c.getType())
                || TypeUtils.isAssignableTo(BROWSERLESS_TEST, c.getType()) || isContextClass(c));
    }

    /** A class holding a Browserless context or extension in a field, instead of extending a test base class. */
    static boolean isContextClass(J.ClassDeclaration c) {
        return c.getBody().getStatements().stream().anyMatch(s -> s instanceof J.VariableDeclarations v
                && CONTEXT_TYPES.stream().anyMatch(t -> TypeUtils.isAssignableTo(t, v.getType())));
    }

    /** The context API's types a test class holds: its application, user and window, and the JUnit extension. */
    static final List<String> CONTEXT_TYPES = List.of("com.vaadin.browserless.BrowserlessApplicationContext",
            "com.vaadin.browserless.BrowserlessUserContext", UI_CONTEXT, EXTENSION);

    /**
     * A handle: an identifier holding a window ({@code BrowserlessUIContext}) or the JUnit extension, whose
     * methods are the test base's ({@code navigate}, {@code find}, {@code test}, {@code findTextField()}, ...).
     */
    static boolean isHandle(Expression e) {
        return e instanceof J.Identifier id && id.getType() != null
                && (TypeUtils.isAssignableTo(UI_CONTEXT, id.getType()) || TypeUtils.isAssignableTo(EXTENSION, id.getType()));
    }

    /**
     * Whether the class hands itself ({@code this}) to something, such as a page object taking the test: it would
     * no longer compile without the test base class, so the recipe leaves the class as it is.
     */
    static boolean passesItself(J.ClassDeclaration c) {
        AtomicBoolean passes = new AtomicBoolean();
        new JavaIsoVisitor<AtomicBoolean>() {
            @Override
            public J.Identifier visitIdentifier(J.Identifier identifier, AtomicBoolean found) {
                Object parent = getCursor().getParentTreeCursor().getValue();
                if (identifier.getSimpleName().equals("this")
                        && (parent instanceof J.NewClass n && n.getArguments().contains(identifier)
                                || parent instanceof J.MethodInvocation call && call.getArguments().contains(identifier))) {
                    found.set(true);
                }
                return identifier;
            }
        }.visit(c.getBody(), passes);
        return passes.get();
    }

    static boolean isTestBase(JavaType type) {
        return TypeUtils.isOfClassType(type, SPRING_BROWSERLESS_TEST) || TypeUtils.isOfClassType(type, BROWSERLESS_TEST);
    }

    /** A call to the test base: receiverless on a test base class, or on a handle (the context API). */
    static boolean isTestBaseCall(J.MethodInvocation m) {
        JavaType.Method type = m.getMethodType();
        return m.getSelect() == null ? type != null && TEST_BASE_TYPES.contains(type.getDeclaringType().getFullyQualifiedName())
                : isHandle(m.getSelect());
    }

    /**
     * A locator's start, {@code window.findTextField()}: a {@code find..()} on a handle returning a
     * {@code ..Locator} of a component that has a Wrapper. Its Wrapper, else {@code null}.
     */
    static Wrapper ofLocatorStart(Expression e) {
        if (!(e instanceof J.MethodInvocation call) || call.getSelect() == null || !isHandle(call.getSelect())
                || !call.getSimpleName().startsWith("find") || call.getMethodType() == null
                || !(call.getMethodType().getReturnType() instanceof JavaType.FullyQualified locator)
                || !locator.getFullyQualifiedName().endsWith("Locator")) {
            return null;
        }
        String fqn = locator.getFullyQualifiedName();
        return forComponent(fqn.substring(0, fqn.length() - "Locator".length()));
    }

    /** The Wrapper a whole locator expression converts to: its start, then only the Wrapper's filters. */
    static Wrapper ofLocator(Expression e) {
        List<String> filters = new ArrayList<>();
        Expression start = e;
        while (start instanceof J.MethodInvocation call && ofLocatorStart(call) == null) {
            filters.add(call.getSimpleName());
            start = call.getSelect();
        }
        Wrapper wrapper = ofLocatorStart(start);
        return wrapper != null && wrapper.filters().keySet().containsAll(filters) ? wrapper : null;
    }

    /** The whole locator expression the cursor's call (its start or a filter) is part of. */
    static Cursor locatorOf(Cursor callCursor) {
        Cursor c = callCursor;
        while (c.getParentTreeCursor().getValue() instanceof J.MethodInvocation p && p.getSelect() == c.getValue()
                && ofLocator(p) != null) {
            c = c.getParentTreeCursor();
        }
        return ofLocator((Expression) c.getValue()) == null ? null : c;
    }

    /** The Wrapper a whole locator converts to: it is used only through a call or assertion the Wrapper has. */
    static Wrapper convertibleLocator(Cursor locatorCursor) {
        Wrapper wrapper = ofLocator((Expression) locatorCursor.getValue());
        if (wrapper == null) {
            return null;
        }
        Object parent = locatorCursor.getParentTreeCursor().getValue();
        boolean used = parent instanceof J.MethodInvocation call && call.getSelect() == locatorCursor.getValue()
                && wrapper.testerCalls().containsKey(call.getSimpleName())
                || TestBenchAssertionsToUnified.isUnifiedAssertion(parent);
        return used ? wrapper : null;
    }

    /** The Wrapper for a Flow component class, or {@code null} when it has none yet. */
    static Wrapper forComponent(String componentFqn) {
        return BY_COMPONENT.get(componentFqn);
    }

    /** {@code $(X.class)}, {@code $view(X.class)}, {@code find(X.class)}: the start of a query, and its Wrapper. */
    static Wrapper ofLookupStart(Expression e) {
        if (!(e instanceof J.MethodInvocation call) || !LOOKUPS.contains(call.getSimpleName()) || !isTestBaseCall(call)
                || call.getArguments().size() != 1
                || !(call.getArguments().get(0) instanceof J.FieldAccess classLiteral)
                || !(classLiteral.getTarget().getType() instanceof JavaType.FullyQualified component)) {
            return null;
        }
        return forComponent(component.getFullyQualifiedName());
    }

    /**
     * The Wrapper a lookup converts to, when the expression is a whole lookup the Wrapper can express: a query
     * start, then only the Wrapper's filters, then a terminal. Else {@code null}.
     */
    static Wrapper ofLookup(Expression e) {
        if (!(e instanceof J.MethodInvocation terminal) || !TERMINALS.contains(terminal.getSimpleName())) {
            return null;
        }
        List<String> filters = new ArrayList<>();
        Expression start = terminal.getSelect();
        while (start instanceof J.MethodInvocation call && ofLookupStart(call) == null) {
            filters.add(call.getSimpleName());
            start = call.getSelect();
        }
        Wrapper wrapper = ofLookupStart(start);
        return wrapper != null && wrapper.filters().keySet().containsAll(filters) ? wrapper : null;
    }

    /** The terminal of the query the cursor's call is part of (the call itself, a filter, or the start). */
    static Cursor terminalOf(Cursor callCursor) {
        Cursor c = callCursor;
        while (!(c.getValue() instanceof J.MethodInvocation call && TERMINALS.contains(call.getSimpleName()))) {
            Cursor parent = c.getParentTreeCursor();
            if (!(parent.getValue() instanceof J.MethodInvocation p) || p.getSelect() != c.getValue()) {
                return null;
            }
            c = parent;
        }
        return c;
    }

    /** The Wrapper the lookup at the cursor (its terminal) converts to, counting how what it finds is used. */
    static Wrapper convertibleLookup(Cursor terminalCursor) {
        Wrapper wrapper = ofLookup((Expression) terminalCursor.getValue());
        return wrapper != null && usedThrough(terminalCursor, wrapper) ? wrapper : null;
    }

    /** The Wrapper a local component variable converts to: initialized with a lookup, used only through it. */
    static Wrapper convertibleLocal(Cursor methodCursor, J.VariableDeclarations.NamedVariable variable) {
        Wrapper wrapper = ofLookup(variable.getInitializer());
        if (wrapper == null) {
            return null;
        }
        AtomicBoolean other = new AtomicBoolean();
        new JavaIsoVisitor<AtomicBoolean>() {
            @Override
            public J.Identifier visitIdentifier(J.Identifier identifier, AtomicBoolean found) {
                if (identifier.getSimpleName().equals(variable.getSimpleName())
                        && !(getCursor().getParentTreeCursor().getValue() instanceof J.VariableDeclarations.NamedVariable)
                        && !usedThrough(getCursor(), wrapper)) {
                    found.set(true);
                }
                return identifier;
            }
        }.visit(methodCursor.getValue(), other, methodCursor.getParentOrThrow());
        return other.get() ? null : wrapper;
    }

    /** The Wrapper a local variable named by {@code id} converts to, or {@code null}. */
    static Wrapper convertibleLocal(Cursor cursor, J.Identifier id) {
        Cursor methodCursor = cursor.dropParentUntil(t -> t instanceof J.MethodDeclaration || t == Cursor.ROOT_VALUE);
        if (!(methodCursor.getValue() instanceof J.MethodDeclaration method) || method.getBody() == null) {
            return null;
        }
        for (var statement : method.getBody().getStatements()) {
            if (statement instanceof J.VariableDeclarations declarations && declarations.getVariables().size() == 1
                    && declarations.getVariables().get(0).getSimpleName().equals(id.getSimpleName())) {
                return convertibleLocal(methodCursor, declarations.getVariables().get(0));
            }
        }
        return null;
    }

    /** The Wrapper an expression holding a component converts to: a lookup, or a convertible local. */
    static Wrapper convertibleElement(Cursor cursor, Expression e) {
        if (e instanceof J.Identifier id) {
            return convertibleLocal(cursor, id);
        }
        return e instanceof J.MethodInvocation ? convertibleLookup(new Cursor(cursor, e)) : null;
    }

    /**
     * Whether the component at the cursor is only used in a way the Wrapper has: a getter on it, a tester call
     * on {@code test(it)}, a Unified assertion on it, or a local variable the Wrapper can hold.
     */
    static boolean usedThrough(Cursor elementCursor, Wrapper wrapper) {
        Object element = elementCursor.getValue();
        Cursor parentCursor = elementCursor.getParentTreeCursor();
        Object parent = parentCursor.getValue();
        if (parent instanceof J.MethodInvocation call && call.getSelect() == element) {
            return wrapper.getters().containsKey(call.getSimpleName()) && noArguments(call);
        }
        if (parent instanceof J.MethodInvocation test && isTest(test) && test.getArguments().get(0) == element) {
            Object grandparent = parentCursor.getParentTreeCursor().getValue();
            return grandparent instanceof J.MethodInvocation testerCall && testerCall.getSelect() == test
                    && wrapper.testerCalls().containsKey(testerCall.getSimpleName());
        }
        if (TestBenchAssertionsToUnified.isUnifiedAssertion(parent)) {
            return true;
        }
        if (parent instanceof J.VariableDeclarations.NamedVariable variable && variable.getInitializer() == element) {
            Cursor method = parentCursor.dropParentUntil(t -> t instanceof J.MethodDeclaration || t == Cursor.ROOT_VALUE);
            return method.getValue() instanceof J.MethodDeclaration && convertibleLocal(method, variable) != null;
        }
        return false;
    }

    /** {@code test(component)}: the test base's tester factory, with one argument. */
    static boolean isTest(J.MethodInvocation m) {
        return m.getSimpleName().equals("test") && isTestBaseCall(m) && m.getArguments().size() == 1;
    }

    static boolean noArguments(J.MethodInvocation m) {
        return m.getArguments().stream().allMatch(a -> a instanceof J.Empty);
    }

    private static Map<String, String> with(Map<String, String> common, Map<String, String> more) {
        return Stream.concat(common.entrySet().stream(), more.entrySet().stream())
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
