package org.vaadin.addons.unified.rewrite;

import static java.util.Collections.emptyList;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.AnnotationMatcher;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.RemoveAnnotationVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JContainer;
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.Markers;

/**
 * The setup of a Dramafinder test (ADR 0006): drops {@code extends SpringPlaywrightIT} /
 * {@code extends AbstractBasePlaywrightIT} and its {@code getUrl()} / {@code getView()} overrides, makes the class a
 * {@code @UnifiedSpringBootTest} or {@code @UnifiedUITest}, removes an {@code @EnabledIfSystemProperty} guard on
 * the Testing Tool, gives every test method and every helper using the test base a {@code UnifiedPage page}
 * parameter, and converts the base class's navigation calls and checks to the Unified Page. A test starts at the
 * view {@code getView()} named, unless it navigates first itself.
 */
public class DramafinderSetupToUnified extends Recipe {

    private static final String HELPERS = "unified.dramafinder.helpers";
    private static final String SPRING_BOOT_TEST = "org.springframework.boot.test.context.SpringBootTest";
    private static final String UNIFIED_SPRING_BOOT_TEST = "com.vaadin.testing.unified.spring.UnifiedSpringBootTest";
    private static final String ENABLED_IF_SYSTEM_PROPERTY = "org.junit.jupiter.api.condition.EnabledIfSystemProperty";
    private static final Set<String> TEST_ANNOTATIONS = Set.of(SPRING_BOOT_TEST, UNIFIED_SPRING_BOOT_TEST,
            TestBenchSetupToUnified.UNIFIED_UI_TEST);

    @Override
    public String getDisplayName() {
        return "Migrate Dramafinder test setup to a Unified Page";
    }

    @Override
    public String getDescription() {
        return "Drops `extends SpringPlaywrightIT` / `extends AbstractBasePlaywrightIT`, gives each test method, and each "
                + "helper using the test base, a `UnifiedPage page` parameter, and moves navigation to the page.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<>() {

            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                if (!Dramafinder.isDramafinderClass(classDecl) || !Dramafinder.migratable(classDecl)) {
                    return super.visitClassDeclaration(classDecl, ctx);
                }
                Set<String> helpers = Helpers.using(classDecl, Set.of(), Dramafinder::usesTestBase);
                getCursor().putMessage(HELPERS, helpers);
                J.ClassDeclaration c = super.visitClassDeclaration(classDecl, ctx);
                List<String> beforeEach = beforeEachOf(classDecl, helpers);
                c = c.withBody(c.getBody().withStatements(startAtView(classDecl, Dramafinder.viewOf(classDecl),
                        Helpers.callBeforeEachFirst(c, beforeEach))));
                // getUrl() and getView(): the base URL is configuration, the view is now each test's first step
                c = c.withBody(c.getBody().withStatements(c.getBody().getStatements().stream()
                        .filter(s -> !(s instanceof J.MethodDeclaration m && Dramafinder.CONVERTED_OVERRIDES.contains(m.getSimpleName())
                                && m.getParameters().stream().allMatch(p -> p instanceof J.Empty)))
                        .toList()));
                boolean spring = Dramafinder.isSpringClass(classDecl);
                JavaType.FullyQualified parent = TypeUtils.asFullyQualified(classDecl.getExtends().getType());
                c = c.withExtends(null);
                maybeRemoveImport(Dramafinder.BASE);
                maybeRemoveImport(Dramafinder.SPRING_BASE);
                if (parent != null) {
                    maybeRemoveImport(parent.getFullyQualifiedName()); // a copy of SpringPlaywrightIT
                }
                c = withoutToolGuard(c, ctx);
                if (c.getLeadingAnnotations().stream().anyMatch(a -> TEST_ANNOTATIONS.stream()
                        .anyMatch(t -> TypeUtils.isOfClassType(a.getType(), t)))) {
                    return c; // @SpringBootTest becomes @UnifiedSpringBootTest
                }
                String annotation = spring ? UNIFIED_SPRING_BOOT_TEST : TestBenchSetupToUnified.UNIFIED_UI_TEST;
                String simpleName = annotation.substring(annotation.lastIndexOf('.') + 1);
                String pkg = annotation.substring(0, annotation.lastIndexOf('.'));
                maybeAddImport(annotation);
                return JavaTemplate.builder("@" + simpleName)
                        .imports(annotation)
                        .javaParser(Stubs.parser("package " + pkg + "; public @interface " + simpleName + " {}"))
                        .build()
                        .apply(updateCursor(c), c.getCoordinates().addAnnotation((a, b) -> 0));
            }

