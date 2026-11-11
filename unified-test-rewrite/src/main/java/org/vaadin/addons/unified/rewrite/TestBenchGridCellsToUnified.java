package org.vaadin.addons.unified.rewrite;

import static java.util.Collections.emptyList;

import java.util.HashSet;
import java.util.Set;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.marker.Markers;

/**
 * Rewrites TestBench's {@code GridElement.getCell(..)} on a Grid that becomes a {@code UnifiedGrid}:
 * {@code getCell(row, column).getText()} → {@code row(row).getCellText(column)}, and a cell found by text,
 * used for its row, → {@code rows().withCellText(text).single()} (a {@code UnifiedGridRow}, with a Migration
 * Marker: it was the first match, it is now the only one).
 */
public class TestBenchGridCellsToUnified extends Recipe {

    private static final String UNIFIED_GRID = "com.vaadin.testing.unified.component.UnifiedGrid";
    private static final String CELL_ROWS = "unified.cell-rows";

    @Override
    public String getDisplayName() {
        return "Migrate TestBench Grid cells to Unified Grid rows";
    }

    @Override
    public String getDescription() {
        return "Rewrites `getCell(row, column).getText()` and cells found with `getCell(text)` on a Grid that becomes "
                + "a `UnifiedGrid`.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<>() {

            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                if (!Pages.hasPageParameter(method)) {
                    return method;
                }
                var markers = MigrationMarkers.collect(getCursor());
                getCursor().putMessage(CELL_ROWS, new HashSet<String>());
                return MigrationMarkers.write(super.visitMethodDeclaration(method, ctx), markers);
            }

            @Override
            public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                J.VariableDeclarations v = super.visitVariableDeclarations(multiVariable, ctx);
                Set<String> cellRows = getCursor().getNearestMessage(CELL_ROWS);
                if (cellRows != null && v.getVariables().size() == 1 && v.getTypeExpression() != null
                        && v.getVariables().get(0).getInitializer() instanceof J.MethodInvocation init
                        && init.getSimpleName().equals("single") && init.getSelect() instanceof J.MethodInvocation withText
                        && withText.getSimpleName().equals("withCellText")
                        && cellRows.contains(v.getVariables().get(0).getSimpleName())) {
                    maybeAddImport(UNIFIED_GRID);
                    return v.withTypeExpression(new J.Identifier(Tree.randomId(), v.getTypeExpression().getPrefix(),
                            Markers.EMPTY, emptyList(), "UnifiedGrid.UnifiedGridRow",
                            JavaType.ShallowClass.build(GridCells.ROW), null));
                }
                return v;
            }

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                Set<String> cellRows = getCursor().getNearestMessage(CELL_ROWS);
                if (cellRows == null) {
                    return m;
                }
                // cell.getRow() on a local now holding the row
                if (m.getSelect() instanceof J.Identifier local && cellRows.contains(local.getSimpleName())
                        && GridCells.ROW_METHODS.containsKey(m.getSimpleName())) {
                    return m.withName(m.getName().withSimpleName(GridCells.ROW_METHODS.get(m.getSimpleName())));
                }
                // getCell(row, column).getText()
                if (m.getSimpleName().equals("getText") && method.getSelect() instanceof J.MethodInvocation getCell
                        && GridCells.isGetCell(getCell) && GridCells.byIndex(getCell) && gridConverts(getCell.getSelect())) {
                    return template("#{any(" + UNIFIED_GRID + ")}.row(#{any(int)}).getCellText(#{any(int)})",
                            getCell.getSelect(), getCell.getArguments().get(0), getCell.getArguments().get(1));
                }
                // getCell(text), used for its row
                if (GridCells.isGetCell(method) && !GridCells.byIndex(method) && gridConverts(method.getSelect())
                        && GridCells.converts(method, getCursor())) {
                    Object parent = getCursor().getParentTreeCursor().getValue();
                    if (parent instanceof J.VariableDeclarations.NamedVariable variable) {
                        cellRows.add(variable.getSimpleName());
                    }
                    MigrationMarkers.add(getCursor(), "was getCell(" + method.getArguments().get(0).printTrimmed(getCursor())
                            + "); single() fails when several rows match");
                    return template("#{any(" + UNIFIED_GRID + ")}.rows().withCellText(#{any(String)}).single()",
                            m.getSelect(), m.getArguments().get(0));
                }
                // getCell(text).getRow() / .click() inline
                if (method.getSelect() instanceof J.MethodInvocation cell && GridCells.isGetCell(cell)
                        && !GridCells.byIndex(cell) && GridCells.ROW_METHODS.containsKey(m.getSimpleName())) {
                    return m.withName(m.getName().withSimpleName(GridCells.ROW_METHODS.get(m.getSimpleName())));
                }
                return m;
            }

            private boolean gridConverts(Expression grid) {
                return grid instanceof J.Identifier local
                        ? Locals.wrapperOf(getCursor(), local) != null
                        : Wrappers.ofLookup(grid) != null;
            }

            private J.MethodInvocation template(String code, Object... parameters) {
                return JavaTemplate.builder(code)
                        .javaParser(Stubs.parser())
                        .build()
                        .apply(getCursor(), ((J.MethodInvocation) getCursor().getValue()).getCoordinates().replace(), parameters);
            }
        };
    }
}
