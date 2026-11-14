package org.vaadin.addons.unified.rewrite;

import java.util.HashMap;
import java.util.Map;
import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

/**
 * Deletes a project's copy of Dramafinder's {@code SpringPlaywrightIT} (Dramafinder 1.x projects copied it from a
 * template) once the migration leaves nothing extending it: every test that extended it was migrated (ADR 0006).
 * A copy one test still extends, because it uses a member no rule converts, stays.
 */
public class DramafinderCopiedBaseRemoved extends ScanningRecipe<Map<String, Boolean>> {

    @Override
    public String getDisplayName() {
        return "Delete a copied SpringPlaywrightIT nothing extends any more";
    }

    @Override
    public String getDescription() {
        return "Deletes a project's copy of Dramafinder's `SpringPlaywrightIT` when every test extending it was migrated.";
    }

    /** For each copy's name: whether every test extending it migrates. */
    @Override
    public Map<String, Boolean> getInitialValue(ExecutionContext ctx) {
        return new HashMap<>();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Map<String, Boolean> copies) {
        return new JavaIsoVisitor<>() {
            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                JavaType.FullyQualified parent = classDecl.getExtends() == null ? null
                        : TypeUtils.asFullyQualified(classDecl.getExtends().getType());
                if (parent != null && Dramafinder.isCopiedSpringBase(parent)) {
                    boolean migrates = Dramafinder.isDramafinderClass(classDecl) && Dramafinder.migratable(classDecl);
                    copies.merge(parent.getFullyQualifiedName(), migrates, Boolean::logicalAnd);
                }
                return super.visitClassDeclaration(classDecl, ctx);
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Map<String, Boolean> copies) {
        return new JavaIsoVisitor<>() {
            @Override
            public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
                if (cu.getClasses().size() == 1 && cu.getClasses().get(0).getType() != null) {
                    JavaType.FullyQualified type = cu.getClasses().get(0).getType();
                    if (Dramafinder.isCopiedSpringBase(type) && Boolean.TRUE.equals(copies.get(type.getFullyQualifiedName()))) {
                        //noinspection DataFlowIssue: a null source file is deleted
                        return null;
                    }
                }
                return cu;
            }
        };
    }
}
