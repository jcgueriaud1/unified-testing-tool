package org.vaadin.addons.unified.rewrite;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.java.tree.TypeUtils;

/** What the Dramafinder recipes know about Dramafinder's test base classes and API (ADR 0006). */
final class Dramafinder {

    static final String BASE = "org.vaadin.addons.dramafinder.AbstractBasePlaywrightIT";
    static final String SPRING_BASE = "org.vaadin.addons.dramafinder.spring.SpringPlaywrightIT";
    static final String HAS_TEST_VIEW = "org.vaadin.addons.dramafinder.HasTestView";
    static final String PAGE = "com.microsoft.playwright.Page";
    /** The local the Escape Hatch declares for the Playwright page. */
    static final String VARIABLE = "playwright";
    private static final Set<String> BASE_TYPES = Set.of(BASE, SPRING_BASE, HAS_TEST_VIEW);
    /** Base class methods the setup recipe converts; any other base class member keeps a test from migrating. */
    static final Set<String> CONVERTED_CALLS = Set.of("navigate", "assertAt", "assertPath", "assertQueryParameter",
            "getPage");
    /** Base class methods a test may override: the base URL and the view each test starts at. */
    static final Set<String> CONVERTED_OVERRIDES = Set.of("getUrl", "getView");

    private Dramafinder() {
    }

    /**
     * A test class extending Dramafinder's {@code AbstractBasePlaywrightIT} or {@code SpringPlaywrightIT} directly,
     * or a copy of {@code SpringPlaywrightIT} in the project, as Dramafinder 1.x projects have.
     */
    static boolean isDramafinderClass(J.ClassDeclaration c) {
        if (c.getExtends() == null || c.hasModifier(J.Modifier.Type.Abstract)) {
            return false;
        }
        JavaType.FullyQualified parent = TypeUtils.asFullyQualified(c.getExtends().getType());
        return parent != null && (parent.getFullyQualifiedName().equals(BASE)
                || parent.getFullyQualifiedName().equals(SPRING_BASE) || isCopiedSpringBase(parent));
    }

    static boolean isSpringClass(J.ClassDeclaration c) {
        JavaType.FullyQualified parent = c.getExtends() == null ? null : TypeUtils.asFullyQualified(c.getExtends().getType());
        return parent != null && (parent.getFullyQualifiedName().equals(SPRING_BASE) || isCopiedSpringBase(parent));
    }

    /**
     * A project's copy of {@code SpringPlaywrightIT}, from the template Dramafinder 1.x projects started with: an
     * abstract class extending {@code AbstractBasePlaywrightIT} with {@code @SpringBootTest}.
     */
    static boolean isCopiedSpringBase(JavaType.FullyQualified type) {
        return type != null && !BASE_TYPES.contains(type.getFullyQualifiedName())
                && type.hasFlags(org.openrewrite.java.tree.Flag.Abstract) && type.getSupertype() != null
                && type.getSupertype().getFullyQualifiedName().equals(BASE)
                && type.getAnnotations().stream().anyMatch(a -> a.getFullyQualifiedName()
                        .equals("org.springframework.boot.test.context.SpringBootTest"));
    }

    /** One of Dramafinder's base types, or a project's copy of {@code SpringPlaywrightIT}. */
    static boolean isBaseType(JavaType type) {
        JavaType.FullyQualified fq = TypeUtils.asFullyQualified(type);
        return fq != null && (BASE_TYPES.contains(fq.getFullyQualifiedName()) || isCopiedSpringBase(fq));
    }

    /** The base class's {@code page} field. */
    static boolean isPageField(Expression e) {
        if (e instanceof J.FieldAccess f && f.getTarget() instanceof J.Identifier t && t.getSimpleName().equals("this")) {
            e = f.getName();
        }
        return e instanceof J.Identifier id && id.getSimpleName().equals("page") && id.getFieldType() != null
                && isBaseType(id.getFieldType().getOwner());
    }

    /** A call to a base class method, on this test. */
    static boolean isBaseCall(J.MethodInvocation m) {
        if (m.getSelect() != null && !(m.getSelect() instanceof J.Identifier s
                && (s.getSimpleName().equals("this") || s.getSimpleName().equals("super")))) {
            return false;
        }
        return m.getMethodType() != null && isBaseType(m.getMethodType().getDeclaringType());
    }

    /** {@code getPage()} on this test: the base class's page. */
    static boolean isGetPage(J.MethodInvocation m) {
        return isBaseCall(m) && m.getSimpleName().equals("getPage") && m.getArguments().stream().allMatch(a -> a instanceof J.Empty);
    }

