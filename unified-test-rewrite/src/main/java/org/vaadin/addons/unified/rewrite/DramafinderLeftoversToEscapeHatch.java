package org.vaadin.addons.unified.rewrite;

import static java.util.Collections.emptyList;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.Markers;

/**
 * Keeps the Dramafinder and Playwright code no rule converted compiling and running on Playwright (ADR 0006): the
 * base class's {@code page} becomes {@code page.unwrap(Page.class)}, the Escape Hatch, each statement calling
 * Dramafinder or Playwright gets a Migration Marker, and the test method gets
 * {@code @RequiresTestingTool(TestingTool.PLAYWRIGHT)}.
 */
public class DramafinderLeftoversToEscapeHatch extends Recipe {

    private static final String LEFTOVERS = "unified.dramafinder.leftovers";
    private static final String STUBS = "package com.microsoft.playwright; public interface Page {}";

    @Override
    public String getDisplayName() {
        return "Keep unconverted Dramafinder calls running through the Escape Hatch";
    }

    @Override
    public String getDescription() {
        return "Hands the Playwright `Page` from `page.unwrap(Page.class)` to the Dramafinder and Playwright calls no "
                + "rule converted, marks each with a Migration Marker and makes the test method require Playwright.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new org.openrewrite.java.JavaVisitor<>() {

            @Override
            public J visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                if (method.getBody() == null || !Pages.hasPageParameter(method)) {
                    return method;
                }
                var markers = MigrationMarkers.collect(getCursor());
                AtomicBoolean unwraps = new AtomicBoolean();
                getCursor().putMessage(LEFTOVERS, unwraps);
                J.MethodDeclaration m = (J.MethodDeclaration) super.visitMethodDeclaration(method, ctx);
                if (unwraps.get()) {
                    maybeAddImport(Dramafinder.PAGE);
                    m = withFirst(m, ctx, "Page " + Dramafinder.VARIABLE + " = page.unwrap(Page.class);");
                }
                boolean escapes = !markers.isEmpty();
                m = MigrationMarkers.write(m, markers);
                return Helpers.isBrowserTest(m) && (escapes || callsAnEscapingHelper(m)) ? requiresPlaywright(m) : m;
            }

            @Override
            public J visitIdentifier(J.Identifier identifier, ExecutionContext ctx) {
                AtomicBoolean unwraps = getCursor().getNearestMessage(LEFTOVERS);
                if (unwraps == null || !Dramafinder.isPageField(identifier)
                        || getCursor().getParentTreeCursor().getValue() instanceof J.FieldAccess) {
                    return identifier;
                }
                unwraps.set(true);
                return playwright(identifier.getPrefix());
            }

