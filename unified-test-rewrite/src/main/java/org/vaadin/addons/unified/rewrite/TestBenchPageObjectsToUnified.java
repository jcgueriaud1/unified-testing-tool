package org.vaadin.addons.unified.rewrite;

import static java.util.Collections.emptyList;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeTree;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.Markers;

/**
 * Page objects: plain classes that hold the TestBench test ({@code HasElementQuery}, {@code TestBenchTestCase},
 * {@code BrowserTestBase}) and look components up through it. One whose every lookup a Wrapper can express, and
 * whose callers use what it returns only through the Wrappers, takes the Unified Page instead; its tests pass
 * {@code page} for {@code this}. Any other page object stays TestBench.
 */
public class TestBenchPageObjectsToUnified extends ScanningRecipe<TestBenchPageObjectsToUnified.Found> {

    static final Set<String> CONTEXTS = Set.of("com.vaadin.testbench.HasElementQuery",
            "com.vaadin.testbench.TestBenchTestCase", "com.vaadin.testbench.BrowserTestBase",
            "com.vaadin.testbench.AbstractBrowserTestBase");

    /** Candidate page objects; those that can't convert are removed while scanning. */
    public record Found(Map<String, Map<String, Wrappers.Wrapper>> returnsByPageObject, Set<String> blocked) {
        boolean converts(String fqn) {
            return returnsByPageObject.containsKey(fqn) && !blocked.contains(fqn);
        }
    }

    @Override
    public String getDisplayName() {
        return "Migrate TestBench page objects to the Unified Page";
    }

    @Override
    public String getDescription() {
        return "Page objects that look components up through the TestBench test take the Unified Page instead, "
                + "when every lookup and every use of what they return converts.";
    }

