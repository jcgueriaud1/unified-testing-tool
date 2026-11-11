package org.vaadin.addons.unified.rewrite;

import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeUtils;

/** Where the setup recipe gave a method the Unified Page. */
final class Pages {

    private Pages() {
    }

    static boolean hasPageParameter(J.MethodDeclaration method) {
        return method.getParameters().stream().anyMatch(p -> p instanceof J.VariableDeclarations v
                && TypeUtils.isOfClassType(v.getType(), TestBenchSetupToUnified.UNIFIED_PAGE));
    }
}
