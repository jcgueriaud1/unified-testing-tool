package org.vaadin.addons.unified.rewrite;

import static java.util.Collections.emptyList;

import java.util.Arrays;
import org.openrewrite.Tree;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;

/**
 * Source stubs of the Unified API types the recipes write, so templates type-check in a project that does
 * not have the Unified API on its classpath yet. Only the members the recipes use.
 */
final class Stubs {

    private static final String[] SOURCES = {
            """
            package com.vaadin.testing.unified;
            public interface UnifiedPage {
                void navigate(String path);
                void navigate(Class<?> view);
                Object find(Class<?> type);
            }
            """,
            """
            package com.vaadin.testing.unified.assertion;
            public final class UnifiedAssertions {
                public static Assert assertThat(Object wrapper) { return null; }
                public interface Assert {
                    Assert hasValue(Object value);
                    Assert hasText(String text);
                }
            }
            """
    };

    private Stubs() {
    }

    /** A parser that knows the Unified API stubs and the given extra stub sources. */
    static JavaParser.Builder<?, ?> parser(String... extra) {
        String[] all = Arrays.copyOf(SOURCES, SOURCES.length + extra.length);
        System.arraycopy(extra, 0, all, SOURCES.length, extra.length);
        return JavaParser.fromJavaVersion().dependsOn(all);
    }

    /** An empty class stub, so a template can name a class of the project being migrated. */
    static String classStub(String fqn) {
        int dot = fqn.lastIndexOf('.');
        String pkg = dot < 0 ? "" : "package " + fqn.substring(0, dot) + ";\n";
        return pkg + "public class " + fqn.substring(dot + 1) + " {}";
    }

    /** An empty interface stub, for a Wrapper a template names. */
    static String interfaceStub(String fqn) {
        int dot = fqn.lastIndexOf('.');
        return "package " + fqn.substring(0, dot) + ";\npublic interface " + fqn.substring(dot + 1) + " {}";
    }

    /** The {@code page} parameter the setup recipe adds to every test method. */
    static J.Identifier page() {
        return new J.Identifier(Tree.randomId(), Space.EMPTY, Markers.EMPTY, emptyList(), "page",
                JavaType.ShallowClass.build(TestBenchSetupToUnified.UNIFIED_PAGE), null);
    }

    static J.Literal literal(String value) {
        return new J.Literal(Tree.randomId(), Space.EMPTY, Markers.EMPTY, value,
                "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"", null, JavaType.Primitive.String);
    }
}
