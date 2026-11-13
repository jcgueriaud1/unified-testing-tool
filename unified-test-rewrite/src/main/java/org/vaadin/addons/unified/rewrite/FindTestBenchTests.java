package org.vaadin.addons.unified.rewrite;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.SearchResult;

/**
 * Finds TestBench test classes: classes that are or extend {@code BrowserTestBase}, directly or through a base
 * class of the project. The precondition of the test code recipes.
 */
public class FindTestBenchTests extends Recipe {

    @Override
    public String getDisplayName() {
        return "Find TestBench test classes";
    }

    @Override
    public String getDescription() {
        return "Finds classes that extend TestBench's `BrowserTestBase`, directly or through a base class.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<>() {
            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                J.ClassDeclaration c = super.visitClassDeclaration(classDecl, ctx);
                return TestBenchSetupToUnified.isTestBenchClass(classDecl) ? SearchResult.found(c) : c;
            }
        };
    }
}
