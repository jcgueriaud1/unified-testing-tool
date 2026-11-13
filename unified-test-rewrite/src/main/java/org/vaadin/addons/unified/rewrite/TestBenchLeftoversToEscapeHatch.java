package org.vaadin.addons.unified.rewrite;

import static java.util.Collections.emptyList;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;

/**
 * Keeps the TestBench base-class calls no rule converted ({@code $(GridElement.class)}, {@code executeScript},
 * {@code waitUntil}, ...) compiling and running on Selenium: they go through a {@code TestBenchEscapeHatch} on
 * the test's page, each statement gets a Migration Marker naming the call, and the method gets
 * {@code @RequiresTestingTool(TestingTool.SELENIUM)}.
 */
public class TestBenchLeftoversToEscapeHatch extends Recipe {

    static final String ESCAPE_HATCH = "com.vaadin.testing.unified.selenium.TestBenchEscapeHatch";
    static final String REQUIRES_TESTING_TOOL = "com.vaadin.testing.unified.RequiresTestingTool";
    static final String TESTING_TOOL = "com.vaadin.testing.unified.TestingTool";
    static final String VARIABLE = "testBench";

    /** The types whose methods a {@code BrowserTestBase} test calls without a receiver. */
    private static final Set<String> TEST_BASE_TYPES = Set.of(
            "com.vaadin.testbench.BrowserTestBase",
            "com.vaadin.testbench.TestBenchTestCase",
            "com.vaadin.testbench.parallel.ParallelTest",
            "com.vaadin.testbench.AbstractBrowserDriverTestBase",
            "com.vaadin.testbench.AbstractBrowserTestBase",
            "com.vaadin.testbench.HasElementQuery",
            "com.vaadin.testbench.HasSearchContext",
            "com.vaadin.testbench.HasDriver",
            "com.vaadin.testbench.HasTestBenchCommandExecutor");

    private static final String LEFTOVERS = "unified.leftovers";

    @Override
    public String getDisplayName() {
        return "Keep unconverted TestBench calls running through the Escape Hatch";
    }

    @Override
    public String getDescription() {
        return "Routes TestBench base-class calls no rule converted through `TestBenchEscapeHatch`, marks each with a "
                + "Migration Marker and makes the test method require Selenium.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<>() {

            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                if (method.getBody() == null || !Pages.hasPageParameter(method)) {
                    return method;
                }
                Map<UUID, List<String>> markers = MigrationMarkers.collect(getCursor());
                AtomicBoolean escapes = new AtomicBoolean();
                getCursor().putMessage(LEFTOVERS, escapes);
                J.MethodDeclaration m = super.visitMethodDeclaration(method, ctx);
                if (!escapes.get()) {
                    return requiresSeleniumIfItCallsAnEscapingHelper(m);
                }
                m = MigrationMarkers.write(m, markers);
                maybeAddImport(ESCAPE_HATCH);
                m = JavaTemplate.builder("TestBenchEscapeHatch " + VARIABLE + " = TestBenchEscapeHatch.of(#{any("
                                + TestBenchSetupToUnified.UNIFIED_PAGE + ")});")
                        .imports(ESCAPE_HATCH)
                        .javaParser(Stubs.parser())
                        .build()
                        .apply(updateCursor(m), m.getBody().getStatements().get(0).getCoordinates().before(), Stubs.page());
                if (!Helpers.isBrowserTest(m)) {
                    return m;
                }
                return requiresSelenium(m);
            }

