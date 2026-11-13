package org.vaadin.addons.unified.rewrite;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.AnnotationMatcher;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.RemoveAnnotationVisitor;
import org.openrewrite.java.tree.Comment;
import org.openrewrite.java.tree.TextComment;
import org.openrewrite.marker.Markers;
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
    private static final String RUN_LOCALLY = "com.vaadin.testbench.annotations.RunLocally";
    private static final String BROWSER_CONFIGURATION = "com.vaadin.testbench.annotations.BrowserConfiguration";
    /** Runners the Unified extensions replace: Spring's, and TestBench's parallel runner. */
    private static final List<String> RUNNERS = List.of("org.springframework.test.context.junit4.SpringRunner",
            "org.springframework.test.context.junit4.SpringJUnit4ClassRunner", "com.vaadin.testbench.parallel.ParallelRunner");
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
                List<String> browserSetup = new ArrayList<>();
                if (c.getLeadingAnnotations().stream().anyMatch(a -> TypeUtils.isOfClassType(a.getType(), RUN_LOCALLY))) {
                    browserSetup.add("@RunLocally");
                }
                if (c.getBody().getStatements().stream().anyMatch(TestBenchDriverLifecycleRemoved::isBrowserConfiguration)) {
                    browserSetup.add("@BrowserConfiguration");
                }
                c = c.withBody(c.getBody().withStatements(ListUtils.map(c.getBody().getStatements(), s ->
                        isScreenshotRule(s) || isEmptyLifecycleMethod(s) || isBrowserConfiguration(s) ? null : s)));
                c = (J.ClassDeclaration) new RemoveAnnotationVisitor(new AnnotationMatcher("@" + RUN_LOCALLY))
                        .visitNonNull(c, ctx, getCursor().getParentOrThrow());
                for (String runner : RUNNERS) {
                    c = (J.ClassDeclaration) new RemoveAnnotationVisitor(
                            new AnnotationMatcher("@org.junit.runner.RunWith(" + runner + ".class)"))
                            .visitNonNull(c, ctx, getCursor().getParentOrThrow());
                    maybeRemoveImport(runner);
                }
                if (!browserSetup.isEmpty()) {
                    List<Comment> comments = new ArrayList<>(c.getPrefix().getComments());
                    comments.add(new TextComment(false, " TODO unified: browser setup removed (" + String.join(", ", browserSetup)
                            + "): configure the Unified Selenium adapter (vaadin.testing.* settings)",
                            c.getPrefix().getWhitespace().isEmpty() ? "\n" : "\n" + c.getPrefix().getIndent(), Markers.EMPTY));
                    c = c.withPrefix(c.getPrefix().withComments(comments));
                }
                for (String type : List.of(SCREENSHOT_RULE, "org.junit.Rule", RUN_LOCALLY, BROWSER_CONFIGURATION,
                        "org.junit.runner.RunWith", "com.vaadin.testbench.parallel.Browser",
                        "org.openqa.selenium.remote.DesiredCapabilities", "java.util.List")) {
                    maybeRemoveImport(type);
                }
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

    private static boolean isBrowserConfiguration(Statement s) {
        return s instanceof J.MethodDeclaration m && m.getLeadingAnnotations().stream()
                .anyMatch(a -> TypeUtils.isOfClassType(a.getType(), BROWSER_CONFIGURATION));
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
