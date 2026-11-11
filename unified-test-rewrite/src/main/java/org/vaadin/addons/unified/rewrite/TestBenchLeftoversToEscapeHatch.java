package org.vaadin.addons.unified.rewrite;

import static java.util.Collections.emptyList;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.Comment;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.java.tree.TextComment;
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
                Map<UUID, List<String>> leftovers = new LinkedHashMap<>();
                getCursor().putMessage(LEFTOVERS, leftovers);
                J.MethodDeclaration m = super.visitMethodDeclaration(method, ctx);
                if (leftovers.isEmpty()) {
                    return m;
                }
                m = m.withBody(m.getBody().withStatements(marked(m.getBody().getStatements(), leftovers)));
                maybeAddImport(ESCAPE_HATCH);
                maybeAddImport(REQUIRES_TESTING_TOOL);
                maybeAddImport(TESTING_TOOL);
                m = JavaTemplate.builder("TestBenchEscapeHatch " + VARIABLE + " = TestBenchEscapeHatch.of(#{any("
                                + TestBenchSetupToUnified.UNIFIED_PAGE + ")});")
                        .imports(ESCAPE_HATCH)
                        .javaParser(Stubs.parser())
                        .build()
                        .apply(updateCursor(m), m.getBody().getStatements().get(0).getCoordinates().before(), Stubs.page());
                return JavaTemplate.builder("@RequiresTestingTool(TestingTool.SELENIUM)")
                        .imports(REQUIRES_TESTING_TOOL, TESTING_TOOL)
                        .javaParser(Stubs.parser())
                        .build()
                        .apply(updateCursor(m), m.getCoordinates().addAnnotation((a, b) -> 1));
            }

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                Map<UUID, List<String>> leftovers = getCursor().getNearestMessage(LEFTOVERS);
                if (leftovers == null || m.getSelect() != null || !isTestBaseCall(m)) {
                    return m;
                }
                Cursor statement = statementInMethodBody();
                if (statement == null) {
                    return m;
                }
                leftovers.computeIfAbsent(((Statement) statement.getValue()).getId(), id -> new ArrayList<>())
                        .add(method.printTrimmed(getCursor()));
                return m.getPadding().withSelect(JRightPadded.build(new J.Identifier(Tree.randomId(), Space.EMPTY,
                        Markers.EMPTY, emptyList(), VARIABLE, JavaType.ShallowClass.build(ESCAPE_HATCH), null)));
            }

            /** The statement of the method body this invocation is part of. */
            private Cursor statementInMethodBody() {
                Cursor c = getCursor();
                while (c.getParent() != null) {
                    Cursor parent = c.getParentTreeCursor();
                    if (parent.getValue() instanceof J.Block && parent.getParentTreeCursor().getValue() instanceof J.MethodDeclaration) {
                        return c;
                    }
                    c = parent;
                }
                return null;
            }
        };
    }

    private static boolean isTestBaseCall(J.MethodInvocation m) {
        JavaType.Method type = m.getMethodType();
        return type != null && TEST_BASE_TYPES.contains(type.getDeclaringType().getFullyQualifiedName());
    }

    /** The statements, each one with leftovers preceded by a Migration Marker naming them. */
    private static List<Statement> marked(List<Statement> statements, Map<UUID, List<String>> leftovers) {
        List<Statement> result = new ArrayList<>();
        for (Statement s : statements) {
            List<String> calls = leftovers.get(s.getId());
            if (calls != null) {
                String indent = s.getPrefix().getIndent();
                List<Comment> comments = new ArrayList<>(s.getPrefix().getComments());
                comments.add(new TextComment(false, " TODO unified: TestBench Escape Hatch: " + String.join(", ", calls),
                        "\n" + indent, Markers.EMPTY));
                s = s.withPrefix(s.getPrefix().withComments(comments));
            }
            result.add(s);
        }
        return result;
    }
}