            /** A test that calls a helper going through the Escape Hatch runs on Selenium only, too. */
            private J.MethodDeclaration requiresSeleniumIfItCallsAnEscapingHelper(J.MethodDeclaration m) {
                J.ClassDeclaration c = getCursor().firstEnclosing(J.ClassDeclaration.class);
                if (!Helpers.isBrowserTest(m) || c == null) {
                    return m;
                }
                Set<String> escaping = Helpers.usingTestBench(c);
                AtomicBoolean calls = new AtomicBoolean();
                new JavaIsoVisitor<Integer>() {
                    @Override
                    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation call, Integer p) {
                        if (call.getSelect() == null && escaping.contains(call.getSimpleName())) {
                            calls.set(true);
                        }
                        return super.visitMethodInvocation(call, p);
                    }
                }.visit(m.getBody(), 0);
                return calls.get() ? requiresSelenium(m) : m;
            }

            private J.MethodDeclaration requiresSelenium(J.MethodDeclaration m) {
                maybeAddImport(REQUIRES_TESTING_TOOL);
                maybeAddImport(TESTING_TOOL);
                return JavaTemplate.builder("@RequiresTestingTool(TestingTool.SELENIUM)")
                        .imports(REQUIRES_TESTING_TOOL, TESTING_TOOL)
                        .javaParser(Stubs.parser())
                        .build()
                        .apply(updateCursor(m), m.getCoordinates().addAnnotation((a, b) -> 1));
            }

            /** {@code this} handed to a page object or helper as the TestBench test: it gets the Escape Hatch instead. */
            @Override
            public J.Identifier visitIdentifier(J.Identifier identifier, ExecutionContext ctx) {
                J.Identifier id = super.visitIdentifier(identifier, ctx);
                AtomicBoolean escapes = getCursor().getNearestMessage(LEFTOVERS);
                Object parent = getCursor().getParentTreeCursor().getValue();
                J.ClassDeclaration c = getCursor().firstEnclosing(J.ClassDeclaration.class);
                boolean argument = parent instanceof J.NewClass n && n.getArguments().contains(identifier)
                        || parent instanceof J.MethodInvocation call && call.getArguments().contains(identifier);
                if (escapes == null || !id.getSimpleName().equals("this") || !argument || c == null
                        || !TestBenchSetupToUnified.isTestBenchClass(c)
                        || !MigrationMarkers.add(getCursor(), "TestBench page object: "
                                + ((J) parent).printTrimmed(getCursor().getParentTreeCursor()))) {
                    return id;
                }
                escapes.set(true);
                return new J.Identifier(Tree.randomId(), id.getPrefix(), Markers.EMPTY, emptyList(), VARIABLE,
                        JavaType.ShallowClass.build(ESCAPE_HATCH), null);
            }

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                AtomicBoolean escapes = getCursor().getNearestMessage(LEFTOVERS);
                if (escapes == null || m.getSelect() != null || !isTestBaseCall(m)
                        || !MigrationMarkers.add(getCursor(), "TestBench Escape Hatch: " + method.printTrimmed(getCursor())
                                + (isCustomElementLookup(method) ? ", a custom element: write a Wrapper for it (ADR 0003)" : ""))) {
                    return m;
                }
                escapes.set(true);
                return m.getPadding().withSelect(JRightPadded.build(new J.Identifier(Tree.randomId(), Space.EMPTY,
                        Markers.EMPTY, emptyList(), VARIABLE, JavaType.ShallowClass.build(ESCAPE_HATCH), null)));
            }
        };
    }

    /** A {@code $(X.class)} of an application's own {@code @Element} class, not one TestBench ships. */
    private static boolean isCustomElementLookup(J.MethodInvocation m) {
        if (!m.getSimpleName().equals("$") || m.getArguments().size() != 1
                || !(m.getArguments().get(0) instanceof J.FieldAccess classLiteral)
                || !(classLiteral.getTarget().getType() instanceof JavaType.FullyQualified element)) {
            return false;
        }
        return !element.getFullyQualifiedName().startsWith("com.vaadin.") && element.getAnnotations().stream()
                .anyMatch(a -> a.getFullyQualifiedName().equals("com.vaadin.testbench.elementsbase.Element"));
    }

    static boolean isTestBaseCall(J.MethodInvocation m) {
        JavaType.Method type = m.getMethodType();
        return type != null && TEST_BASE_TYPES.contains(type.getDeclaringType().getFullyQualifiedName());
    }
}
