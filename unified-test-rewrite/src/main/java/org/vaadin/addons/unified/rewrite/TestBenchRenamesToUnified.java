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
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
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

            private Wrappers.Wrapper wrapperOf(Expression element) {
                return element instanceof J.Identifier local
                        ? Locals.wrapperOf(getCursor(), local)
                        : Wrappers.ofLookup(element);
            }
        };
    }
}
