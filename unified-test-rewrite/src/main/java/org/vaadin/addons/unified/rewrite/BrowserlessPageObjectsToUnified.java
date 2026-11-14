package org.vaadin.addons.unified.rewrite;

import static java.util.Collections.emptyList;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.java.tree.TypeTree;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.Markers;

/**
 * Browserless page objects: plain classes that hold the Browserless test in a field ({@code BrowserlessTest},
 * {@code SpringBrowserlessTest}, or the project's base class) and look components up through it. One whose every
 * use of the test converts, and whose callers use what it returns only through Wrapper calls, takes the Unified Page
 * instead: its lookups become {@code page.find(..)}, its methods return Wrappers, and tests pass {@code page} for
 * {@code this}. It needs no Escape Hatch, so its tests stay portable. Any other page object stays as it is, and its
 * tests too ({@link BrowserlessPageObjectsMarked}).
 */
public class BrowserlessPageObjectsToUnified extends ScanningRecipe<BrowserlessPageObjectsToUnified.Found> {

    /** The page objects found, the Wrapper each of their methods returns, and those that can't convert. */
    public record Found(Map<String, Map<String, Browserless.Wrapper>> returnsByPageObject, Set<String> blocked) {
        boolean converts(String fqn) {
            return returnsByPageObject.containsKey(fqn) && !blocked.contains(fqn);
        }
    }

    @Override
    public String getDisplayName() {
        return "Migrate Browserless page objects to the Unified Page";
    }

    @Override
    public String getDescription() {
        return "Page objects that look components up through the Browserless test take the Unified Page instead, "
                + "when every use of the test and every use of what they return converts.";
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
                if (classDecl.getType() == null || Browserless.isBrowserlessClass(classDecl)
                        || testFields(classDecl).isEmpty()) {
                    return super.visitClassDeclaration(classDecl, ctx);
                }
                String fqn = classDecl.getType().getFullyQualifiedName();
                Map<String, Browserless.Wrapper> returns = new HashMap<>();
                for (Statement s : classDecl.getBody().getStatements()) {
                    if (s instanceof J.MethodDeclaration m && m.getReturnTypeExpression() != null
                            && m.getReturnTypeExpression().getType() instanceof JavaType.FullyQualified r
                            && Browserless.forComponent(r.getFullyQualifiedName()) != null) {
                        returns.put(m.getSimpleName(), Browserless.forComponent(r.getFullyQualifiedName()));
                    }
                }
                found.returnsByPageObject().put(fqn, returns);
                getCursor().putMessage(Browserless.PAGE_OBJECT, fqn);
                return super.visitClassDeclaration(classDecl, ctx);
            }

            /** Inside a page object: every use of the field holding the test must convert. */
            @Override
            public J.Identifier visitIdentifier(J.Identifier identifier, ExecutionContext ctx) {
                String pageObject = getCursor().getNearestMessage(Browserless.PAGE_OBJECT);
                if (pageObject == null || identifier.getFieldType() == null || !Browserless.isHandle(identifier)) {
                    return identifier;
                }
                Cursor use = getCursor();
                if (use.getParentTreeCursor().getValue() instanceof J.FieldAccess access && access.getName() == identifier) {
                    use = use.getParentTreeCursor(); // this.test
                }
                Object parent = use.getParentTreeCursor().getValue();
                if (parent instanceof J.VariableDeclarations.NamedVariable
                        || parent instanceof J.Assignment // this.test = test, in the constructor
                        || parent instanceof J.FieldAccess) {
                    return identifier;
                }
                if (!(parent instanceof J.MethodInvocation call) || call.getSelect() != use.getValue()
                        || !converts(new Cursor(use.getParentTreeCursor().getParentOrThrow(), call))) {
                    found.blocked().add(pageObject);
                }
                return identifier;
            }

