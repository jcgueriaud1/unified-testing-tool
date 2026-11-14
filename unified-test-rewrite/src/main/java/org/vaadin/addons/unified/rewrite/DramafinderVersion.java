package org.vaadin.addons.unified.rewrite;

import java.util.Set;
import org.openrewrite.ExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.marker.JavaSourceSet;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.maven.tree.Scope;

/**
 * Finds which Dramafinder the project uses (ADR 0006): the version its Maven build resolves, else the API its
 * classpath has, else the API of the base class its tests were attributed against. Dramafinder 1.x and 2.x share their class and method names but not what a lookup matches, so the
 * lookup rules need to know. A version that can't be found counts as 1.x: a wrong 1.x guess only adds markers.
 * A scanning recipe collects what {@link #scanner(Set)} finds and asks {@link #isVersion2(Set)}.
 */
final class DramafinderVersion {

    private static final String QUERIES = "org.vaadin.addons.dramafinder.element.query.ElementQueries";

    private DramafinderVersion() {
    }

    /** Adds what each source file says about the Dramafinder version to {@code found}. */
    static TreeVisitor<?, ExecutionContext> scanner(Set<String> found) {
        return new TreeVisitor<>() {
            @Override
            public Tree visit(Tree tree, ExecutionContext ctx) {
                if (!(tree instanceof SourceFile source)) {
                    return tree;
                }
                source.getMarkers().findFirst(MavenResolutionResult.class).ifPresent(maven -> {
                    boolean declared = maven.getPom().getRequestedDependencies().stream()
                            .anyMatch(d -> "dramafinder".equals(d.getArtifactId()) && "org.vaadin.addons".equals(d.getGroupId()));
                    boolean resolved = false;
                    for (Scope scope : Scope.values()) {
                        for (ResolvedDependency d : maven.getDependencies().getOrDefault(scope, java.util.List.of())) {
                            if (d.getArtifactId().equals("dramafinder") && d.getGroupId().equals("org.vaadin.addons")) {
                                found.add("build:" + d.getVersion());
                                resolved = true;
                            }
                        }
                    }
                    if (declared && !resolved) {
                        found.add("build:unresolved");
                    }
                });
                source.getMarkers().findFirst(JavaSourceSet.class).ifPresent(sourceSet -> {
                    boolean dramafinder = false;
                    boolean queries = false;
                    for (JavaType.FullyQualified type : sourceSet.getClasspath()) {
                        dramafinder |= type.getFullyQualifiedName().equals(Dramafinder.BASE);
                        queries |= type.getFullyQualifiedName().equals(QUERIES);
                    }
                    if (dramafinder) {
                        found.add(queries ? "classpath:2" : "classpath:1");
                    }
                });
                if (source instanceof org.openrewrite.java.tree.J.CompilationUnit cu) {
                    // the types the parser attributed: 2.x's base class has navigate(Class), 1.x's doesn't
                    for (JavaType type : cu.getTypesInUse().getTypesInUse()) {
                        for (JavaType.FullyQualified t = org.openrewrite.java.tree.TypeUtils.asFullyQualified(type);
                                t != null; t = t.getSupertype()) {
                            if (t.getFullyQualifiedName().equals(Dramafinder.BASE) && !t.getMethods().isEmpty()) {
                                found.add(t.getMethods().stream().anyMatch(m -> m.getName().equals("navigate"))
                                        ? "types:2" : "types:1");
                            }
                        }
                    }
                }
                return tree;
            }
        };
    }

    /** Whether the project uses Dramafinder 2.x; false when it uses 1.x or the version can't be found. */
    static boolean isVersion2(Set<String> found) {
        if (found.isEmpty()) {
            return false;
        }
        Set<String> build = found.stream().filter(f -> f.startsWith("build:")).collect(java.util.stream.Collectors.toSet());
        if (!build.isEmpty()) {
            return build.stream().allMatch(f -> f.matches("build:([2-9]|\\d{2,})\\..*"));
        }
        if (found.stream().anyMatch(f -> f.startsWith("classpath:"))) {
            return found.contains("classpath:2") && !found.contains("classpath:1");
        }
        return found.contains("types:2") && !found.contains("types:1");
    }
}
