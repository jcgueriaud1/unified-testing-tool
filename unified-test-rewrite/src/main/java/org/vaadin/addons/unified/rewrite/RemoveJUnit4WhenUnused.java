package org.vaadin.addons.unified.rewrite;

import java.util.concurrent.atomic.AtomicBoolean;
import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.maven.RemoveDependency;
import org.openrewrite.xml.tree.Xml;

/**
 * Once the TestBench tests are migrated to JUnit 6, JUnit 4 and the vintage engine that runs it stay in the build
 * only while another test still uses JUnit 4: removes {@code junit:junit} and {@code junit-vintage-engine} when no
 * JUnit 4 test is left.
 */
public class RemoveJUnit4WhenUnused extends ScanningRecipe<RemoveJUnit4WhenUnused.Seen> {

    /** Whether the project has TestBench tests, and whether anything else uses JUnit 4. */
    public static final class Seen {
        final AtomicBoolean testBenchTests = new AtomicBoolean();
        final AtomicBoolean otherJUnit4 = new AtomicBoolean();
    }

    @Override
    public String getDisplayName() {
        return "Remove JUnit 4 and the vintage engine when no JUnit 4 test is left";
    }

    @Override
    public String getDescription() {
        return "Removes `junit:junit` and `junit-vintage-engine` once the migrated TestBench tests were the only JUnit 4 tests.";
    }

    @Override
    public Seen getInitialValue(ExecutionContext ctx) {
        return new Seen();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Seen seen) {
        return new TreeVisitor<>() {
            @Override
            public Tree visit(Tree tree, ExecutionContext ctx) {
                if (!(tree instanceof JavaSourceFile cu)) {
                    return tree;
                }
                AtomicBoolean testBench = new AtomicBoolean();
                new JavaIsoVisitor<Integer>() {
                    @Override
                    public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, Integer p) {
                        if (TestBenchSetupToUnified.isTestBenchClass(classDecl)) {
                            testBench.set(true);
                        }
                        return super.visitClassDeclaration(classDecl, p);
                    }
                }.visit(cu, 0);
                if (testBench.get()) {
                    seen.testBenchTests.set(true);
                } else if (cu.getTypesInUse().getTypesInUse().stream().anyMatch(RemoveJUnit4WhenUnused::isJUnit4)) {
                    seen.otherJUnit4.set(true);
                }
                return tree;
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Seen seen) {
        return new TreeVisitor<>() {
            @Override
            public Tree visit(Tree tree, ExecutionContext ctx) {
                if (!(tree instanceof Xml.Document) || !seen.testBenchTests.get() || seen.otherJUnit4.get()) {
                    return tree;
                }
                Tree t = new RemoveDependency("junit", "junit", null).getVisitor().visit(tree, ctx);
                return new RemoveDependency("org.junit.vintage", "junit-vintage-engine", null).getVisitor().visit(t, ctx);
            }
        };
    }

    private static boolean isJUnit4(JavaType type) {
        return type instanceof JavaType.FullyQualified fq && fq.getFullyQualifiedName().startsWith("org.junit.")
                && !fq.getFullyQualifiedName().startsWith("org.junit.jupiter.")
                && !fq.getFullyQualifiedName().startsWith("org.junit.platform.");
    }
}