            /** A call on the test: a lookup that converts, {@code test(..)} of one, or a {@code navigate(..)} statement. */
            private boolean converts(Cursor callCursor) {
                J.MethodInvocation call = (J.MethodInvocation) callCursor.getValue();
                if (Browserless.ofLookupStart(call) != null) {
                    Cursor terminal = Browserless.terminalOf(callCursor);
                    return terminal != null && Browserless.convertibleLookup(terminal) != null;
                }
                if (Browserless.isTest(call)) {
                    return Browserless.convertibleElement(callCursor, call.getArguments().get(0)) != null;
                }
                return call.getSimpleName().equals("navigate") && callCursor.getParentTreeCursor().getValue() instanceof J.Block
                        && call.getArguments().get(0) instanceof J.FieldAccess;
            }

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                // a caller of a page object method returning a component uses it only through the Wrapper
                JavaType.Method type = method.getMethodType();
                if (type != null) {
                    String owner = type.getDeclaringType().getFullyQualifiedName();
                    Map<String, Browserless.Wrapper> returns = found.returnsByPageObject().get(owner);
                    Browserless.Wrapper w = returns == null ? null : returns.get(method.getSimpleName());
                    if (w != null && !Browserless.usedThrough(getCursor(), w)) {
                        found.blocked().add(owner);
                    }
                }
                return m;
            }

            /** {@code new LoginPage(this)} only from a Browserless test, which then has the page to pass. */
            @Override
            public J.NewClass visitNewClass(J.NewClass newClass, ExecutionContext ctx) {
                J.NewClass n = super.visitNewClass(newClass, ctx);
                JavaType.FullyQualified type = TypeUtils.asFullyQualified(newClass.getType());
                J.ClassDeclaration enclosing = getCursor().firstEnclosing(J.ClassDeclaration.class);
                if (type != null && passesThis(newClass)
                        && (enclosing == null || !Browserless.isBrowserlessClass(enclosing))) {
                    found.blocked().add(type.getFullyQualifiedName());
                }
                return n;
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Found found) {
        return new JavaIsoVisitor<>() {
            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                if (classDecl.getType() == null || !found.converts(classDecl.getType().getFullyQualifiedName())) {
                    return super.visitClassDeclaration(classDecl, ctx);
                }
                // the page object's lookups and tester calls, as in a test, starting from its own field; the message
                // goes on the compilation unit, which the lookup visitor's cursors descend from
                getCursor().getParentOrThrow().putMessage(Browserless.PAGE_OBJECT, classDecl.getType().getFullyQualifiedName());
                J.ClassDeclaration c = (J.ClassDeclaration) new BrowserlessLookupsToUnified().getVisitor()
                        .visitNonNull(classDecl, ctx, getCursor().getParentOrThrow());
                c = super.visitClassDeclaration(c, ctx);
                for (String base : Set.of(Browserless.BROWSERLESS_TEST, Browserless.SPRING_BROWSERLESS_TEST,
                        Browserless.BASE_TEST)) {
                    doAfterVisit(new org.openrewrite.java.RemoveImport<>(base, true));
                }
                maybeAddImport(TestBenchSetupToUnified.UNIFIED_PAGE);
                // the lookup visitor ran on its own: its import changes are made here
                Browserless.wrappersByComponent().forEach((component, w) -> {
                    maybeAddImport(w.fqn());
                    maybeRemoveImport(component);
                });
                return c;
            }

            /** The field and constructor parameter holding the test hold the Unified Page. */
            @Override
            public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                J.VariableDeclarations v = super.visitVariableDeclarations(multiVariable, ctx);
                if (!inConvertingPageObject() || v.getTypeExpression() == null || v.getType() == null
                        || !TypeUtils.isAssignableTo(Browserless.BASE_TEST, v.getType())) {
                    return v;
                }
                return v.withTypeExpression(typeName(v.getTypeExpression(), "UnifiedPage", TestBenchSetupToUnified.UNIFIED_PAGE));
            }

            /** A method returning a component returns its Wrapper. */
            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                J.MethodDeclaration m = super.visitMethodDeclaration(method, ctx);
                if (!inConvertingPageObject() || m.getReturnTypeExpression() == null
                        || !(m.getReturnTypeExpression().getType() instanceof JavaType.FullyQualified r)
                        || Browserless.forComponent(r.getFullyQualifiedName()) == null) {
                    return m;
                }
                Browserless.Wrapper w = Browserless.forComponent(r.getFullyQualifiedName());
                maybeAddImport(w.fqn());
                maybeRemoveImport(r.getFullyQualifiedName());
                return m.withReturnTypeExpression(typeName(m.getReturnTypeExpression(), w.simpleName(), w.fqn()));
            }

            /** Callers of a converted page object: its methods return Wrappers, and {@code this} becomes the page. */
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                JavaType.Method type = m.getMethodType();
                if (type == null || !found.converts(type.getDeclaringType().getFullyQualifiedName())) {
                    return m;
                }
                Browserless.Wrapper w = found.returnsByPageObject().get(type.getDeclaringType().getFullyQualifiedName())
                        .get(m.getSimpleName());
                if (w == null) {
                    return m;
                }
                JavaType.Method returning = type.withReturnType(JavaType.ShallowClass.build(w.fqn()));
                return m.withMethodType(returning).withName(m.getName().withType(returning));
            }

            @Override
            public J.NewClass visitNewClass(J.NewClass newClass, ExecutionContext ctx) {
                J.NewClass n = super.visitNewClass(newClass, ctx);
                JavaType.FullyQualified type = TypeUtils.asFullyQualified(newClass.getType());
                if (type == null || !found.converts(type.getFullyQualifiedName())) {
                    return n;
                }
                return n.withArguments(ListUtils.map(n.getArguments(),
                        a -> a instanceof J.Identifier id && id.getSimpleName().equals("this")
                                ? Stubs.page().withPrefix(a.getPrefix()) : a));
            }

            private boolean inConvertingPageObject() {
                J.ClassDeclaration c = getCursor().firstEnclosing(J.ClassDeclaration.class);
                return c != null && c.getType() != null && found.converts(c.getType().getFullyQualifiedName());
            }
        };
    }

    /** The class's fields holding the Browserless test. */
    private static Set<String> testFields(J.ClassDeclaration c) {
        Set<String> names = new HashSet<>();
        for (Statement s : c.getBody().getStatements()) {
            if (s instanceof J.VariableDeclarations v && v.getType() != null
                    && TypeUtils.isAssignableTo(Browserless.BASE_TEST, v.getType())) {
                v.getVariables().forEach(var -> names.add(var.getSimpleName()));
            }
        }
        return names;
    }

    private static boolean passesThis(J.NewClass n) {
        return n.getArguments().stream().anyMatch(a -> a instanceof J.Identifier id && id.getSimpleName().equals("this"));
    }

    private static TypeTree typeName(TypeTree old, String simpleName, String fqn) {
        return new J.Identifier(Tree.randomId(), old.getPrefix(), Markers.EMPTY, emptyList(), simpleName,
                JavaType.ShallowClass.build(fqn), null);
    }
}
