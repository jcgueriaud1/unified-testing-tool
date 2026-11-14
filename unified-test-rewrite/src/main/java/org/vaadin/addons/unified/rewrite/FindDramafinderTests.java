package org.vaadin.addons.unified.rewrite;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.SearchResult;

/**
 * Finds Dramafinder test classes the recipes can migrate: classes that extend {@code AbstractBasePlaywrightIT} or
 * {@code SpringPlaywrightIT} directly and use no base class member the recipes don't convert. The precondition of
 * the Dramafinder test code recipes.
 */
public class FindDramafinderTests extends Recipe {

    @Override
    public String getDisplayName() {
        return "Find Dramafinder test classes";
    }

    @Override
    public String getDescription() {
        return "Finds classes that extend Dramafinder's `AbstractBasePlaywrightIT` or `SpringPlaywrightIT`.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<>() {
            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                J.ClassDeclaration c = super.visitClassDeclaration(classDecl, ctx);
                return Dramafinder.isDramafinderClass(classDecl) && Dramafinder.migratable(classDecl)
                        ? SearchResult.found(c) : c;
            }
        };
    }
}
