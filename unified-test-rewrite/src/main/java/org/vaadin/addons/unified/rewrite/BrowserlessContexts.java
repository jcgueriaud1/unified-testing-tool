package org.vaadin.addons.unified.rewrite;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.java.tree.TypeUtils;

/**
 * The wiring of a context-API or extension Browserless test, which the Unified Page replaces: the fields holding
 * the application, user, window or extension; a {@code @TestConfiguration} that builds the application; and the
 * lifecycle methods that only open or close them. What the wiring knew about the views ({@code create(View.class)},
 * {@code withViewPackages(..)}) becomes {@code @ViewPackages}.
 */
final class BrowserlessContexts {

    private static final Set<String> LIFECYCLE = Set.of("org.junit.jupiter.api.BeforeEach",
            "org.junit.jupiter.api.AfterEach", "org.junit.jupiter.api.BeforeAll", "org.junit.jupiter.api.AfterAll");
    private static final String TEST_CONFIGURATION = "org.springframework.boot.test.context.TestConfiguration";
    private static final Set<String> VIEW_SOURCES = Set.of("create", "withViewPackages");

    private BrowserlessContexts() {
    }

    /** The names of the class's fields holding the context API's objects or the extension. */
    static Set<String> fields(J.ClassDeclaration c) {
        Set<String> names = new LinkedHashSet<>();
        for (Statement s : c.getBody().getStatements()) {
            if (s instanceof J.VariableDeclarations v && isContextType(v)) {
                v.getVariables().forEach(var -> names.add(var.getSimpleName()));
            }
        }
        return names;
    }

    /** The names of the fields holding a window or the extension: the handles the tests call. */
    static Set<String> handles(J.ClassDeclaration c) {
        Set<String> names = new LinkedHashSet<>();
        for (Statement s : c.getBody().getStatements()) {
            if (s instanceof J.VariableDeclarations v && v.getType() != null
                    && (TypeUtils.isAssignableTo(Browserless.UI_CONTEXT, v.getType())
                            || TypeUtils.isAssignableTo(Browserless.EXTENSION, v.getType()))) {
                v.getVariables().forEach(var -> names.add(var.getSimpleName()));
            }
        }
        return names;
    }

    /** Whether the statement is wiring: a context field, a context-building configuration, or a lifecycle method. */
    static boolean isWiring(Statement s, Set<String> fields) {
        if (s instanceof J.VariableDeclarations v) {
            return isContextType(v);
        }
        if (s instanceof J.ClassDeclaration nested) {
            return annotated(nested.getLeadingAnnotations(), Set.of(TEST_CONFIGURATION))
                    && nested.getBody().getStatements().stream().allMatch(m -> m instanceof J.MethodDeclaration md
                            && md.getReturnTypeExpression() != null
                            && Browserless.CONTEXT_TYPES.stream().anyMatch(t -> TypeUtils.isAssignableTo(t,
                                    md.getReturnTypeExpression().getType())));
        }
        return s instanceof J.MethodDeclaration m && m.getBody() != null
                && annotated(m.getLeadingAnnotations(), LIFECYCLE)
                && m.getBody().getStatements().stream().allMatch(st -> onlyWiring(st, fields));
    }

    /**
     * Whether a class can be migrated: outside its wiring it uses only the handles, not the application or user
     * (a second user or window is the Unified Page's {@code openAsUser(..)} / {@code openWindow()}, by hand).
     */
    static boolean migratable(J.ClassDeclaration c) {
        Set<String> fields = fields(c);
        Set<String> others = new LinkedHashSet<>(fields);
        others.removeAll(handles(c));
        AtomicBoolean uses = new AtomicBoolean();
        for (Statement s : c.getBody().getStatements()) {
            if (isWiring(s, fields)) {
                continue;
            }
            new JavaIsoVisitor<AtomicBoolean>() {
                @Override
                public J.Identifier visitIdentifier(J.Identifier identifier, AtomicBoolean found) {
                    if (others.contains(identifier.getSimpleName()) && identifier.getFieldType() != null) {
                        found.set(true);
                    }
                    return identifier;
                }
            }.visit(s, uses);
        }
        return !uses.get();
    }

    /** The views the wiring names, as {@code @ViewPackages} arguments: class literals, or package names. */
    static List<Expression> viewSources(J.ClassDeclaration c) {
        List<Expression> sources = new ArrayList<>();
        Set<String> fields = fields(c);
        for (Statement s : c.getBody().getStatements()) {
            if (!isWiring(s, fields)) {
                continue;
            }
            new JavaIsoVisitor<List<Expression>>() {
                @Override
                public J.MethodInvocation visitMethodInvocation(J.MethodInvocation call, List<Expression> found) {
                    if (VIEW_SOURCES.contains(call.getSimpleName()) && call.getMethodType() != null
                            && call.getMethodType().getDeclaringType().getFullyQualifiedName().startsWith("com.vaadin.browserless.")) {
                        call.getArguments().stream().filter(a -> !(a instanceof J.Empty)).forEach(found::add);
                    }
                    return super.visitMethodInvocation(call, found);
                }
            }.visit(s, sources);
        }
        return sources;
    }

    private static boolean onlyWiring(Statement s, Set<String> fields) {
        Expression e = s instanceof J.Assignment a ? a.getVariable() : s instanceof Expression x ? x : null;
        return e != null && fields.contains(root(e));
    }

    /** The identifier a call chain or field access starts from. */
    private static String root(Expression e) {
        while (true) {
            if (e instanceof J.Identifier id) {
                return id.getSimpleName();
            } else if (e instanceof J.MethodInvocation call && call.getSelect() != null) {
                e = call.getSelect();
            } else if (e instanceof J.FieldAccess access && !(access.getTarget() instanceof J.Identifier t
                    && t.getSimpleName().equals("this"))) {
                e = access.getTarget();
            } else if (e instanceof J.FieldAccess access) {
                return access.getSimpleName();
            } else {
                return null;
            }
        }
    }

    private static boolean isContextType(J.VariableDeclarations v) {
        return v.getType() != null && Browserless.CONTEXT_TYPES.stream().anyMatch(t -> TypeUtils.isAssignableTo(t, v.getType()));
    }

    private static boolean annotated(List<J.Annotation> annotations, Set<String> types) {
        return annotations.stream().anyMatch(a -> types.stream().anyMatch(t -> TypeUtils.isOfClassType(a.getType(), t)));
    }
}
