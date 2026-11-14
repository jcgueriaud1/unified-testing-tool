package org.vaadin.addons.unified.rewrite;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import org.openrewrite.Cursor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.java.tree.TypeUtils;

/**
 * Dramafinder's {@code GridElement} rows and cells in Unified terms (ADR 0006): {@code grid.findRow(index)} is
 * {@code grid.row(index)}, {@code findRowIndexesWithColumnText(col, text).get(0)} the index of
 * {@code grid.rows().withCellText(..).single()}, a row's {@code getCell(col).getText()} its
 * {@code getCellText(col)}, and grid waits Auto-Wait makes unnecessary go.
 */
final class DramafinderGrids {

    /** Grid calls dropped as statements: Auto-Wait waits for the grid's data and scrolls to a row it reads. */
    static final Set<String> DROPPED = Set.of("waitForGridToStopLoading", "scrollToRow");
    private static final Set<String> UNWRAP = Set.of("orElseThrow", "get");

    private DramafinderGrids() {
    }

    /** {@code grid.findRowIndexesWithColumnText(col, text).get(0)} on a grid local. */
    static boolean isRowIndexLookup(Expression e, Predicate<String> gridLocal) {
        return e instanceof J.MethodInvocation get && get.getSimpleName().equals("get") && get.getArguments().size() == 1
                && get.getArguments().get(0) instanceof J.Literal zero && Integer.valueOf(0).equals(zero.getValue())
                && get.getSelect() instanceof J.MethodInvocation indexes
                && indexes.getSimpleName().equals("findRowIndexesWithColumnText") && indexes.getArguments().size() == 2
                && indexes.getSelect() instanceof J.Identifier grid && gridLocal.test(grid.getSimpleName());
    }

    /** {@code grid.findRow(index).orElseThrow()} (or {@code .get()}) on a grid local. */
    static boolean isFindRow(Expression e, Predicate<String> gridLocal) {
        return e instanceof J.MethodInvocation unwrap && UNWRAP.contains(unwrap.getSimpleName()) && noArguments(unwrap)
                && unwrap.getSelect() instanceof J.MethodInvocation findRow && findRow.getSimpleName().equals("findRow")
                && findRow.getArguments().size() == 1 && findRow.getSelect() instanceof J.Identifier grid
                && gridLocal.test(grid.getSimpleName());
    }

    /** A row local's {@code getCell(col)}, which only some cell calls convert. */
    static boolean isGetCell(Expression e, Predicate<String> rowLocal) {
        return e instanceof J.MethodInvocation cell && cell.getSimpleName().equals("getCell") && cell.getArguments().size() == 1
                && cell.getSelect() instanceof J.Identifier row && rowLocal.test(row.getSimpleName());
    }

    /** A cell call that converts: {@code click()}, {@code getText()}, or {@code assertText(s)} on a header. */
    static boolean cellCallConverts(J.MethodInvocation call, boolean statement) {
        J.MethodInvocation cell = (J.MethodInvocation) call.getSelect();
        return switch (call.getSimpleName()) {
            case "click", "getText" -> noArguments(call);
            case "assertText" -> statement && TypeUtils.isString(cell.getArguments().get(0).getType());
            default -> false;
        };
    }

    /** Whether every use of the grid local converts. */
    static boolean gridUsesConvert(J.MethodDeclaration method, String name) {
        return usesConvert(method, name, (call, cursor) -> {
            Object outer = cursor.getParentTreeCursor().getValue();
            boolean statement = outer instanceof J.Block;
            return switch (call.getSimpleName()) {
                case "getTotalRowCount" -> true;
                case "assertRowCount" -> statement;
                case "waitForGridToStopLoading", "scrollToRow" -> statement;
                case "findRowIndexesWithColumnText" -> isRowIndexLookup((Expression) outer, n -> n.equals(name));
                case "findRow" -> isFindRow((Expression) outer, n -> n.equals(name))
                        && cursor.getParentTreeCursor().getParentTreeCursor().getValue()
                                instanceof J.VariableDeclarations.NamedVariable row
                        && rowUsesConvert(method, row.getSimpleName());
                default -> false;
            };
        });
    }

    /** Whether every use of the row local converts. */
    static boolean rowUsesConvert(J.MethodDeclaration method, String name) {
        return usesConvert(method, name, (call, cursor) -> {
            Object outer = cursor.getParentTreeCursor().getValue();
            if (call.getSimpleName().equals("getCell")) {
                return outer instanceof J.MethodInvocation cellCall && cellCall.getSelect() == call
                        && cellCallConverts(cellCall, cursor.getParentTreeCursor().getParentTreeCursor().getValue() instanceof J.Block);
            }
            return DramafinderWrappers.ROW.methods().containsKey(call.getSimpleName())
                    || outer instanceof J.Block && DramafinderWrappers.ROW.assertions().containsKey(call.getSimpleName());
        });
    }

    private interface Use {
        boolean converts(J.MethodInvocation call, Cursor callCursor);
    }

    private static boolean usesConvert(J.MethodDeclaration method, String name, Use use) {
        if (method == null || method.getBody() == null) {
            return false;
        }
        AtomicBoolean other = new AtomicBoolean();
        new JavaIsoVisitor<AtomicBoolean>() {
            @Override
            public J.Identifier visitIdentifier(J.Identifier identifier, AtomicBoolean found) {
                Cursor parent = getCursor().getParentTreeCursor();
                if (identifier.getSimpleName().equals(name)
                        && !(parent.getValue() instanceof J.VariableDeclarations.NamedVariable)
                        && !(parent.getValue() instanceof J.MethodInvocation call && call.getSelect() == identifier
                                && use.converts(call, parent))) {
                    found.set(true);
                }
                return identifier;
            }
        }.visit(method.getBody(), other, new Cursor(null, method));
        return !other.get();
    }

    /** {@code grid.waitForGridToStopLoading();} or {@code grid.scrollToRow(i);} on a converted grid local. */
    static boolean isDropped(Statement s, Predicate<String> gridLocal) {
        return s instanceof J.MethodInvocation call && DROPPED.contains(call.getSimpleName())
                && call.getSelect() instanceof J.Identifier grid && gridLocal.test(grid.getSimpleName());
    }

    private static boolean noArguments(J.MethodInvocation call) {
        return call.getArguments().stream().allMatch(a -> a instanceof J.Empty);
    }
}
