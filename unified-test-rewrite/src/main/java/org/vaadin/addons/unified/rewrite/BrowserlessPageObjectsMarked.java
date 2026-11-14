package org.vaadin.addons.unified.rewrite;

import java.util.ArrayList;
import java.util.List;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Comment;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.TextComment;
import org.openrewrite.marker.Markers;

/**
 * A Browserless test class the migration skips gets a Migration Marker saying so, above the class: one that hands
 * itself ({@code this}) to a page object or helper, which without its test base class would not compile, and a
 * context-API test that uses its application or user context beyond opening a window.
 */
public class BrowserlessPageObjectsMarked extends Recipe {

    static final String MARKER = "not migrated: this class passes itself (this) to a page object or helper; "
            + "make them take a UnifiedPage, then run the migration again";
    static final String CONTEXT_MARKER = "not migrated: this test uses the Browserless application or user context "
            + "beyond opening a window; use page.openWindow() or page.openAsUser(..) instead, then run the migration again";

    @Override
    public String getDisplayName() {
        return "Mark Browserless tests that pass themselves to a page object";
    }

    @Override
    public String getDescription() {
        return "Leaves a Migration Marker on Browserless test classes the migration skips because they pass `this` to "
                + "a page object or helper.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<>() {
            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                J.ClassDeclaration c = super.visitClassDeclaration(classDecl, ctx);
                String marker = !Browserless.isBrowserlessClass(classDecl) ? null
                        : Browserless.passesItself(classDecl) ? MARKER
                        : Browserless.isContextClass(classDecl) && !BrowserlessContexts.migratable(classDecl) ? CONTEXT_MARKER
                        : null;
                if (marker == null || c.getComments().stream().anyMatch(comment -> comment instanceof TextComment t
                        && t.getText().contains(marker))) {
                    return c;
                }
                List<Comment> comments = new ArrayList<>(c.getPrefix().getComments());
                comments.add(new TextComment(false, " TODO unified: " + marker, "\n" + c.getPrefix().getIndent(),
                        Markers.EMPTY));
                return c.withPrefix(Space.build(c.getPrefix().getWhitespace(), comments));
            }
        };
    }
}
