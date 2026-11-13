package org.vaadin.addons.unified.rewrite;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.maven.search.DependencyInsight;

/**
 * Finds Maven projects that depend directly on Browserless Test ({@code browserless-test*}) or on TestBench UI Unit
 * ({@code vaadin-testbench-unit*}). The precondition of the Browserless build recipe.
 */
public class FindBrowserlessDependencies extends Recipe {

    @Override
    public String getDisplayName() {
        return "Find Browserless Test dependencies";
    }

    @Override
    public String getDescription() {
        return "Finds Maven projects with a direct `browserless-test*` or `vaadin-testbench-unit*` dependency.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.or(
                new DependencyInsight("com.vaadin", "browserless-test*", null, null, true).getVisitor(),
                new DependencyInsight("com.vaadin", "vaadin-testbench-unit*", null, null, true).getVisitor());
    }
}
