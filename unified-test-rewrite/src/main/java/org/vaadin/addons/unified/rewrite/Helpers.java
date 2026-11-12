package org.vaadin.addons.unified.rewrite;

import static java.util.Collections.emptyList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.openrewrite.Tree;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JContainer;
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.Markers;

/**
 * The non-test methods of a TestBench test class that use TestBench, directly or through one another. They get
 * the Unified Page as their first parameter; a {@code @BeforeEach} among them becomes a call at the start of
 * every test, since a lifecycle method gets a page of its own, not the test's.
 */
final class Helpers {

    private Helpers() {
    }

    static Set<String> usingTestBench(J.ClassDeclaration c) {
        return usingTestBench(c, Set.of());
    }

    /** The class's own helpers using TestBench, counting calls to the given inherited helpers as such use. */
    static Set<String> usingTestBench(J.ClassDeclaration c, Set<String> inherited) {
        Map<String, Set<String>> calls = new HashMap<>();
        Set<String> using = new HashSet<>();
        for (Statement s : c.getBody().getStatements()) {
            if (s instanceof J.MethodDeclaration m && m.getBody() != null && !isBrowserTest(m)
                    && !m.hasModifier(J.Modifier.Type.Static)) {
                calls.put(m.getSimpleName(), new HashSet<>());
                AtomicBoolean direct = new AtomicBoolean();
                new JavaIsoVisitor<Integer>() {
                    @Override
                    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation call, Integer p) {
                        if (call.getSelect() == null && (TestBenchLeftoversToEscapeHatch.isTestBaseCall(call)
                                || inherited.contains(call.getSimpleName()))) {
                            direct.set(true);
                        } else if (call.getSelect() == null) {
                            calls.get(m.getSimpleName()).add(call.getSimpleName());
                        }
                        return super.visitMethodInvocation(call, p);
                    }
                }.visit(m.getBody(), 0);
                if (direct.get()) {
                    using.add(m.getSimpleName());
                }
            }
        }
        boolean grew = true;
        while (grew) {
            grew = false;
            for (var e : calls.entrySet()) {
                if (!using.contains(e.getKey()) && e.getValue().stream().anyMatch(using::contains)) {
                    using.add(e.getKey());
                    grew = true;
                }
            }
        }
        return using;
    }

    /** The class's statements with every test method starting with calls to the given methods, passing the page. */
    static List<Statement> callBeforeEachFirst(J.ClassDeclaration c, List<String> beforeEach) {
        if (beforeEach.isEmpty()) {
            return c.getBody().getStatements();
        }
        Map<String, JavaType.Method> types = new HashMap<>();
        for (JavaType.FullyQualified t = c.getType() == null ? null : c.getType().getSupertype(); t != null; t = t.getSupertype()) {
            for (JavaType.Method inherited : t.getMethods()) {
                types.putIfAbsent(inherited.getName(), inherited);
            }
        }
        for (Statement s : c.getBody().getStatements()) {
            if (s instanceof J.MethodDeclaration m) {
                types.put(m.getSimpleName(), m.getMethodType());
            }
        }
        List<Statement> result = new ArrayList<>();
        for (Statement s : c.getBody().getStatements()) {
            if (s instanceof J.MethodDeclaration m && isBrowserTest(m) && m.getBody() != null
                    && !m.getBody().getStatements().isEmpty()) {
                Space prefix = m.getBody().getStatements().get(0).getPrefix().withComments(emptyList());
                List<Statement> statements = new ArrayList<>();
                for (String name : beforeEach) {
                    statements.add(call(name, types.get(name), prefix));
                }
                statements.addAll(m.getBody().getStatements());
                s = m.withBody(m.getBody().withStatements(statements));
            }
            result.add(s);
        }
        return result;
    }

    private static J.MethodInvocation call(String name, JavaType.Method type, Space prefix) {
        return new J.MethodInvocation(Tree.randomId(), prefix, Markers.EMPTY, null, null,
                new J.Identifier(Tree.randomId(), Space.EMPTY, Markers.EMPTY, emptyList(), name, null, null),
                JContainer.build(Space.EMPTY, List.of(JRightPadded.<Expression>build(Stubs.page())), Markers.EMPTY),
                type);
    }

    static boolean isBrowserTest(J.MethodDeclaration m) {
        return m.getLeadingAnnotations().stream()
                .anyMatch(a -> TypeUtils.isOfClassType(a.getType(), TestBenchSetupToUnified.BROWSER_TEST));
    }
}
