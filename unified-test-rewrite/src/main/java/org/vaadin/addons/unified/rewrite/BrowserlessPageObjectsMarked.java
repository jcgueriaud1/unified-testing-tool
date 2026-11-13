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
 * A Browserless test class that hands itself ({@code this}) to a page object or helper is not migrated: without
 * its test base class it would not compile. It gets a Migration Marker saying so, above the class.
 */
public class BrowserlessPageObjectsMarked extends Recipe {

    static final String MARKER = "not migrated: this class passes itself (this) to a page object or helper; "
            + "make them take a UnifiedPage, then run the migration again";

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
                if (!Browserless.isBrowserlessClass(classDecl) || !Browserless.passesItself(classDecl)
                        || c.getComments().stream().anyMatch(comment -> comment instanceof TextComment t
                                && t.getText().contains(MARKER))) {
                    return c;
                }
                List<Comment> comments = new ArrayList<>(c.getPrefix().getComments());
                comments.add(new TextComment(false, " TODO unified: " + MARKER, "\n" + c.getPrefix().getIndent(),
                        Markers.EMPTY));
                return c.withPrefix(Space.build(c.getPrefix().getWhitespace(), comments));
            }
        };
    }
}
