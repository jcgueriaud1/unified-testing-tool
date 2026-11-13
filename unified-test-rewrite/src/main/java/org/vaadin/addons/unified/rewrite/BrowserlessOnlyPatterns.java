package org.vaadin.addons.unified.rewrite;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.Comment;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.TextComment;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.Markers;

/**
 * Browserless tests that rely on Browserless running the views in the test's thread and JVM
 * ({@code docs/spec/02-test-setup.md}) keep running on Browserless only: a {@code @Transactional} test, and a
 * test on a narrow Spring context ({@code SpringBrowserlessTest} without {@code @SpringBootTest}). They get
 * {@code @RequiresTestingTool(TestingTool.BROWSERLESS)} and a Migration Marker naming the pattern; the narrow
 * context keeps its Spring extension, which {@code SpringBrowserlessTest} brought.
 */
public class BrowserlessOnlyPatterns extends Recipe {

    private static final Set<String> TRANSACTIONAL = Set.of("org.springframework.transaction.annotation.Transactional",
            "jakarta.transaction.Transactional");
    private static final String SPRING_BOOT_TEST = "org.springframework.boot.test.context.SpringBootTest";
    private static final String EXTEND_WITH = "org.junit.jupiter.api.extension.ExtendWith";
    private static final String SPRING_EXTENSION = "org.springframework.test.context.junit.jupiter.SpringExtension";
    private static final String TRANSACTIONAL_MARKER = "Browserless-only pattern: @Transactional rolls back what the "
            + "views write only on Browserless; commit test data and clean up to run everywhere (spec 02)";
    private static final String NARROW_CONTEXT_MARKER = "Browserless-only pattern: a Spring context without "
            + "@SpringBootTest has no web server; use @UnifiedSpringBootTest with @MockitoBean to run everywhere (spec 02)";

    @Override
    public String getDisplayName() {
        return "Keep Browserless-only Browserless tests on Browserless";
    }

    @Override
    public String getDescription() {
        return "Makes `@Transactional` Browserless tests and tests on a narrow Spring context require Browserless, "
                + "with a Migration Marker naming the pattern.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<>() {
            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                J.ClassDeclaration c = super.visitClassDeclaration(classDecl, ctx);
                if (!Browserless.isBrowserlessClass(classDecl)
                        || annotated(classDecl.getLeadingAnnotations(), Set.of(TestBenchLeftoversToEscapeHatch.REQUIRES_TESTING_TOOL))) {
                    return c;
                }
                boolean narrowContext = classDecl.getExtends() != null
                        && TypeUtils.isOfClassType(classDecl.getExtends().getType(), Browserless.SPRING_BROWSERLESS_TEST)
                        && !annotated(classDecl.getLeadingAnnotations(), Set.of(SPRING_BOOT_TEST));
                List<String> markers = new ArrayList<>();
                if (narrowContext) {
                    maybeAddImport(EXTEND_WITH);
                    maybeAddImport(SPRING_EXTENSION);
                    c = JavaTemplate.builder("@ExtendWith(SpringExtension.class)")
                            .imports(EXTEND_WITH, SPRING_EXTENSION)
                            .javaParser(Stubs.parser(
                                    "package org.junit.jupiter.api.extension; public @interface ExtendWith { Class<?>[] value(); }",
                                    "package org.springframework.test.context.junit.jupiter; public class SpringExtension {}"))
                            .build()
                            .apply(updateCursor(c), c.getCoordinates().addAnnotation((a, b) -> 1));
                    markers.add(NARROW_CONTEXT_MARKER);
                }
                if (transactional(classDecl.getLeadingAnnotations())) {
                    markers.add(TRANSACTIONAL_MARKER);
                }
                if (markers.isEmpty()) {
                    return c;
                }
                c = requiresBrowserless(c, c.getCoordinates().addAnnotation((a, b) -> 1));
                return c.withLeadingAnnotations(marked(c.getLeadingAnnotations(), markers, c.getPrefix().getIndent()));
            }

            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                J.MethodDeclaration m = super.visitMethodDeclaration(method, ctx);
                J.ClassDeclaration c = getCursor().firstEnclosing(J.ClassDeclaration.class);
                if (c == null || !Browserless.isBrowserlessClass(c) || !transactional(m.getLeadingAnnotations())
                        || annotated(m.getLeadingAnnotations(), Set.of(TestBenchLeftoversToEscapeHatch.REQUIRES_TESTING_TOOL))) {
                    return m;
                }
                m = requiresBrowserless(m, m.getCoordinates().addAnnotation((a, b) -> 1));
                return m.withLeadingAnnotations(marked(m.getLeadingAnnotations(), List.of(TRANSACTIONAL_MARKER),
                        m.getPrefix().getIndent()));
            }

            private <T extends J> T requiresBrowserless(T tree, org.openrewrite.java.tree.JavaCoordinates where) {
                maybeAddImport(TestBenchLeftoversToEscapeHatch.REQUIRES_TESTING_TOOL);
                maybeAddImport(TestBenchLeftoversToEscapeHatch.TESTING_TOOL);
                return JavaTemplate.builder("@RequiresTestingTool(TestingTool.BROWSERLESS)")
                        .imports(TestBenchLeftoversToEscapeHatch.REQUIRES_TESTING_TOOL, TestBenchLeftoversToEscapeHatch.TESTING_TOOL)
                        .javaParser(Stubs.parser())
                        .build()
                        .apply(updateCursor(tree), where);
            }
        };
    }

    /** {@code @Transactional}, unless its propagation keeps the test out of a transaction. */
    private static boolean transactional(List<J.Annotation> annotations) {
        return annotations.stream().anyMatch(a -> TRANSACTIONAL.stream().anyMatch(t -> TypeUtils.isOfClassType(a.getType(), t))
                && (a.getArguments() == null || a.getArguments().stream().noneMatch(arg -> {
                    String printed = arg.printTrimmed();
                    return printed.contains("NOT_SUPPORTED") || printed.contains("NEVER");
                })));
    }

    private static boolean annotated(List<J.Annotation> annotations, Set<String> types) {
        return annotations.stream().anyMatch(a -> types.stream().anyMatch(t -> TypeUtils.isOfClassType(a.getType(), t)));
    }

    /** The annotations with the Migration Markers above the first one, at the declaration's indent. */
    private static List<J.Annotation> marked(List<J.Annotation> annotations, List<String> markers, String indent) {
        List<J.Annotation> result = new ArrayList<>(annotations);
        J.Annotation first = result.get(0);
        List<Comment> comments = new ArrayList<>(first.getPrefix().getComments());
        String whitespace = first.getPrefix().getWhitespace();
        for (String text : markers) {
            comments.add(new TextComment(false, " TODO unified: " + text, "\n" + indent, Markers.EMPTY));
        }
        result.set(0, first.withPrefix(Space.build(whitespace, comments)));
        return result;
    }
}