    @Override
    public Found getInitialValue(ExecutionContext ctx) {
        return new Found(new HashMap<>(), new HashSet<>());
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Found found) {
        return new JavaIsoVisitor<>() {
            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                J.ClassDeclaration c = super.visitClassDeclaration(classDecl, ctx);
                if (classDecl.getType() == null || TestBenchSetupToUnified.isTestBenchClass(classDecl)) {
                    return c;
                }
                Set<String> fields = contextFields(classDecl);
                if (fields.isEmpty()) {
                    return c;
                }
                String fqn = classDecl.getType().getFullyQualifiedName();
                Map<String, Wrappers.Wrapper> returns = new HashMap<>();
                found.returnsByPageObject().put(fqn, returns);
                for (var statement : classDecl.getBody().getStatements()) {
                    if (statement instanceof J.MethodDeclaration m && m.getReturnTypeExpression() != null
                            && TypeUtils.asFullyQualified(m.getReturnTypeExpression().getType()) instanceof JavaType.FullyQualified r) {
                        Wrappers.Wrapper w = Wrappers.forElement(r.getFullyQualifiedName());
                        if (w != null) {
                            returns.put(m.getSimpleName(), w);
                        }
                    }
                }
                if (!usesContextOnlyForLookups(classDecl, fields)) {
                    found.blocked().add(fqn);
                }
                return c;
            }

            @Override
            public J.NewClass visitNewClass(J.NewClass newClass, ExecutionContext ctx) {
                J.NewClass n = super.visitNewClass(newClass, ctx);
                JavaType.FullyQualified type = TypeUtils.asFullyQualified(newClass.getType());
                if (type != null && newClass.getArguments().stream().anyMatch(a -> a instanceof J.Identifier id
                        && id.getSimpleName().equals("this"))) {
                    J.MethodDeclaration m = getCursor().firstEnclosing(J.MethodDeclaration.class);
                    if (m == null || !Helpers.isBrowserTest(m)) {
                        found.blocked().add(type.getFullyQualifiedName());
                    }
                }
                return n;
            }

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                JavaType.Method type = method.getMethodType();
                if (type == null) {
                    return m;
                }
                String owner = type.getDeclaringType().getFullyQualifiedName();
                Map<String, Wrappers.Wrapper> returns = found.returnsByPageObject().get(owner);
                Wrappers.Wrapper w = returns == null ? null : returns.get(method.getSimpleName());
                if (w != null) {
                    Object parent = getCursor().getParentTreeCursor().getValue();
                    boolean ok = parent instanceof J.Block || parent instanceof J.Return
                            || parent instanceof J.MethodInvocation next && next.getSelect() == method && w.has(next.getSimpleName());
                    if (!ok) {
                        found.blocked().add(owner);
                    }
                }
                return m;
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Found found) {
        return new JavaIsoVisitor<>() {
            private boolean inPageObject() {
                J.ClassDeclaration c = getCursor().firstEnclosing(J.ClassDeclaration.class);
                return c != null && c.getType() != null && found.converts(c.getType().getFullyQualifiedName());
            }

            @Override
            public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                J.VariableDeclarations v = super.visitVariableDeclarations(multiVariable, ctx);
                if (inPageObject() && v.getTypeExpression() != null && isContext(v.getTypeExpression().getType())) {
                    maybeAddImport(TestBenchSetupToUnified.UNIFIED_PAGE);
                    // forced: the field's identifiers still carry the TestBench type until the next parse
                    doAfterVisit(new org.openrewrite.java.RemoveImport<>(
                            TypeUtils.asFullyQualified(v.getTypeExpression().getType()).getFullyQualifiedName(), true));
                    return v.withTypeExpression(typeName(v.getTypeExpression(), "UnifiedPage", TestBenchSetupToUnified.UNIFIED_PAGE));
                }
                return v;
            }

            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                J.MethodDeclaration m = super.visitMethodDeclaration(method, ctx);
                if (inPageObject() && m.getReturnTypeExpression() != null) {
                    JavaType.FullyQualified r = TypeUtils.asFullyQualified(m.getReturnTypeExpression().getType());
                    Wrappers.Wrapper w = r == null ? null : Wrappers.forElement(r.getFullyQualifiedName());
                    if (w != null) {
                        maybeAddImport(w.fqn());
                        maybeRemoveImport(r.getFullyQualifiedName());
                        return m.withReturnTypeExpression(typeName(m.getReturnTypeExpression(), w.simpleName(), w.fqn()));
                    }
                }
                return m;
            }

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                if (!inPageObject() || !method.getSimpleName().equals("$") || !(method.getSelect() instanceof J.Identifier context)
                        || !isContext(context.getType())) {
                    return m;
                }
                Wrappers.Wrapper w = Chains.ofDollar(method);
                JavaType.FullyQualified element = TypeUtils.asFullyQualified(
                        ((J.FieldAccess) method.getArguments().get(0)).getTarget().getType());
                maybeAddImport(w.fqn());
                maybeRemoveImport(element.getFullyQualifiedName());
                return JavaTemplate.builder("#{any(" + TestBenchSetupToUnified.UNIFIED_PAGE + ")}.find(" + w.simpleName() + ".class)")
                        .imports(w.fqn())
                        .javaParser(Stubs.parser(Stubs.interfaceStub(w.fqn())))
                        .build()
                        .apply(getCursor(), m.getCoordinates().replace(), m.getSelect());
            }

            @Override
            public J.NewClass visitNewClass(J.NewClass newClass, ExecutionContext ctx) {
                J.NewClass n = super.visitNewClass(newClass, ctx);
                JavaType.FullyQualified type = TypeUtils.asFullyQualified(newClass.getType());
                if (type == null || !found.converts(type.getFullyQualifiedName())) {
                    return n;
                }
                return n.withArguments(org.openrewrite.internal.ListUtils.map(n.getArguments(),
                        a -> a instanceof J.Identifier id && id.getSimpleName().equals("this")
                                ? Stubs.page().withPrefix(a.getPrefix()) : a));
            }
        };
    }

    private static boolean isContext(JavaType type) {
        JavaType.FullyQualified t = TypeUtils.asFullyQualified(type);
        return t != null && CONTEXTS.contains(t.getFullyQualifiedName());
    }

    private static TypeTree typeName(TypeTree old, String simpleName, String fqn) {
        return new J.Identifier(Tree.randomId(), old.getPrefix(), Markers.EMPTY, emptyList(), simpleName,
                JavaType.ShallowClass.build(fqn), null);
    }

    /** The names of the class's fields holding the TestBench test. */
    private static Set<String> contextFields(J.ClassDeclaration c) {
        Set<String> names = new HashSet<>();
        for (var statement : c.getBody().getStatements()) {
            if (statement instanceof J.VariableDeclarations v && v.getTypeExpression() != null
                    && isContext(v.getTypeExpression().getType())) {
                v.getVariables().forEach(var -> names.add(var.getSimpleName()));
            }
        }
        return names;
    }

    /** Whether the context is only assigned and used for lookups a Wrapper can express. */
    private static boolean usesContextOnlyForLookups(J.ClassDeclaration c, Set<String> fields) {
        AtomicBoolean other = new AtomicBoolean();
        new JavaIsoVisitor<AtomicBoolean>() {
            @Override
            public J.Identifier visitIdentifier(J.Identifier identifier, AtomicBoolean bad) {
                if (!fields.contains(identifier.getSimpleName()) || !(identifier.getFieldType() != null || isContext(identifier.getType()))) {
                    return identifier;
                }
                Cursor parentCursor = getCursor().getParentTreeCursor();
                Object parent = parentCursor.getValue();
                if (parent instanceof J.VariableDeclarations.NamedVariable || parent instanceof J.FieldAccess
                        || parent instanceof J.Assignment) {
                    return identifier;
                }
                if (parent instanceof J.MethodInvocation call && call.getSelect() == identifier
                        && Chains.ofDollar(call) != null && Chains.supported(parentCursor, false, Chains.ofDollar(call))) {
                    return identifier;
                }
                if (parent instanceof J.MethodInvocation call && call.getSelect() == identifier
                        && Chains.ofDollar(call) != null && returnsTheElement(parentCursor)) {
                    return identifier;
                }
                bad.set(true);
                return identifier;
            }
        }.visit(c, other);
        return !other.get();
    }

    /** Whether the lookup at the cursor is returned: filters and a terminal, then {@code return}. */
    private static boolean returnsTheElement(Cursor dollar) {
        Wrappers.Wrapper w = Chains.ofDollar((J.MethodInvocation) dollar.getValue());
        Cursor c = dollar;
        boolean terminal = false;
        while (c.getParentTreeCursor().getValue() instanceof J.MethodInvocation call && call.getSelect() == c.getValue()) {
            if (!terminal && Wrappers.TERMINALS.contains(call.getSimpleName())) {
                terminal = call.getSimpleName().equals("single");
            } else if (terminal || !w.filters().contains(call.getSimpleName())) {
                return false;
            }
            c = c.getParentTreeCursor();
        }
        return terminal && c.getParentTreeCursor().getValue() instanceof J.Return;
    }
}