    /** A call that needs the test base: a base class method, or a call handed or made on the base class's page. */
    static boolean usesTestBase(J.MethodInvocation m) {
        return isBaseCall(m) || m.getSelect() != null && isPageField(m.getSelect())
                || m.getArguments().stream().anyMatch(Dramafinder::isPageField);
    }

    /** A Dramafinder or Playwright method or constructor: code that runs on Playwright only. */
    static boolean isToolType(JavaType.FullyQualified type) {
        if (type == null) {
            return false;
        }
        String name = type.getFullyQualifiedName();
        return name.startsWith("org.vaadin.addons.dramafinder.") || name.startsWith("com.microsoft.playwright.");
    }

    static boolean isToolCall(J j) {
        if (j instanceof J.MethodInvocation m) {
            return m.getMethodType() != null && !isBaseCall(m) && (isToolType(m.getMethodType().getDeclaringType())
                    || isCustomElement(m.getMethodType().getDeclaringType()));
        }
        return j instanceof J.NewClass n && (isToolType(TypeUtils.asFullyQualified(n.getType()))
                || isCustomElement(TypeUtils.asFullyQualified(n.getType())));
    }

    /** A project's own Dramafinder element: a class extending Dramafinder's {@code VaadinElement}. */
    static boolean isCustomElement(JavaType.FullyQualified type) {
        if (type == null || isToolType(type)) {
            return false;
        }
        for (JavaType.FullyQualified t = type.getSupertype(); t != null; t = t.getSupertype()) {
            if (t.getFullyQualifiedName().equals("org.vaadin.addons.dramafinder.element.VaadinElement")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether the recipes can migrate the class: it uses no base class member they don't convert. A test that
     * calls {@code loginAs(..)}, overrides {@code prepareBrowserContext(..)} or computes its view is left as it is.
     */
    static boolean migratable(J.ClassDeclaration c) {
        for (Statement s : c.getBody().getStatements()) {
            if (s instanceof J.MethodDeclaration m && overridesBase(c, m)
                    && !(CONVERTED_OVERRIDES.contains(m.getSimpleName()) && (m.getSimpleName().equals("getUrl")
                            || viewOf(c) != null))) {
                return false;
            }
        }
        AtomicBoolean ok = new AtomicBoolean(true);
        new JavaIsoVisitor<Integer>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation m, Integer p) {
                if (isBaseCall(m) && !CONVERTED_CALLS.contains(m.getSimpleName())) {
                    ok.set(false);
                }
                return super.visitMethodInvocation(m, p);
            }

            @Override
            public J.Identifier visitIdentifier(J.Identifier id, Integer p) {
                // a base class field other than page (the page itself the Escape Hatch replaces)
                if (id.getFieldType() != null && isBaseType(id.getFieldType().getOwner()) && !id.getSimpleName().equals("page")) {
                    ok.set(false);
                }
                return id;
            }
        }.visit(c.getBody(), 0);
        return ok.get();
    }

    private static boolean overridesBase(J.ClassDeclaration c, J.MethodDeclaration m) {
        if (m.getMethodType() == null || m.hasModifier(J.Modifier.Type.Static)) {
            return false;
        }
        for (JavaType.FullyQualified t = c.getType() == null ? null : c.getType().getSupertype(); t != null; t = t.getSupertype()) {
            if (isBaseType(t) && t.getMethods().stream()
                    .anyMatch(b -> b.getName().equals(m.getSimpleName()) && b.getParameterTypes().size() == m.getMethodType().getParameterTypes().size())) {
                return true;
            }
        }
        // HasTestView, the interface the base class implements
        return CONVERTED_OVERRIDES.contains(m.getSimpleName()) && m.getParameters().stream().allMatch(p -> p instanceof J.Empty);
    }

    /**
     * The path each test starts at: what the class's {@code getView()} returns, {@code ""} when it doesn't
     * override it, {@code null} when it returns anything but a string literal.
     */
    static String viewOf(J.ClassDeclaration c) {
        for (Statement s : c.getBody().getStatements()) {
            if (s instanceof J.MethodDeclaration m && m.getSimpleName().equals("getView")
                    && m.getParameters().stream().allMatch(p -> p instanceof J.Empty)) {
                if (m.getBody() != null && m.getBody().getStatements().size() == 1
                        && m.getBody().getStatements().get(0) instanceof J.Return r
                        && r.getExpression() instanceof J.Literal l && l.getValue() instanceof String view) {
                    return view;
                }
                return null;
            }
        }
        return "";
    }
}
