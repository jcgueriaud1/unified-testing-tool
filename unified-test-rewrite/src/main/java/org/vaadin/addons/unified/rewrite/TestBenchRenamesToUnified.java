package org.vaadin.addons.unified.rewrite;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

/**
 * Renames the TestBench element calls a Wrapper has under another name ({@code selectByText} →
 * {@code selectItem}, {@code getDate} → {@code getValue}, ...), on elements whose lookup converts.
 */
public class TestBenchRenamesToUnified extends Recipe {

    @Override
    public String getDisplayName() {
        return "Rename TestBench element calls to their Unified names";
    }

    @Override
    public String getDescription() {
        return "Renames TestBench element calls a Wrapper has under another name, on elements whose lookup converts.";
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
                String filter = Wrappers.BUTTON_FILTER_RENAMES.get(m.getSimpleName());
                if (filter != null && m.getArguments().size() == 1 && onButtonLookupThatConverts()
                        && MigrationMarkers.add(getCursor(), "was " + method.printTrimmed(getCursor()).replaceFirst("^.*\\.(?=with)", "")
                                + "; use withAccessibleName(..) for a button without text")) {
                    return m.withName(m.getName().withSimpleName(filter));
                }
                if (method.getSelect() == null || getCursor().firstEnclosing(J.MethodDeclaration.class) == null
                        || !Pages.hasPageParameter(getCursor().firstEnclosing(J.MethodDeclaration.class))) {
                    return m;
                }
                Wrappers.Wrapper wrapper = wrapperOf(method.getSelect());
                if (wrapper != null && !wrapper.has(m.getSimpleName()) && m.getSimpleName().equals("setValue")
                        && NumberFields.number(m.getArguments().get(0)) != null) {
                    return m.withArguments(java.util.List.of(NumberFields.doubleLiteral(m.getArguments().get(0))));
                }
                String renamed = wrapper == null ? null : wrapper.renames().get(m.getSimpleName());
                return renamed == null ? m : m.withName(m.getName().withSimpleName(renamed));
            }

            /** Whether this filter call is in a {@code $(ButtonElement.class)} lookup the Button Wrapper can express. */
            private boolean onButtonLookupThatConverts() {
                org.openrewrite.Cursor c = getCursor();
                while (c.getParentTreeCursor().getValue() instanceof J.MethodInvocation parent && parent.getSelect() == c.getValue()
                        && !Wrappers.TERMINALS.contains(parent.getSimpleName())) {
                    c = c.getParentTreeCursor();
                }
                Object parent = c.getParentTreeCursor().getValue();
                if (!(parent instanceof J.MethodInvocation terminal) || terminal.getSelect() != c.getValue()) {
                    return false;
                }
                Wrappers.Wrapper wrapper = Wrappers.ofLookup(terminal);
                return wrapper != null && wrapper == Wrappers.forElement(Wrappers.BUTTON_ELEMENT);
            }

            private Wrappers.Wrapper wrapperOf(Expression element) {
                return element instanceof J.Identifier local
                        ? Locals.wrapperOf(getCursor(), local)
                        : Wrappers.ofLookup(element);
            }
        };
    }
}
