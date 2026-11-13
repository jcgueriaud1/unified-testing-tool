package org.vaadin.addons.unified.rewrite;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.AnnotationMatcher;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.RemoveAnnotationVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.java.tree.TypeUtils;

/**
 * The parts of a Browserless test's setup that need more than a type change: drops {@code extends
 * SpringBrowserlessTest} / {@code extends BrowserlessTest}, makes a plain {@code BrowserlessTest} a
 * {@code @UnifiedUITest}, gives every test method a {@code UnifiedPage page} parameter, and does the same for the
 * helpers that use Browserless Test; a {@code @BeforeEach} among them becomes a call at the start of every test.
 */
public class BrowserlessSetupToUnified extends ScanningRecipe<BrowserlessSetupToUnified.Scanned> {

    private static final String HELPERS = "unified.browserless.helpers";
    private static final String SPRING_BOOT_TEST = "org.springframework.boot.test.context.SpringBootTest";
    private static final Set<String> TEST_ANNOTATIONS = Set.of(SPRING_BOOT_TEST,
            "com.vaadin.testing.unified.spring.UnifiedSpringBootTest", TestBenchSetupToUnified.UNIFIED_UI_TEST);

    @Override
    public String getDisplayName() {
        return "Migrate Browserless test setup to a Unified Page";
    }

    @Override
    public String getDescription() {
        return "Drops `extends SpringBrowserlessTest` / `extends BrowserlessTest`, and gives each test method, and each "
                + "helper using Browserless Test, a `UnifiedPage page` parameter.";
    }

    /** The Browserless helpers, and the {@code @BeforeEach} ones, of each Browserless test class (and base class). */
    public record Scanned(Map<String, Set<String>> helpersByClass, Map<String, List<String>> beforeEachByClass) {
    }