            /** Without {@code @EnabledIfSystemProperty(named = "vaadin.testing.tool", ..)}: the test now runs on any tool. */
            private J.ClassDeclaration withoutToolGuard(J.ClassDeclaration c, ExecutionContext ctx) {
                boolean guarded = c.getLeadingAnnotations().stream().anyMatch(a -> TypeUtils.isOfClassType(a.getType(),
                        ENABLED_IF_SYSTEM_PROPERTY) && a.getArguments() != null && a.getArguments().stream().anyMatch(arg ->
                        arg instanceof J.Assignment as && as.getVariable() instanceof J.Identifier n
                                && n.getSimpleName().equals("named") && as.getAssignment() instanceof J.Literal l
                                && "vaadin.testing.tool".equals(l.getValue())));
                if (!guarded) {
                    return c;
                }
                maybeRemoveImport(ENABLED_IF_SYSTEM_PROPERTY);
                return (J.ClassDeclaration) new RemoveAnnotationVisitor(new AnnotationMatcher("@" + ENABLED_IF_SYSTEM_PROPERTY))
                        .visitNonNull(c, ctx, getCursor().getParentOrThrow());
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
                Set<String> helpers = getCursor().getNearestMessage(HELPERS);
                if (helpers == null) {
                    return m;
                }
                if (m.getSelect() == null && helpers.contains(m.getSimpleName())) {
                    List<Expression> arguments = new ArrayList<>();
                    arguments.add(Stubs.page());
                    for (Expression a : m.getArguments()) {
                        if (!(a instanceof J.Empty)) {
                            arguments.add(a.withPrefix(Space.SINGLE_SPACE));
                        }
                    }
                    return m.withArguments(arguments);
                }
                if (!Dramafinder.isBaseCall(method)) {
                    return m;
                }
                List<Expression> arguments = m.getArguments().stream().filter(a -> !(a instanceof J.Empty)).toList();
                return switch (m.getSimpleName()) {
                    case "navigate" -> onPage(m, "navigate", arguments);
                    case "assertAt" -> pageAssertion(m, "isAt", "java.lang.Class", arguments);
                    case "assertPath" -> pageAssertion(m, "hasPath", "java.lang.String", arguments);
                    case "assertQueryParameter" -> pageAssertion(m, "hasQueryParameter", "java.lang.String", arguments);
                    default -> m; // getPage(): the Escape Hatch's page
                };
            }

            /** {@code page.<name>(..)} with the call's arguments. */
            private J.MethodInvocation onPage(J.MethodInvocation m, String name, List<Expression> arguments) {
                Object[] parameters = new Object[arguments.size() + 1];
                parameters[0] = Stubs.page();
                for (int i = 0; i < arguments.size(); i++) {
                    parameters[i + 1] = arguments.get(i);
                }
                String placeholders = String.join(", ", java.util.Collections.nCopies(arguments.size(), "#{any()}"));
                return JavaTemplate.builder("#{any(" + TestBenchSetupToUnified.UNIFIED_PAGE + ")}." + name + "(" + placeholders + ")")
                        .javaParser(Stubs.parser())
                        .build()
                        .<J.MethodInvocation>apply(getCursor(), m.getCoordinates().replace(), parameters)
                        .withId(m.getId()).withPrefix(m.getPrefix());
            }

            /** {@code assertThat(page).<name>(..)} with the call's arguments. */
            private J.MethodInvocation pageAssertion(J.MethodInvocation m, String name, String argumentType,
                    List<Expression> arguments) {
                maybeAddImport(TestBenchAssertionsToUnified.UNIFIED_ASSERTIONS, "assertThat", false);
                Object[] parameters = new Object[arguments.size() + 1];
                parameters[0] = Stubs.page();
                for (int i = 0; i < arguments.size(); i++) {
                    parameters[i + 1] = arguments.get(i);
                }
                String placeholders = String.join(", ",
                        java.util.Collections.nCopies(arguments.size(), "#{any(" + argumentType + ")}"));
                return JavaTemplate.builder("assertThat(#{any(" + TestBenchSetupToUnified.UNIFIED_PAGE + ")})." + name
                                + "(" + placeholders + ")")
                        .staticImports(TestBenchAssertionsToUnified.UNIFIED_ASSERTIONS + ".assertThat")
                        .javaParser(Stubs.parser())
                        .build()
                        .<J.MethodInvocation>apply(getCursor(), m.getCoordinates().replace(), parameters)
                        .withId(m.getId()).withPrefix(m.getPrefix());
            }
        };
    }

    /**
     * The class's statements with every test that doesn't navigate first starting at the view, as the base class's
     * per-test setup opened it.
     */
    private static List<Statement> startAtView(J.ClassDeclaration original, String view, List<Statement> statements) {
        List<Statement> originals = original.getBody().getStatements();
        List<Statement> result = new ArrayList<>();
        for (Statement s : statements) {
            if (s instanceof J.MethodDeclaration m && Helpers.isBrowserTest(m) && m.getBody() != null
                    && !m.getBody().getStatements().isEmpty() && !navigatesFirst(originals, m)) {
                Space prefix = m.getBody().getStatements().get(0).getPrefix().withComments(emptyList());
                List<Statement> body = new ArrayList<>();
                body.add(navigate(view, prefix));
                body.addAll(m.getBody().getStatements());
                s = m.withBody(m.getBody().withStatements(body));
            }
            result.add(s);
        }
        return result;
    }

    /** Whether the test's own first statement navigates. */
    private static boolean navigatesFirst(List<Statement> originals, J.MethodDeclaration m) {
        return originals.stream().anyMatch(o -> o instanceof J.MethodDeclaration om && om.getId().equals(m.getId())
                && om.getBody() != null && !om.getBody().getStatements().isEmpty()
                && om.getBody().getStatements().get(0) instanceof J.MethodInvocation call
                && call.getSimpleName().equals("navigate"));
    }

    private static J.MethodInvocation navigate(String view, Space prefix) {
        JavaType.Method type = new JavaType.Method(null, 1L, JavaType.ShallowClass.build(TestBenchSetupToUnified.UNIFIED_PAGE),
                "navigate", JavaType.Primitive.Void, List.of("path"), List.of(JavaType.Primitive.String), null, null,
                null, null);
        return new J.MethodInvocation(Tree.randomId(), prefix, Markers.EMPTY, JRightPadded.build(Stubs.page()), null,
                new J.Identifier(Tree.randomId(), Space.EMPTY, Markers.EMPTY, emptyList(), "navigate", type, null),
                JContainer.build(Space.EMPTY, List.of(JRightPadded.<Expression>build(Stubs.literal(view))), Markers.EMPTY),
                type);
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
