package org.vaadin.addons.unified.rewrite;

import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

/**
 * Reads the application path out of a URL a TestBench test opens, such as
 * {@code "http://localhost:" + port + "/people"} or {@code "http://localhost:8080/people"}.
 */
final class Urls {

    private Urls() {
    }

    /**
     * The path expression in {@code base + "/" + path}, for a path only known at run time, else {@code null}.
     */
    static Expression pathExpression(Expression url) {
        if (url instanceof J.Binary b && b.getOperator() == J.Binary.Type.Addition
                && !(b.getRight() instanceof J.Literal)
                && b.getLeft() instanceof J.Binary left && left.getRight() instanceof J.Literal slash
                && "/".equals(slash.getValue())) {
            return b.getRight().withPrefix(org.openrewrite.java.tree.Space.EMPTY);
        }
        return null;
    }

    /** The path after the host and port, without its leading slash, or {@code null} when it can't be read. */
    static String pathOf(Expression url) {
        if (url instanceof J.Binary b && b.getOperator() == J.Binary.Type.Addition
                && b.getRight() instanceof J.Literal right && right.getValue() instanceof String tail
                && tail.startsWith("/")) {
            return tail.substring(1);
        }
        if (url instanceof J.Literal literal && literal.getValue() instanceof String full) {
            int scheme = full.indexOf("://");
            if (scheme < 0) {
                return null;
            }
            int slash = full.indexOf('/', scheme + 3);
            return slash < 0 ? "" : full.substring(slash + 1);
        }
        return null;
    }
}
