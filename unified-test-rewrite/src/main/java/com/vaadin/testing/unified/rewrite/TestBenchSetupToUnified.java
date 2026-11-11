package com.vaadin.testing.unified.rewrite;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.java.tree.TypeUtils;

/**
 * The parts of a TestBench test's setup that need more than a type change: drops
 * {@code extends BrowserTestBase} and the {@code @LocalServerPort} field, gives every
 * {@code @BrowserTest} method a {@code UnifiedPage page} parameter, and turns
 * {@code getDriver().get(url)} into {@code page.navigate(path)}.
 */
public class TestBenchSetupToUnified extends ScanningRecipe<Map<String, Set<String>>> {

    static final String BROWSER_TEST_BASE = "com.vaadin.testbench.BrowserTestBase";
    static final String BROWSER_TEST = "com.vaadin.testbench.BrowserTest";
    static final String LOCAL_SERVER_PORT = "org.springframework.boot.test.web.server.LocalServerPort";
    static final String ROUTE = "com.vaadin.flow.router.Route";
    static final String UNIFIED_PAGE = "com.vaadin.testing.unified.UnifiedPage";

    private static final MethodMatcher DRIVER_GET = new MethodMatcher("org.openqa.selenium.WebDriver get(String)");
    private static final MethodMatcher GET_DRIVER = new MethodMatcher("com.vaadin.testbench.HasDriver getDriver()", true);

    @Override
    public String getDisplayName() {
        return "Migrate TestBench test setup to a Unified Page";
    }

    @Override
    public String getDescription() {
        return "Drops `extends BrowserTestBase` and the server port, gives each `@BrowserTest` a `UnifiedPage page` "
                + "parameter and turns `getDriver().get(url)` into `page.navigate(path)`.";
    }

    @Override
    public Map<String, Set<String>> getInitialValue(ExecutionContext ctx) {
        return new HashMap<>();
    }

    /** Collects the views by route path, to navigate by class when exactly one view has the path. */
    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Map<String, Set<String>> routes) {
        return new JavaIsoVisitor<>() {
            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                if (classDecl.getType() != null) {
                    for (J.Annotation a : classDecl.getLeadingAnnotations()) {
                        String path = routePath(a);
                        if (path != null) {
                            routes.computeIfAbsent(path, p -> new TreeSet<>())
                                    .add(classDecl.getType().getFullyQualifiedName());
                        }
                    }
                }
                return super.visitClassDeclaration(classDecl, ctx);
            }
        };
    }

    /** The explicit path of a {@code @Route("path")} or {@code @Route(value = "path")}, else {@code null}. */
    private static String routePath(J.Annotation a) {
        if (!TypeUtils.isOfClassType(a.getType(), ROUTE) || a.getArguments() == null) {
            return null;
        }
        for (Expression arg : a.getArguments()) {
            Expression value = arg instanceof J.Assignment as && as.getVariable() instanceof J.Identifier id
                    && id.getSimpleName().equals("value") ? as.getAssignment() : arg;
            if (value instanceof J.Literal literal && literal.getValue() instanceof String path) {
                return path;
            }
        }
        return null;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Map<String, Set<String>> routes) {
        return new JavaIsoVisitor<>() {

            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                if (classDecl.getExtends() == null
                        || !TypeUtils.isOfClassType(classDecl.getExtends().getType(), BROWSER_TEST_BASE)) {
                    return super.visitClassDeclaration(classDecl, ctx);
                }
                J.ClassDeclaration c = super.visitClassDeclaration(classDecl, ctx);
                c = c.withExtends(null);
                c = c.withBody(c.getBody().withStatements(withoutUnusedPortFields(c)));
                maybeRemoveImport(BROWSER_TEST_BASE);
                maybeRemoveImport(LOCAL_SERVER_PORT);
                return c;
            }

            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                J.MethodDeclaration m = super.visitMethodDeclaration(method, ctx);
                boolean browserTest = m.getLeadingAnnotations().stream()
                        .anyMatch(a -> TypeUtils.isOfClassType(a.getType(), BROWSER_TEST));
                if (!browserTest || !m.getParameters().stream().allMatch(p -> p instanceof J.Empty)) {
                    return m;
                }
                maybeAddImport(UNIFIED_PAGE);
                return JavaTemplate.builder("UnifiedPage page")
                        .imports(UNIFIED_PAGE)
                        .javaParser(Stubs.parser())
                        .build()
                        .apply(updateCursor(m), m.getCoordinates().replaceParameters());
            }

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                if (!DRIVER_GET.matches(m) || !(m.getSelect() instanceof J.MethodInvocation select)
                        || !GET_DRIVER.matches(select)) {
                    return m;
                }
                String path = Urls.pathOf(m.getArguments().get(0));
                if (path == null) {
                    return m;
                }
                Set<String> views = routes.getOrDefault(path, Set.of());
                if (views.size() == 1) {
                    String view = views.iterator().next();
                    maybeAddImport(view);
                    return JavaTemplate.builder("#{any(" + UNIFIED_PAGE + ")}.navigate(" + simpleName(view) + ".class)")
                            .imports(view)
                            .javaParser(Stubs.parser(Stubs.classStub(view)))
                            .build()
                            .apply(getCursor(), m.getCoordinates().replace(), Stubs.page());
                }
                return JavaTemplate.builder("#{any(" + UNIFIED_PAGE + ")}.navigate(#{any(String)})")
                        .javaParser(Stubs.parser())
                        .build()
                        .apply(getCursor(), m.getCoordinates().replace(), Stubs.page(), Stubs.literal(path));
            }
        };
    }

    private static String simpleName(String fqn) {
        return fqn.substring(fqn.lastIndexOf('.') + 1).replace('$', '.');
    }

    /** The class's statements without {@code @LocalServerPort} fields nothing reads any more. */
    private static List<Statement> withoutUnusedPortFields(J.ClassDeclaration c) {
        List<Statement> kept = new ArrayList<>();
        for (Statement s : c.getBody().getStatements()) {
            if (s instanceof J.VariableDeclarations v
                    && v.getLeadingAnnotations().stream().anyMatch(a -> TypeUtils.isOfClassType(a.getType(), LOCAL_SERVER_PORT))
                    && v.getVariables().stream().noneMatch(var -> isRead(c, var.getSimpleName()))) {
                continue;
            }
            kept.add(s);
        }
        return kept;
    }

    private static boolean isRead(J.ClassDeclaration c, String name) {
        AtomicBoolean read = new AtomicBoolean();
        new JavaIsoVisitor<AtomicBoolean>() {
            @Override
            public J.Identifier visitIdentifier(J.Identifier identifier, AtomicBoolean found) {
                if (identifier.getSimpleName().equals(name) && identifier.getFieldType() != null
                        && !(getCursor().getParentTreeCursor().getValue() instanceof J.VariableDeclarations.NamedVariable)) {
                    found.set(true);
                }
                return identifier;
            }
        }.visit(c.getBody(), read);
        return read.get();
    }
}
