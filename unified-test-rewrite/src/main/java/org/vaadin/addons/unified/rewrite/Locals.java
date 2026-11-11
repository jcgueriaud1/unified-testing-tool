package org.vaadin.addons.unified.rewrite;

import java.util.concurrent.atomic.AtomicBoolean;
import org.openrewrite.Cursor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;

/**
 * Local variables holding a TestBench element: one initialized with a lookup the Wrapper can express, and used
 * only through the Wrapper's methods, takes the Wrapper's type.
 */
final class Locals {

    private Locals() {
    }

    /** The Wrapper the variable converts to, or {@code null}. */
    static Wrappers.Wrapper wrapperOf(J.MethodDeclaration method, J.VariableDeclarations.NamedVariable variable) {
        Wrappers.Wrapper wrapper = Wrappers.ofLookup(variable.getInitializer());
        return wrapper != null && usedOnlyThrough(method, variable.getSimpleName(), wrapper) ? wrapper : null;
    }

    /** The Wrapper the local variable named by {@code id} converts to, or {@code null}. */
    static Wrappers.Wrapper wrapperOf(Cursor cursor, J.Identifier id) {
        J.MethodDeclaration method = cursor.firstEnclosing(J.MethodDeclaration.class);
        if (method == null || method.getBody() == null) {
            return null;
        }
        for (var statement : method.getBody().getStatements()) {
            if (statement instanceof J.VariableDeclarations declarations) {
                for (var variable : declarations.getVariables()) {
                    if (variable.getSimpleName().equals(id.getSimpleName())) {
                        return declarations.getVariables().size() == 1 ? wrapperOf(method, variable) : null;
                    }
                }
            }
        }
        return null;
    }

    private static boolean usedOnlyThrough(J.MethodDeclaration method, String name, Wrappers.Wrapper wrapper) {
        AtomicBoolean other = new AtomicBoolean();
        new JavaIsoVisitor<AtomicBoolean>() {
            @Override
            public J.Identifier visitIdentifier(J.Identifier identifier, AtomicBoolean found) {
                Object parent = getCursor().getParentTreeCursor().getValue();
                if (identifier.getSimpleName().equals(name)
                        && !(parent instanceof J.VariableDeclarations.NamedVariable)
                        && !(parent instanceof J.MethodInvocation call && call.getSelect() == identifier
                                && wrapper.methods().contains(call.getSimpleName()))
                        && !TestBenchAssertionsToUnified.isUnifiedAssertion(parent)) {
                    found.set(true);
                }
                return identifier;
            }
        }.visit(method.getBody(), other);
        return !other.get();
    }
}
