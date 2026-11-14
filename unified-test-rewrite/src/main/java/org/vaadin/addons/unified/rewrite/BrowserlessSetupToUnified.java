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
    private static final String VIEW_PACKAGES = "com.vaadin.browserless.ViewPackages";
    /** What a context-API or extension test's wiring imports; each goes when nothing else uses it. */
    private static final List<String> WIRING_IMPORTS = List.of("com.vaadin.browserless.BrowserlessApplicationContext",
            "com.vaadin.browserless.BrowserlessUserContext", Browserless.UI_CONTEXT, Browserless.EXTENSION,
            "com.vaadin.browserless.SpringBrowserlessApplicationContext",
            "org.junit.jupiter.api.extension.RegisterExtension", "org.junit.jupiter.api.BeforeEach",
            "org.junit.jupiter.api.AfterEach", "org.junit.jupiter.api.BeforeAll", "org.junit.jupiter.api.AfterAll",
            "org.springframework.boot.test.context.TestConfiguration", "org.springframework.context.annotation.Bean",
            "org.springframework.beans.factory.annotation.Autowired", "org.springframework.context.ApplicationContext");
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
                if (Browserless.isContextClass(classDecl)) {
                    return contextApiToUnified(classDecl, c);
                }
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

            /**
             * A context-API or extension test: its wiring goes, and a plain one becomes a {@code @UnifiedUITest} with
             * the views the wiring named as {@code @ViewPackages}. The tests' calls on the window or extension are
             * converted by the other recipes, or keep it through {@code page.unwrap(BrowserlessUIContext.class)}.
             */
            private J.ClassDeclaration contextApiToUnified(J.ClassDeclaration original, J.ClassDeclaration c) {
                Set<String> fields = BrowserlessContexts.fields(original);
                List<Expression> views = BrowserlessContexts.viewSources(original);
                List<Statement> kept = new ArrayList<>();
                List<Statement> before = original.getBody().getStatements();
                List<Statement> after = c.getBody().getStatements();
                for (int i = 0; i < after.size(); i++) {
                    if (i >= before.size() || !BrowserlessContexts.isWiring(before.get(i), fields)) {
                        kept.add(after.get(i));
                    }
                }
                c = c.withBody(c.getBody().withStatements(kept));
                // type attribution can keep a removed field's types in use: a type the class no longer names goes
                String printed = c.printTrimmed(getCursor().getParentOrThrow());
                for (String wiring : WIRING_IMPORTS) {
                    if (!printed.matches("(?s).*\\b" + wiring.substring(wiring.lastIndexOf('.') + 1) + "\\b.*")) {
                        doAfterVisit(new org.openrewrite.java.RemoveImport<>(wiring, true));
                    }
                }
                if (c.getLeadingAnnotations().stream().anyMatch(a -> TEST_ANNOTATIONS.stream()
                        .anyMatch(t -> TypeUtils.isOfClassType(a.getType(), t)))) {
                    return c; // a Spring test: @SpringBootTest becomes @UnifiedSpringBootTest
                }
                maybeAddImport(TestBenchSetupToUnified.UNIFIED_UI_TEST);
                c = JavaTemplate.builder("@UnifiedUITest")
                        .imports(TestBenchSetupToUnified.UNIFIED_UI_TEST)
                        .javaParser(Stubs.parser("package com.vaadin.testing.unified; public @interface UnifiedUITest {}"))
                        .build()
                        .apply(updateCursor(c), c.getCoordinates().addAnnotation((a, b) -> 0));
                List<Expression> classes = views.stream().filter(v -> v instanceof J.FieldAccess f
                        && f.getSimpleName().equals("class")).toList();
                List<Expression> packages = views.stream().filter(v -> v instanceof J.Literal l
                        && l.getValue() instanceof String).toList();
                if (classes.isEmpty() && packages.isEmpty()) {
                    return c;
                }
                String attribute = classes.isEmpty() ? "packages" : "classes";
                List<Expression> values = classes.isEmpty() ? packages : classes;
                // the original expressions as parameters, so a view class keeps its type
                String parameter = classes.isEmpty() ? "#{any(java.lang.String)}" : "#{any(java.lang.Class)}";
                String code = "@ViewPackages(" + attribute + " = {"
                        + String.join(", ", java.util.Collections.nCopies(values.size(), parameter)) + "})";
                maybeAddImport(VIEW_PACKAGES);
                return JavaTemplate.builder(code)
                        .imports(VIEW_PACKAGES)
                        .javaParser(Stubs.parser("package com.vaadin.browserless; public @interface ViewPackages {"
                                + " Class<?>[] classes() default {}; String[] packages() default {}; }"))
                        .build()
                        .apply(updateCursor(c), c.getCoordinates().addAnnotation((a, b) -> 1),
                                values.stream().map(v -> v.withPrefix(org.openrewrite.java.tree.Space.EMPTY)).toArray());
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
