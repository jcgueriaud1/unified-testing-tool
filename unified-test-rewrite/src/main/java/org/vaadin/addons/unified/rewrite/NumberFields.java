package org.vaadin.addons.unified.rewrite;

import org.openrewrite.Cursor;
import org.openrewrite.Tree;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.marker.Markers;

/**
 * TestBench's NumberField value is a String, the Unified one a Double: {@code setValue("42")} and
 * {@code assertEquals("42", field.getValue())} convert, with {@code 42.0}; any other use of the value doesn't.
 */
final class NumberFields {

    private static final MethodMatcher ASSERT_EQUALS =
            new MethodMatcher("org.junit.jupiter.api.Assertions assertEquals(Object, Object)");

    private NumberFields() {
    }

    static boolean converts(J.MethodInvocation call, Cursor callCursor) {
        if (call.getSimpleName().equals("setValue") && call.getArguments().size() == 1) {
            Expression value = call.getArguments().get(0);
            // a numeric string, or the double a recipe already made of it
            return number(value) != null || value instanceof J.Literal literal && literal.getValue() instanceof Double;
        }
        if (call.getSimpleName().equals("getValue")) {
            Object parent = callCursor.getParentTreeCursor().getValue();
            return parent instanceof J.MethodInvocation assertion && ASSERT_EQUALS.matches(assertion)
                    && assertion.getArguments().get(1) == call && number(assertion.getArguments().get(0)) != null;
        }
        return false;
    }

    /** The number a numeric string literal holds, else {@code null}. */
    static Double number(Expression e) {
        if (e instanceof J.Literal literal && literal.getValue() instanceof String text) {
            try {
                return Double.valueOf(text);
            } catch (NumberFormatException notANumber) {
                return null;
            }
        }
        return null;
    }

    /** The {@code double} literal for a numeric string literal. */
    static J.Literal doubleLiteral(Expression numericString) {
        double value = number(numericString);
        return new J.Literal(Tree.randomId(), numericString.getPrefix(), Markers.EMPTY, value, Double.toString(value),
                null, JavaType.Primitive.Double);
    }
}