            @Override
            public J visitFieldAccess(J.FieldAccess fieldAccess, ExecutionContext ctx) {
                AtomicBoolean unwraps = getCursor().getNearestMessage(LEFTOVERS);
                if (unwraps != null && Dramafinder.isPageField(fieldAccess)) {
                    unwraps.set(true);
                    return playwright(fieldAccess.getPrefix()); // this.page
                }
                return super.visitFieldAccess(fieldAccess, ctx);
            }

            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                AtomicBoolean unwraps = getCursor().getNearestMessage(LEFTOVERS);
                if (unwraps != null && Dramafinder.isGetPage(method)) {
                    unwraps.set(true);
                    return playwright(method.getPrefix());
                }
                J m = super.visitMethodInvocation(method, ctx);
                if (unwraps != null && Dramafinder.isToolCall(method) && !insideAnotherToolCall()) {
                    MigrationMarkers.add(getCursor(), "Dramafinder Escape Hatch: " + m.printTrimmed(getCursor().getParentTreeCursor()).strip());
                }
                return m;
            }

            @Override
            public J visitNewClass(J.NewClass newClass, ExecutionContext ctx) {
                J n = super.visitNewClass(newClass, ctx);
                AtomicBoolean unwraps = getCursor().getNearestMessage(LEFTOVERS);
                if (unwraps != null && (Dramafinder.isToolCall(newClass)
                        || newClass.getArguments().stream().anyMatch(Dramafinder::isPageField)) && !insideAnotherToolCall()) {
                    MigrationMarkers.add(getCursor(), "Dramafinder Escape Hatch: " + n.printTrimmed(getCursor().getParentTreeCursor()).strip());
                }
                return n;
            }

            /** Whether an enclosing call of the same statement calls Dramafinder too: it carries the statement's marker. */
            private boolean insideAnotherToolCall() {
                for (Cursor c = getCursor().getParentTreeCursor(); c.getValue() instanceof J; c = c.getParentTreeCursor()) {
                    if (c.getValue() instanceof J j && Dramafinder.isToolCall(j)) {
                        return true;
                    }
                    if (c.getValue() instanceof J.Block || c.getValue() instanceof J.MethodDeclaration) {
                        return false;
                    }
                }
                return false;
            }

            /** A test that calls a helper going through the Escape Hatch runs on Playwright only, too. */
            private boolean callsAnEscapingHelper(J.MethodDeclaration m) {
                J.ClassDeclaration c = getCursor().firstEnclosing(J.ClassDeclaration.class);
                if (c == null) {
                    return false;
                }
                Set<String> escaping = Helpers.using(c, Set.of(), call -> Dramafinder.isToolCall(call)
                        || Dramafinder.usesTestBase(call));
                AtomicBoolean calls = new AtomicBoolean();
                new JavaIsoVisitor<Integer>() {
                    @Override
                    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation call, Integer p) {
                        if (call.getSelect() == null && escaping.contains(call.getSimpleName())) {
                            calls.set(true);
                        }
                        return super.visitMethodInvocation(call, p);
                    }
                }.visit(m.getBody(), 0);
                return calls.get();
            }

            private J.MethodDeclaration requiresPlaywright(J.MethodDeclaration m) {
                if (m.getLeadingAnnotations().stream().anyMatch(a -> TypeUtils.isOfClassType(a.getType(),
                        TestBenchLeftoversToEscapeHatch.REQUIRES_TESTING_TOOL))) {
                    return m;
                }
                maybeAddImport(TestBenchLeftoversToEscapeHatch.REQUIRES_TESTING_TOOL);
                maybeAddImport(TestBenchLeftoversToEscapeHatch.TESTING_TOOL);
                return JavaTemplate.builder("@RequiresTestingTool(TestingTool.PLAYWRIGHT)")
                        .imports(TestBenchLeftoversToEscapeHatch.REQUIRES_TESTING_TOOL, TestBenchLeftoversToEscapeHatch.TESTING_TOOL)
                        .javaParser(Stubs.parser())
                        .build()
                        .apply(updateCursor(m), m.getCoordinates().addAnnotation((a, b) -> 1));
            }

            /**
             * The method with the given declaration as its first statement. Parsed on its own and inserted, as a
             * template inserted before a statement this recipe chain already rewrote can be lost.
             */
            private J.MethodDeclaration withFirst(J.MethodDeclaration m, ExecutionContext ctx, String declarationSource) {
                String source = "import " + Dramafinder.PAGE + ";\nclass EscapeHatchStatement { void m("
                        + TestBenchSetupToUnified.UNIFIED_PAGE + " page) { " + declarationSource + " } }";
                J.CompilationUnit cu = (J.CompilationUnit) Stubs.parser(STUBS).build().parse(ctx, source)
                        .findFirst().orElseThrow();
                J.MethodDeclaration parsed = (J.MethodDeclaration) cu.getClasses().get(0).getBody().getStatements().get(0);
                Statement declaration = parsed.getBody().getStatements().get(0);
                List<Statement> statements = new ArrayList<>(m.getBody().getStatements());
                Statement first = statements.get(0);
                statements.add(0, declaration.withPrefix(first.getPrefix().withComments(emptyList())));
                statements.set(1, first.withPrefix(first.getPrefix().withWhitespace("\n" + first.getPrefix().getIndent())));
                return m.withBody(m.getBody().withStatements(statements));
            }
        };
    }

    /** The Escape Hatch's {@code playwright} local, in place of the base class's page. */
    private static J.Identifier playwright(Space prefix) {
        return new J.Identifier(Tree.randomId(), prefix, Markers.EMPTY, emptyList(), Dramafinder.VARIABLE,
                JavaType.ShallowClass.build(Dramafinder.PAGE), null);
    }
}
