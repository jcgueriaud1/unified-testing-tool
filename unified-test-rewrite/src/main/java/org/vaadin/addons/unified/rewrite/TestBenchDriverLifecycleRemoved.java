package org.vaadin.addons.unified.rewrite;

import java.util.Set;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.java.tree.TypeUtils;

/**
 * The Unified API owns the browser and writes failure evidence: a TestBench test's own driver lifecycle goes —
 * {@code setDriver(..)}, {@code getDriver().quit()} / {@code close()}, the {@code ScreenshotOnFailureRule} field —
 * and so do lifecycle methods left empty.
 */
public class TestBenchDriverLifecycleRemoved extends Recipe {

    private static final String SCREENSHOT_RULE = "com.vaadin.testbench.ScreenshotOnFailureRule";
    private static final Set<String> LIFECYCLE = Set.of("org.junit.Before", "org.junit.After",
            "org.junit.jupiter.api.BeforeEach", "org.junit.jupiter.api.AfterEach");

    @Override
    public String getDisplayName() {
        return "Remove a TestBench test's own driver lifecycle";
    }

    @Override
    public String getDescription() {
        return "Removes `setDriver(..)`, `getDriver().quit()`, `getDriver().close()` and the `ScreenshotOnFailureRule` "
                + "field, which the Unified API takes over, and lifecycle methods left empty.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<>() {
            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                J.ClassDeclaration c = super.visitClassDeclaration(classDecl, ctx);
                if (!TestBenchSetupToUnified.isTestBenchClass(classDecl)) {
                    return c;
                }
                c = c.withBody(c.getBody().withStatements(ListUtils.map(c.getBody().getStatements(), s ->
                        isScreenshotRule(s) || isEmptyLifecycleMethod(s) ? null : s)));
                maybeRemoveImport(SCREENSHOT_RULE);
                maybeRemoveImport("org.junit.Rule");
                return c;
            }

            @Override
            public J.Block visitBlock(J.Block block, ExecutionContext ctx) {
                J.Block b = super.visitBlock(block, ctx);
                J.ClassDeclaration c = getCursor().firstEnclosing(J.ClassDeclaration.class);
                if (c == null || !TestBenchSetupToUnified.isTestBenchClass(c)
                        || !(getCursor().getParentTreeCursor().getValue() instanceof J.MethodDeclaration)) {
                    return b;
                }
                return b.withStatements(ListUtils.map(b.getStatements(), s -> isDriverLifecycle(s) ? null : s));
            }
        };
    }

    private static boolean isDriverLifecycle(Statement s) {
        if (!(s instanceof J.MethodInvocation call)) {
            return false;
        }
        if (call.getSelect() == null && call.getSimpleName().equals("setDriver")) {
            return true;
        }
        return (call.getSimpleName().equals("quit") || call.getSimpleName().equals("close"))
                && call.getSelect() instanceof J.MethodInvocation driver && driver.getSelect() == null
                && driver.getSimpleName().equals("getDriver");
    }

    private static boolean isScreenshotRule(Statement s) {
        return s instanceof J.VariableDeclarations v && v.getTypeExpression() != null
                && TypeUtils.isOfClassType(v.getTypeExpression().getType(), SCREENSHOT_RULE);
    }

    private static boolean isEmptyLifecycleMethod(Statement s) {
        return s instanceof J.MethodDeclaration m && m.getBody() != null
                && m.getBody().getStatements().stream().allMatch(TestBenchDriverLifecycleRemoved::isDriverLifecycle)
                && m.getLeadingAnnotations().stream().anyMatch(a -> LIFECYCLE.stream()
                        .anyMatch(t -> TypeUtils.isOfClassType(a.getType(), t)));
    }
}
