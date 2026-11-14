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
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.Markers;

/**
 * Keeps the Browserless test base calls no rule converted ({@code test(view.name)}, {@code $(Grid.class)},
 * {@code withCondition(..)}, ...) compiling and running on Browserless: they go through a
 * {@code BrowserlessEscapeHatch} on the test's page, each statement gets a Migration Marker saying why, and the
 * test method gets {@code @RequiresTestingTool(TestingTool.BROWSERLESS)}.
 */
public class BrowserlessLeftoversToEscapeHatch extends Recipe {

    private static final String LEFTOVERS = "unified.browserless.leftovers";
    /** Filters that run Java on each component: no locator says the same. */
    private static final Set<String> JAVA_FILTERS = Set.of("withCondition", "withPropertyValue");

    @Override
    public String getDisplayName() {
        return "Keep unconverted Browserless calls running through the Escape Hatch";
    }

    @Override
    public String getDescription() {
        return "Routes Browserless test base calls no rule converted through `BrowserlessEscapeHatch`, marks each with "
                + "a Migration Marker and makes the test method require Browserless.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<>() {

            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                if (method.getBody() == null || !Pages.hasPageParameter(method)) {
                    return method;
                }
                var markers = MigrationMarkers.collect(getCursor());
                AtomicBoolean escapes = new AtomicBoolean();
                getCursor().putMessage(LEFTOVERS, escapes);
                J.MethodDeclaration m = super.visitMethodDeclaration(method, ctx);
                // a context-API test's window or extension, whose field the setup removed
                List<String> handles = handlesUsed(m);
                if (!escapes.get() && handles.isEmpty()) {
                    return callsAnEscapingHelper(m) ? requiresBrowserless(m) : m;
                }
                if (usesEscapeHatch(m)) {
                    maybeAddImport(Browserless.ESCAPE_HATCH);
                    m = withFirst(m, ctx, "import " + Browserless.ESCAPE_HATCH + ";", "BrowserlessEscapeHatch "
                            + Browserless.VARIABLE + " = BrowserlessEscapeHatch.of(page);");
                }
                for (String handle : handles.reversed()) {
                    maybeAddImport(Browserless.UI_CONTEXT);
                    m = withFirst(m, ctx, "import " + Browserless.UI_CONTEXT + ";", "BrowserlessUIContext " + handle
                            + " = page.unwrap(BrowserlessUIContext.class);");
                }
                m = MigrationMarkers.write(m, markers);
                return Helpers.isBrowserTest(m) ? requiresBrowserless(m) : m;
            }

            /** The handle fields (a window or the extension) the method still names. */
            private List<String> handlesUsed(J.MethodDeclaration m) {
                java.util.Set<String> used = new java.util.LinkedHashSet<>();
                new JavaIsoVisitor<Integer>() {
                    @Override
                    public J.Identifier visitIdentifier(J.Identifier identifier, Integer p) {
                        if (Browserless.isHandle(identifier) && identifier.getFieldType() != null
                                && identifier.getFieldType().getOwner() instanceof JavaType.FullyQualified) {
                            used.add(identifier.getSimpleName());
                        }
                        return identifier;
                    }
                }.visit(m.getBody(), 0);
                return new ArrayList<>(used);
            }

            private boolean usesEscapeHatch(J.MethodDeclaration m) {
                AtomicBoolean uses = new AtomicBoolean();
                new JavaIsoVisitor<Integer>() {
                    @Override
                    public J.Identifier visitIdentifier(J.Identifier identifier, Integer p) {
                        if (identifier.getSimpleName().equals(Browserless.VARIABLE)
                                && TypeUtils.isOfClassType(identifier.getType(), Browserless.ESCAPE_HATCH)) {
                            uses.set(true);
                        }
                        return identifier;
                    }
                }.visit(m.getBody(), 0);
                return uses.get();
            }

            /**
             * The method with the given declaration as its first statement. Parsed on its own and inserted, as a
             * template inserted before a statement this recipe chain already rewrote can be lost.
             */
            private J.MethodDeclaration withFirst(J.MethodDeclaration m, ExecutionContext ctx, String imports,
                    String declarationSource) {
                String source = imports + "\nclass EscapeHatchStatement { void m(" + TestBenchSetupToUnified.UNIFIED_PAGE
                        + " page) { " + declarationSource + " } }";
                J.CompilationUnit cu = (J.CompilationUnit) Stubs.parser(
                                "package com.vaadin.browserless; public class BrowserlessUIContext {}").build().parse(ctx, source)
                        .findFirst().orElseThrow();
                J.MethodDeclaration parsed = (J.MethodDeclaration) cu.getClasses().get(0).getBody().getStatements().get(0);
                Statement declaration = parsed.getBody().getStatements().get(0);
                List<Statement> statements = new ArrayList<>(m.getBody().getStatements());
                Statement first = statements.get(0);
                statements.add(0, declaration.withPrefix(first.getPrefix().withComments(emptyList())));
                statements.set(1, first.withPrefix(first.getPrefix().withWhitespace(
                        "\n" + first.getPrefix().getIndent())));
                return m.withBody(m.getBody().withStatements(statements));
            }

