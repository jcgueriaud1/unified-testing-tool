package org.vaadin.addons.unified.rewrite;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.java.tree.TypeUtils;

/**
 * Browserless test base calls with a Unified meaning: {@code navigate(View.class)} and
 * {@code UI.getCurrent().navigate(View.class)} become {@code page.navigate(View.class)}, {@code roundTrip()} and
 * {@code runPendingSignalsTasks()} are dropped (Auto-Wait), {@code fireShortcut(Key.X)} becomes
 * {@code page.pressKey("X")} with a Migration Marker, and {@code getCurrentView()} checks become
 * {@code assertThat(page).isAt(View.class)}.
 */
public class BrowserlessCallsToUnified extends Recipe {

    private static final MethodMatcher UI_GET_CURRENT = new MethodMatcher("com.vaadin.flow.component.UI getCurrent()");
    private static final MethodMatcher UI_NAVIGATE = new MethodMatcher("com.vaadin.flow.component.UI navigate(java.lang.Class)");
    private static final MethodMatcher ASSERT_TRUE = new MethodMatcher("org.junit.jupiter.api.Assertions assertTrue(boolean)");
    private static final MethodMatcher ASSERT_INSTANCE_OF =
            new MethodMatcher("org.junit.jupiter.api.Assertions assertInstanceOf(java.lang.Class, java.lang.Object)");
    private static final String KEY = "com.vaadin.flow.component.Key";
    private static final String KEY_MODIFIER = "com.vaadin.flow.component.KeyModifier";
    /** Flow's key constants whose Playwright name is not just the constant in camel case. */
    private static final Map<String, String> KEY_NAMES = Map.of("ESCAPE", "Escape", "ENTER", "Enter", "TAB", "Tab",
            "BACKSPACE", "Backspace", "DELETE", "Delete", "ARROW_UP", "ArrowUp", "ARROW_DOWN",
            "ArrowDown", "ARROW_LEFT", "ArrowLeft", "ARROW_RIGHT", "ArrowRight");
    private static final Map<String, String> MODIFIER_NAMES = Map.of("CONTROL", "Control", "SHIFT", "Shift", "ALT",
            "Alt", "META", "Meta");

    @Override
    public String getDisplayName() {
        return "Migrate Browserless test base calls with a Unified meaning";
    }

    @Override
    public String getDescription() {
        return "Turns `navigate(..)` into `page.navigate(..)`, drops `roundTrip()`, turns `fireShortcut(..)` into "
                + "`page.pressKey(..)` and `getCurrentView()` checks into `assertThat(page).isAt(..)`.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<>() {
            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                if (!Pages.hasPageParameter(method)) {
                    return method;
                }
                var markers = MigrationMarkers.collect(getCursor());
                return MigrationMarkers.write(super.visitMethodDeclaration(method, ctx), markers);
            }

