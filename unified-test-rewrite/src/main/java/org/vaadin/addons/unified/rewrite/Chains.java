package org.vaadin.addons.unified.rewrite;

import org.openrewrite.Cursor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

/** The calls chained on a TestBench lookup, and whether its Wrapper can express all of them. */
final class Chains {

    /** Wrappers that are a Scope: {@code container.$(X.class)} becomes {@code container.find(UnifiedX.class)}. */
    static final java.util.Set<String> SCOPES = java.util.Set.of("com.vaadin.testing.unified.component.UnifiedDialog");

    private Chains() {
    }

    /**
     * Whether the chain from the cursor on (a {@code $(..)}, or a terminal when {@code afterTerminal}) only uses
     * the Wrapper's filters, then a terminal, then at most one of the Wrapper's methods, or ends in a Unified
     * assertion, a statement of its own, or a local the Wrapper can hold.
     */
    static boolean supported(Cursor start, boolean afterTerminal, Wrappers.Wrapper wrapper) {
        Cursor c = start;
        boolean found = afterTerminal;
        while (true) {
            Cursor parent = c.getParentTreeCursor();
            if (!(parent.getValue() instanceof J.MethodInvocation call) || call.getSelect() != c.getValue()) {
                return found && (parent.getValue() instanceof J.Block
                        || TestBenchAssertionsToUnified.isUnifiedAssertion(parent.getValue())
                        || parent.getValue() instanceof J.VariableDeclarations.NamedVariable variable
                                && Locals.wrapperOf(start.firstEnclosing(J.MethodDeclaration.class), variable) != null);
            }
            String name = call.getSimpleName();
            if (!found && Wrappers.TERMINALS.contains(name)) {
                found = true;
            } else if (!found && wrapper.filters().contains(name)) {
                // a filter before the terminal
            } else {
                return found && wrapper.supports(call, parent);
            }
            c = parent;
        }
    }

    /** The Wrapper for the element class of a {@code $(XElement.class)} call, or {@code null}. */
    static Wrappers.Wrapper ofDollar(J.MethodInvocation dollar) {
        if (!dollar.getSimpleName().equals("$") || dollar.getArguments().size() != 1
                || !(dollar.getArguments().get(0) instanceof J.FieldAccess classLiteral)
                || !(classLiteral.getTarget().getType() instanceof JavaType.FullyQualified element)) {
            return null;
        }
        return Wrappers.forElement(element.getFullyQualifiedName());
    }

    /** A Scope container's {@code $(X.class)} converts when the lookup it starts does. */
    static boolean scopedLookupConverts(J.MethodInvocation call, Cursor callCursor) {
        Wrappers.Wrapper inner = ofDollar(call);
        return inner != null && supported(callCursor, false, inner);
    }
}
