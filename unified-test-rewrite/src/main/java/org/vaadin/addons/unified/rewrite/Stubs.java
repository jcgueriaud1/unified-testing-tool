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
            package com.vaadin.testing.unified.component;
            public interface UnifiedGrid {
                UnifiedGridRow row(int index);
                RowQuery rows();
                interface RowQuery {
                    RowQuery withCellText(String text);
                    UnifiedGridRow single();
                }
                interface UnifiedGridRow {
                    int getIndex();
                    String getCellText(int columnIndex);
                    void click();
                }
            }
            """,
            """
            package com.vaadin.testing.unified.selenium;
            public final class TestBenchEscapeHatch {
                public static TestBenchEscapeHatch of(com.vaadin.testing.unified.UnifiedPage page) { return null; }
            }
            """,
            """
            package com.vaadin.testing.unified;
            public enum TestingTool { BROWSERLESS, PLAYWRIGHT, SELENIUM }
            """,
            """
            package com.vaadin.testing.unified;
            public @interface RequiresTestingTool {
                TestingTool[] value();
            }
            """,
            """
            package com.vaadin.testing.unified.assertion;
            public final class UnifiedAssertions {
                public static Assert assertThat(Object wrapper) { return null; }
                public interface Assert {
                    Assert hasValue(Object value);
                    Assert hasText(String text);
                    Assert isOpen();
                    Assert isClosed();
                    Assert hasHeaderTitle(String title);
                    Assert hasSelectedTab(String tab);
                    Assert hasTabs(String... tabs);
                    Assert isChecked();
                    Assert isNotChecked();
                    Assert isEnabled();
                    Assert isDisabled();
                    Assert hasSelectedItem(String item);
                    Assert hasSelectedItems(String... items);
                    Assert hasItems(String... items);
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

    /** An empty interface stub, for a Wrapper a template names; none for Wrappers already stubbed above. */
    static String[] interfaceStub(String fqn) {
        if (Arrays.stream(SOURCES).anyMatch(source -> source.contains("public interface " + fqn.substring(fqn.lastIndexOf('.') + 1) + " "))) {
            return new String[0];
        }
        int dot = fqn.lastIndexOf('.');
        return new String[] {"package " + fqn.substring(0, dot) + ";\npublic interface " + fqn.substring(dot + 1) + " {}"};
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
