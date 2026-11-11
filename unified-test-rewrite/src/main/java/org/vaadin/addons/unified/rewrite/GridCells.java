package org.vaadin.addons.unified.rewrite;

import java.util.concurrent.atomic.AtomicBoolean;
import org.openrewrite.Cursor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

/**
 * TestBench's {@code GridElement.getCell(..)} in Unified terms: {@code getCell(row, column).getText()} reads
 * {@code row(row).getCellText(column)}; {@code getCell(text)}, used for its row ({@code getRow()},
 * {@code click()}), finds {@code rows().withCellText(text).single()}.
 */
final class GridCells {

    static final String GRID_ELEMENT = "com.vaadin.flow.component.grid.testbench.GridElement";
    static final String ROW = "com.vaadin.testing.unified.component.UnifiedGrid.UnifiedGridRow";
    /** What a cell found by text is used for, and the row method that replaces it. */
    static final java.util.Map<String, String> ROW_METHODS = java.util.Map.of("getRow", "getIndex", "click", "click");

    private GridCells() {
    }

    /** Whether this {@code getCell(..)} call, at the cursor, converts. */
    static boolean converts(J.MethodInvocation call, Cursor callCursor) {
        if (!isGetCell(call)) {
            return false;
        }
        Object parent = callCursor.getParentTreeCursor().getValue();
        if (byIndex(call)) {
            return parent instanceof J.MethodInvocation next && next.getSelect() == call
                    && next.getSimpleName().equals("getText") && noArguments(next);
        }
        if (parent instanceof J.MethodInvocation next && next.getSelect() == call) {
            return ROW_METHODS.containsKey(next.getSimpleName()) && noArguments(next);
        }
        return parent instanceof J.VariableDeclarations.NamedVariable variable
                && usedOnlyForItsRow(callCursor.firstEnclosing(J.MethodDeclaration.class), variable.getSimpleName());
    }

    static boolean isGetCell(J.MethodInvocation call) {
        JavaType.Method type = call.getMethodType();
        return call.getSimpleName().equals("getCell") && type != null
                && TypeUtils.isOfClassType(type.getDeclaringType(), GRID_ELEMENT)
                && (byIndex(call) || call.getArguments().size() == 1);
    }

    static boolean byIndex(J.MethodInvocation call) {
        return call.getArguments().size() == 2;
    }

    /** Whether every use of the local holding a cell is one of {@link #ROW_METHODS}. */
    static boolean usedOnlyForItsRow(J.MethodDeclaration method, String name) {
        if (method == null || method.getBody() == null) {
            return false;
        }
        AtomicBoolean other = new AtomicBoolean();
        new JavaIsoVisitor<AtomicBoolean>() {
            @Override
            public J.Identifier visitIdentifier(J.Identifier identifier, AtomicBoolean found) {
                Object parent = getCursor().getParentTreeCursor().getValue();
                if (identifier.getSimpleName().equals(name)
                        && !(parent instanceof J.VariableDeclarations.NamedVariable)
                        && !(parent instanceof J.MethodInvocation call && call.getSelect() == identifier
                                && ROW_METHODS.containsKey(call.getSimpleName()) && noArguments(call))) {
                    found.set(true);
                }
                return identifier;
            }
        }.visit(method.getBody(), other);
        return !other.get();
    }

    private static boolean noArguments(J.MethodInvocation call) {
        return call.getArguments().stream().allMatch(a -> a instanceof J.Empty);
    }
}
