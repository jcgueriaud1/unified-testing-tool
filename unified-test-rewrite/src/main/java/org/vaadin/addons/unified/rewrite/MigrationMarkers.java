package org.vaadin.addons.unified.rewrite;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.openrewrite.Cursor;
import org.openrewrite.java.tree.Comment;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.java.tree.TextComment;
import org.openrewrite.marker.Markers;

/**
 * Migration Markers: {@code // TODO unified: ...} comments above the statements of a test method that a recipe
 * changed in a way a reviewer must check, or couldn't convert. Collected while visiting the method, written after.
 */
final class MigrationMarkers {

    private static final String KEY = "unified.migration-markers";

    private MigrationMarkers() {
    }

    /** Starts collecting markers for the method at the cursor. */
    static Map<UUID, List<String>> collect(Cursor methodCursor) {
        Map<UUID, List<String>> markers = new LinkedHashMap<>();
        methodCursor.putMessage(KEY, markers);
        return markers;
    }

    /** Adds a marker above the method-body statement the cursor is in; false outside a collecting method body. */
    static boolean add(Cursor cursor, String text) {
        Map<UUID, List<String>> markers = cursor.getNearestMessage(KEY);
        Cursor statement = statementInMethodBody(cursor);
        if (markers == null || statement == null) {
            return false;
        }
        List<String> texts = markers.computeIfAbsent(((Statement) statement.getValue()).getId(), id -> new ArrayList<>());
        if (!texts.contains(text)) {
            texts.add(text);
        }
        return true;
    }

    /** The method with the collected markers written above their statements. */
    static J.MethodDeclaration write(J.MethodDeclaration method, Map<UUID, List<String>> markers) {
        if (markers.isEmpty() || method.getBody() == null) {
            return method;
        }
        List<Statement> statements = new ArrayList<>();
        for (Statement s : method.getBody().getStatements()) {
            List<String> texts = markers.get(s.getId());
            if (texts != null) {
                String indent = s.getPrefix().getIndent();
                List<Comment> comments = new ArrayList<>(s.getPrefix().getComments());
                for (String text : texts) {
                    comments.add(new TextComment(false, " TODO unified: " + text, "\n" + indent, Markers.EMPTY));
                }
                s = s.withPrefix(s.getPrefix().withComments(comments));
            }
            statements.add(s);
        }
        return method.withBody(method.getBody().withStatements(statements));
    }

    private static Cursor statementInMethodBody(Cursor cursor) {
        Cursor c = cursor;
        while (c.getParent() != null) {
            Cursor parent = c.getParentTreeCursor();
            if (parent.getValue() instanceof J.Block && parent.getParentTreeCursor().getValue() instanceof J.MethodDeclaration) {
                return c;
            }
            c = parent;
        }
        return null;
    }
}
