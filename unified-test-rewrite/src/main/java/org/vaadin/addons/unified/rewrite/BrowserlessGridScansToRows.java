package org.vaadin.addons.unified.rewrite;

import java.util.Set;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeUtils;

/**
 * The row scan Browserless tests write for want of a row lookup by text, once its Grid calls are converted:
 * {@code IntStream.range(0, grid.getRowCount()).filter(i -> grid.row(i).getCellText(col).equals(text)).findFirst()
 * .orElseThrow()} becomes {@code grid.rows().withCellText(text).single().getIndex()}. {@code withCellText(text)}
 * matches any column, the scan one; the Unified API names a column by its header, which the recipe doesn't know,
 * so a Migration Marker says so.
 */
public class BrowserlessGridScansToRows extends Recipe {

    private static final String GRID = "com.vaadin.testing.unified.component.UnifiedGrid";
    private static final Set<String> TERMINALS = Set.of("orElseThrow", "getAsInt");

    @Override
    public String getDisplayName() {
        return "Migrate Grid row scans to a row lookup by text";
    }

    @Override
    public String getDescription() {
        return "Turns `IntStream.range(0, grid.getRowCount()).filter(i -> grid.row(i).getCellText(col).equals(text))"
                + ".findFirst()` into `grid.rows().withCellText(text).single().getIndex()`.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<>() {
            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                var markers = MigrationMarkers.collect(getCursor());
                return MigrationMarkers.write(super.visitMethodDeclaration(method, ctx), markers);
            }

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                Scan scan = scan(method);
                if (scan == null || !MigrationMarkers.add(getCursor(), "was a scan of column "
                        + scan.column().printTrimmed(getCursor()) + "; withCellText(text) matches any column: name the "
                        + "column, withCellText(header, text), if another column can hold the text")) {
                    return m;
                }
                maybeRemoveImport("java.util.stream.IntStream");
                return JavaTemplate.builder("#{any(" + GRID + ")}.rows().withCellText(#{any(java.lang.String)}).single().getIndex()")
                        .javaParser(Stubs.parser())
                        .build()
                        .apply(getCursor(), m.getCoordinates().replace(), scan.grid(), scan.text());
            }
        };
    }

    private record Scan(Expression grid, Expression column, Expression text) {
    }

    /**
     * The scan's parts when the call is
     * {@code IntStream.range(0, g.getRowCount()).filter(i -> g.row(i).getCellText(c).equals(t)).findFirst().orElseThrow()}.
     */
    private static Scan scan(J.MethodInvocation terminal) {
        if (!TERMINALS.contains(terminal.getSimpleName()) || !Browserless.noArguments(terminal)
                || !(terminal.getSelect() instanceof J.MethodInvocation findFirst)
                || !findFirst.getSimpleName().equals("findFirst")
                || !(findFirst.getSelect() instanceof J.MethodInvocation filter) || !filter.getSimpleName().equals("filter")
                || !(filter.getSelect() instanceof J.MethodInvocation range) || !range.getSimpleName().equals("range")
                || range.getArguments().size() != 2
                || !(range.getSelect() instanceof J.Identifier intStream) || !intStream.getSimpleName().equals("IntStream")
                || !(range.getArguments().get(0) instanceof J.Literal zero) || !Integer.valueOf(0).equals(zero.getValue())
                || !(range.getArguments().get(1) instanceof J.MethodInvocation count)
                || !count.getSimpleName().equals("getRowCount") || !(count.getSelect() instanceof J.Identifier grid)
                || !TypeUtils.isOfClassType(grid.getType(), GRID)
                || !(filter.getArguments().get(0) instanceof J.Lambda lambda)
                || lambda.getParameters().getParameters().size() != 1
                || !(lambda.getParameters().getParameters().get(0) instanceof J.VariableDeclarations index)
                || !(lambda.getBody() instanceof J.MethodInvocation equals) || !equals.getSimpleName().equals("equals")
                || equals.getArguments().size() != 1
                || !(equals.getSelect() instanceof J.MethodInvocation cellText)
                || !cellText.getSimpleName().equals("getCellText") || cellText.getArguments().size() != 1
                || !(cellText.getSelect() instanceof J.MethodInvocation row) || !row.getSimpleName().equals("row")
                || !(row.getSelect() instanceof J.Identifier rowGrid) || !rowGrid.getSimpleName().equals(grid.getSimpleName())
                || !(row.getArguments().get(0) instanceof J.Identifier i)
                || !i.getSimpleName().equals(index.getVariables().get(0).getSimpleName())
                || !TypeUtils.isString(equals.getArguments().get(0).getType())) {
            return null;
        }
        return new Scan(grid, cellText.getArguments().get(0), equals.getArguments().get(0));
    }
}