    @Override
    public Scanned getInitialValue(ExecutionContext ctx) {
        return new Scanned(new HashMap<>(), new HashMap<>());
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Scanned scanned) {
        return new JavaIsoVisitor<>() {
            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                if (Browserless.isBrowserlessClass(classDecl)) {
                    Set<String> helpers = Helpers.using(classDecl, Set.of(), Browserless::isTestBaseCall);
                    String name = classDecl.getType().getFullyQualifiedName();
                    scanned.helpersByClass().put(name, helpers);
                    scanned.beforeEachByClass().put(name, beforeEachOf(classDecl, helpers));
                }
                return super.visitClassDeclaration(classDecl, ctx);
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Scanned scanned) {
        return new JavaIsoVisitor<>() {

            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                if (!Browserless.isBrowserlessClass(classDecl)) {
                    return super.visitClassDeclaration(classDecl, ctx);
                }
                Set<String> inherited = inherited(classDecl.getType(), scanned.helpersByClass());
                Set<String> helpers = new HashSet<>(Helpers.using(classDecl, inherited, Browserless::isTestBaseCall));
                helpers.addAll(inherited);
                getCursor().putMessage(HELPERS, helpers);
                J.ClassDeclaration c = super.visitClassDeclaration(classDecl, ctx);
                List<String> beforeEach = new ArrayList<>(inheritedBeforeEach(classDecl.getType(), scanned.beforeEachByClass()));
                beforeEach.addAll(beforeEachOf(classDecl, helpers));
                c = c.withBody(c.getBody().withStatements(Helpers.callBeforeEachFirst(c, beforeEach)));
                if (classDecl.getExtends() == null || !Browserless.isTestBase(classDecl.getExtends().getType())) {
                    return c; // a subclass of the project's own Browserless base class
                }
                c = c.withExtends(null);
                maybeRemoveImport(Browserless.SPRING_BROWSERLESS_TEST);
                maybeRemoveImport(Browserless.BROWSERLESS_TEST);
                if (c.getLeadingAnnotations().stream().noneMatch(a -> TEST_ANNOTATIONS.stream()
                        .anyMatch(t -> TypeUtils.isOfClassType(a.getType(), t)))) {
                    maybeAddImport(TestBenchSetupToUnified.UNIFIED_UI_TEST);
                    c = JavaTemplate.builder("@UnifiedUITest")
                            .imports(TestBenchSetupToUnified.UNIFIED_UI_TEST)
                            .javaParser(Stubs.parser("package com.vaadin.testing.unified; public @interface UnifiedUITest {}"))
                            .build()
                            .apply(updateCursor(c), c.getCoordinates().addAnnotation((a, b) -> 0));
                }
                return c;
            }

            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                J.MethodDeclaration m = super.visitMethodDeclaration(method, ctx);
                Set<String> helpers = getCursor().getNearestMessage(HELPERS);
                if (helpers == null || Pages.hasPageParameter(m)
                        || !Helpers.isBrowserTest(m) && !helpers.contains(m.getSimpleName())) {
                    return m;
                }
                maybeAddImport(TestBenchSetupToUnified.UNIFIED_PAGE);
                if (helpers.contains(m.getSimpleName())) {
                    maybeRemoveImport(TestBenchSetupToUnified.BEFORE_EACH);
                    m = (J.MethodDeclaration) new RemoveAnnotationVisitor(
                            new AnnotationMatcher("@" + TestBenchSetupToUnified.BEFORE_EACH))
                            .visitNonNull(m, ctx, getCursor().getParentOrThrow());
                }
                List<Statement> others = m.getParameters().stream().filter(p -> !(p instanceof J.Empty)).toList();
                J.MethodDeclaration withPage = JavaTemplate.builder("UnifiedPage page")
                        .imports(TestBenchSetupToUnified.UNIFIED_PAGE)
                        .javaParser(Stubs.parser())
                        .build()
                        .apply(updateCursor(m), m.getCoordinates().replaceParameters());
                List<Statement> parameters = new ArrayList<>(withPage.getParameters());
                for (Statement other : others) {
                    parameters.add(other.withPrefix(Space.SINGLE_SPACE));
                }
                return withPage.withParameters(parameters);
            }

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                Set<String> helpers = getCursor().getNearestMessage(HELPERS, Set.of());
                if (m.getSelect() != null || !helpers.contains(m.getSimpleName())) {
                    return m;
                }
                List<Expression> arguments = new ArrayList<>();
                arguments.add(Stubs.page());
                for (Expression a : m.getArguments()) {
                    if (!(a instanceof J.Empty)) {
                        arguments.add(a.withPrefix(Space.SINGLE_SPACE));
                    }
                }
                return m.withArguments(arguments);
            }
        };
    }

    private static Set<String> inherited(JavaType.FullyQualified type, Map<String, Set<String>> byClass) {
        Set<String> inherited = new HashSet<>();
        for (JavaType.FullyQualified s = type.getSupertype(); s != null; s = s.getSupertype()) {
            inherited.addAll(byClass.getOrDefault(s.getFullyQualifiedName(), Set.of()));
        }
        return inherited;
    }

    private static List<String> inheritedBeforeEach(JavaType.FullyQualified type, Map<String, List<String>> byClass) {
        List<String> inherited = new ArrayList<>();
        for (JavaType.FullyQualified s = type.getSupertype(); s != null; s = s.getSupertype()) {
            inherited.addAll(0, byClass.getOrDefault(s.getFullyQualifiedName(), List.of()));
        }
        return inherited;
    }

    private static List<String> beforeEachOf(J.ClassDeclaration c, Set<String> helpers) {
        List<String> names = new ArrayList<>();
        for (Statement s : c.getBody().getStatements()) {
            if (s instanceof J.MethodDeclaration m && helpers.contains(m.getSimpleName())
                    && m.getLeadingAnnotations().stream()
                            .anyMatch(a -> TypeUtils.isOfClassType(a.getType(), TestBenchSetupToUnified.BEFORE_EACH))) {
                names.add(m.getSimpleName());
            }
        }
        return names;
    }
}