            @Override
            public J.Block visitBlock(J.Block block, ExecutionContext ctx) {
                J.Block b = super.visitBlock(block, ctx);
                if (!inPageMethod()) {
                    return b;
                }
                List<Statement> kept = new ArrayList<>();
                boolean dropped = false;
                for (Statement s : b.getStatements()) {
                    if (s instanceof J.MethodInvocation call && Browserless.isTestBaseCall(call)
                            && (call.getSimpleName().equals("roundTrip") || call.getSimpleName().equals("runPendingSignalsTasks"))) {
                        dropped = true;
                        continue;
                    }
                    kept.add(s);
                }
                return dropped ? b.withStatements(kept) : b;
            }

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                if (!inPageMethod()) {
                    return m;
                }
                boolean statement = getCursor().getParentTreeCursor().getValue() instanceof J.Block;
                if (statement && Browserless.isTestBaseCall(method) && method.getSimpleName().equals("navigate")
                        && method.getArguments().get(0) instanceof J.FieldAccess) {
                    // the view it returns is unused: the Unified Page navigates the same way
                    return navigate(m);
                }
                if (statement && UI_NAVIGATE.matches(method) && method.getSelect() instanceof J.MethodInvocation current
                        && UI_GET_CURRENT.matches(current)) {
                    maybeRemoveImport("com.vaadin.flow.component.UI");
                    return navigate(m);
                }
                if (statement && Browserless.isTestBaseCall(method) && method.getSimpleName().equals("fireShortcut")) {
                    String key = keyName(method.getArguments());
                    if (key != null && MigrationMarkers.add(getCursor(),
                            "was fireShortcut(..); pressKey also reaches the focused component and an open dialog")) {
                        maybeRemoveImport(KEY);
                        maybeRemoveImport(KEY_MODIFIER);
                        // built, not templated: a template here reformats the statement after it
                        JavaType.Method pressKey = new JavaType.Method(null, 1L,
                                JavaType.ShallowClass.build(TestBenchSetupToUnified.UNIFIED_PAGE), "pressKey",
                                JavaType.Primitive.Void, List.of("key"), List.of(JavaType.Primitive.String), null, null,
                                null, null);
                        return m.getPadding().withSelect(org.openrewrite.java.tree.JRightPadded.build(Stubs.page()))
                                .withName(m.getName().withSimpleName("pressKey").withType(pressKey))
                                .withMethodType(pressKey)
                                .withArguments(List.of(Stubs.literal(key)));
                    }
                }
                Expression view = viewChecked(method);
                if (view != null) {
                    maybeAddImport(TestBenchAssertionsToUnified.UNIFIED_ASSERTIONS, "assertThat", false);
                    maybeRemoveImport("org.junit.jupiter.api.Assertions");
                    maybeRemoveImport("org.junit.jupiter.api.Assertions.assertTrue");
                    maybeRemoveImport("org.junit.jupiter.api.Assertions.assertInstanceOf");
                    return JavaTemplate.builder("assertThat(#{any(" + TestBenchSetupToUnified.UNIFIED_PAGE + ")}).isAt(#{any(java.lang.Class)})")
                            .staticImports(TestBenchAssertionsToUnified.UNIFIED_ASSERTIONS + ".assertThat")
                            .javaParser(Stubs.parser())
                            .build()
                            .<J.MethodInvocation>apply(getCursor(), m.getCoordinates().replace(), Stubs.page(), view)
                            .withId(m.getId()).withPrefix(m.getPrefix());
                }
                return m;
            }

            /** {@code page.navigate(..)} with the call's arguments. */
            private J.MethodInvocation navigate(J.MethodInvocation m) {
                List<Expression> arguments = m.getArguments();
                Object[] parameters = new Object[arguments.size() + 1];
                parameters[0] = Stubs.page();
                for (int i = 0; i < arguments.size(); i++) {
                    parameters[i + 1] = arguments.get(i);
                }
                String placeholders = String.join(", ", java.util.Collections.nCopies(arguments.size(), "#{any()}"));
                return JavaTemplate.builder("#{any(" + TestBenchSetupToUnified.UNIFIED_PAGE + ")}.navigate(" + placeholders + ")")
                        .javaParser(Stubs.parser())
                        .build()
                        .<J.MethodInvocation>apply(getCursor(), m.getCoordinates().replace(), parameters)
                        .withPrefix(m.getPrefix());
            }

            private boolean inPageMethod() {
                J.MethodDeclaration enclosing = getCursor().firstEnclosing(J.MethodDeclaration.class);
                return enclosing != null && Pages.hasPageParameter(enclosing);
            }
        };
    }

    /**
     * The view class literal of {@code assertTrue(getCurrentView() instanceof View)} or
     * {@code assertInstanceOf(View.class, getCurrentView())}, else {@code null}.
     */
    private static Expression viewChecked(J.MethodInvocation m) {
        if (ASSERT_TRUE.matches(m) && m.getArguments().get(0) instanceof J.InstanceOf check
                && isCurrentView(check.getExpression()) && check.getClazz() instanceof J.Identifier type
                && type.getType() instanceof JavaType.FullyQualified) {
            return classLiteral(type);
        }
        if (ASSERT_INSTANCE_OF.matches(m) && m.getArguments().size() == 2 && isCurrentView(m.getArguments().get(1))
                && m.getArguments().get(0) instanceof J.FieldAccess literal && literal.getSimpleName().equals("class")) {
            return literal;
        }
        return null;
    }

    private static boolean isCurrentView(Expression e) {
        return e instanceof J.MethodInvocation call && call.getSimpleName().equals("getCurrentView")
                && Browserless.isTestBaseCall(call);
    }

    private static J.FieldAccess classLiteral(J.Identifier type) {
        return new J.FieldAccess(org.openrewrite.Tree.randomId(), org.openrewrite.java.tree.Space.EMPTY,
                org.openrewrite.marker.Markers.EMPTY, type.withPrefix(org.openrewrite.java.tree.Space.EMPTY),
                org.openrewrite.java.tree.JLeftPadded.build(new J.Identifier(org.openrewrite.Tree.randomId(),
                        org.openrewrite.java.tree.Space.EMPTY, org.openrewrite.marker.Markers.EMPTY,
                        java.util.Collections.emptyList(), "class", null, null)),
                JavaType.ShallowClass.build("java.lang.Class"));
    }

    /** {@code Key.ENTER} and {@code KeyModifier.CONTROL, ...} as a Playwright key name, or {@code null}. */
    private static String keyName(List<Expression> arguments) {
        List<String> parts = new ArrayList<>();
        for (int i = arguments.size() - 1; i >= 0; i--) {
            if (!(arguments.get(i) instanceof J.FieldAccess constant)
                    || !(constant.getTarget().getType() instanceof JavaType.FullyQualified type)) {
                return null;
            }
            String name = constant.getSimpleName();
            if (i == 0) {
                if (!TypeUtils.isOfClassType(type, KEY)) {
                    return null;
                }
                String key = KEY_NAMES.get(name);
                if (key == null && name.matches("KEY_[A-Z]")) {
                    key = name.substring(4).toLowerCase(Locale.ROOT);
                } else if (key == null && name.matches("DIGIT_[0-9]")) {
                    key = name.substring(6);
                }
                if (key == null) {
                    return null;
                }
                parts.add(0, key);
            } else {
                String modifier = MODIFIER_NAMES.get(name);
                if (!TypeUtils.isOfClassType(type, KEY_MODIFIER) || modifier == null) {
                    return null;
                }
                parts.add(0, modifier);
            }
        }
        if (parts.isEmpty()) {
            return null;
        }
        // fireShortcut(key, modifiers...): Playwright writes the modifiers first
        String key = parts.remove(0);
        parts.add(key);
        return String.join("+", parts);
    }
}