            /** A test that calls a helper going through the Escape Hatch runs on Browserless only, too. */
            private boolean callsAnEscapingHelper(J.MethodDeclaration m) {
                J.ClassDeclaration c = getCursor().firstEnclosing(J.ClassDeclaration.class);
                if (!Helpers.isBrowserTest(m) || c == null) {
                    return false;
                }
                Set<String> escaping = Helpers.using(c, Set.of(), Browserless::isTestBaseCall);
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

            private J.MethodDeclaration requiresBrowserless(J.MethodDeclaration m) {
                if (m.getLeadingAnnotations().stream().anyMatch(a -> org.openrewrite.java.tree.TypeUtils
                        .isOfClassType(a.getType(), TestBenchLeftoversToEscapeHatch.REQUIRES_TESTING_TOOL))) {
                    return m;
                }
                maybeAddImport(TestBenchLeftoversToEscapeHatch.REQUIRES_TESTING_TOOL);
                maybeAddImport(TestBenchLeftoversToEscapeHatch.TESTING_TOOL);
                return JavaTemplate.builder("@RequiresTestingTool(TestingTool.BROWSERLESS)")
                        .imports(TestBenchLeftoversToEscapeHatch.REQUIRES_TESTING_TOOL, TestBenchLeftoversToEscapeHatch.TESTING_TOOL)
                        .javaParser(Stubs.parser())
                        .build()
                        .apply(updateCursor(m), m.getCoordinates().addAnnotation((a, b) -> 1));
            }

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                AtomicBoolean escapes = getCursor().getNearestMessage(LEFTOVERS);
                if (escapes == null || !Browserless.isTestBaseCall(method)) {
                    return m;
                }
                if (!insideAnotherLeftover() && !MigrationMarkers.add(getCursor(), reason(method))) {
                    return m;
                }
                if (method.getSelect() != null) {
                    return m; // a call on a window or extension: it keeps its handle, which the method declares
                }
                escapes.set(true);
                return m.getPadding().withSelect(JRightPadded.build(new J.Identifier(Tree.randomId(), Space.EMPTY,
                        Markers.EMPTY, emptyList(), Browserless.VARIABLE, JavaType.ShallowClass.build(Browserless.ESCAPE_HATCH), null)));
            }

            /** Whether an enclosing call of the same statement is a leftover too: it carries the statement's marker. */
            private boolean insideAnotherLeftover() {
                for (Cursor c = getCursor().getParentTreeCursor(); c.getValue() instanceof J; c = c.getParentTreeCursor()) {
                    if (c.getValue() instanceof J.MethodInvocation call && Browserless.isTestBaseCall(call)) {
                        return true;
                    }
                    if (c.getValue() instanceof J.Block || c.getValue() instanceof J.MethodDeclaration) {
                        return false;
                    }
                }
                return false;
            }

            /** The Migration Marker text: why the call stays behind the Escape Hatch. */
            private String reason(J.MethodInvocation call) {
                // the call and the calls chained on it: what the Wrapper would have to say
                Cursor shown = getCursor();
                while (shown.getParentTreeCursor().getValue() instanceof J.MethodInvocation chained
                        && chained.getSelect() == shown.getValue()) {
                    shown = shown.getParentTreeCursor();
                }
                String printed = ((J) shown.getValue()).printTrimmed(shown.getParentTreeCursor());
                Expression query = Browserless.isTest(call) ? call.getArguments().get(0) : call;
                J.MethodInvocation start = queryStart(query);
                if (Browserless.isTest(call) && start == null) {
                    return "Component Reference: " + printed + ": replace it with a locator that finds the same component";
                }
                if (start != null) {
                    String javaFilter = javaFilter(query);
                    if (javaFilter != null) {
                        return "filter runs Java on the component: " + printed + ": replace " + javaFilter
                                + "(..) with a locator";
                    }
                    if (Browserless.ofLookupStart(start) == null && start.getArguments().get(0) instanceof J.FieldAccess literal) {
                        return "Browserless Escape Hatch: " + printed + ", no Wrapper for " + literal.getTarget()
                                .printTrimmed(getCursor()) + ": write one (ADR 0003)";
                    }
                }
                return "Browserless Escape Hatch: " + printed;
            }
        };
    }

    /** The {@code $(X.class)} a query expression starts with, or {@code null} when it is not a query. */
    private static J.MethodInvocation queryStart(Expression e) {
        while (e instanceof J.MethodInvocation call) {
            if (call.getSelect() == null) {
                return Browserless.isTestBaseCall(call) && Set.of("$", "$view", "find", "findInView")
                        .contains(call.getSimpleName()) && call.getArguments().size() == 1 ? call : null;
            }
            e = call.getSelect();
        }
        return null;
    }

    private static String javaFilter(Expression e) {
        while (e instanceof J.MethodInvocation call && call.getSelect() != null) {
            if (JAVA_FILTERS.contains(call.getSimpleName())) {
                return call.getSimpleName();
            }
            e = call.getSelect();
        }
        return null;
    }
}
