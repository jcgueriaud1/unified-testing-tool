package org.vaadin.addons.unified.rewrite;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.SearchResult;

/**
 * Finds Browserless test classes: classes that extend {@code SpringBrowserlessTest} or {@code BrowserlessTest},
 * directly or through a base class of the project, and that don't hand themselves to a page object. The precondition
 * of the Browserless test code recipes.
 */
public class FindBrowserlessTests extends Recipe {

    @Override
    public String getDisplayName() {
        return "Find Browserless test classes";
    }

    @Override
    public String getDescription() {
        return "Finds classes that extend Browserless Test's `SpringBrowserlessTest` or `BrowserlessTest`.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<>() {
            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                J.ClassDeclaration c = super.visitClassDeclaration(classDecl, ctx);
                return Browserless.isBrowserlessClass(classDecl) && !Browserless.passesItself(classDecl)
                        ? SearchResult.found(c) : c;
            }
        };
    }
}
