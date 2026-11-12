package org.vaadin.addons.unified.rewrite;

import org.openrewrite.Cursor;
import org.openrewrite.java.tree.J;

/**
 * TestBench's TabSheet is index-based, the Unified one label-based: only the label forms convert
 * ({@code setSelectedTabIndex(t.getTab(label))}, {@code getSelectedTabElement().getLabel()},
 * {@code getContent().$(..)}); bare indexes stay TestBench.
 */
final class TabSheets {

    static final String TAB_SHEET = "com.vaadin.testing.unified.component.UnifiedTabSheet";

    private TabSheets() {
    }

    static boolean converts(J.MethodInvocation call, Cursor callCursor) {
        Object parent = callCursor.getParentTreeCursor().getValue();
        return switch (call.getSimpleName()) {
            case "setSelectedTabIndex" -> tabLabel(call) != null;
            case "getTab" -> parent instanceof J.MethodInvocation select && select.getSimpleName().equals("setSelectedTabIndex")
                    && select.getArguments().get(0) == call;
            case "getSelectedTabElement" -> parent instanceof J.MethodInvocation next && next.getSelect() == call
                    && next.getSimpleName().equals("getLabel");
            case "getContent" -> parent instanceof J.MethodInvocation next && next.getSelect() == call
                    && Chains.scopedLookupConverts(next, callCursor.getParentTreeCursor());
            default -> false;
        };
    }

    /** The label in {@code t.setSelectedTabIndex(t.getTab(label))}, else {@code null}. */
    static J tabLabel(J.MethodInvocation setSelectedTabIndex) {
        if (setSelectedTabIndex.getArguments().size() == 1
                && setSelectedTabIndex.getArguments().get(0) instanceof J.MethodInvocation getTab
                && getTab.getSimpleName().equals("getTab") && getTab.getArguments().size() == 1
                && getTab.getSelect() instanceof J.Identifier a && setSelectedTabIndex.getSelect() instanceof J.Identifier b
                && a.getSimpleName().equals(b.getSimpleName())) {
            return getTab.getArguments().get(0);
        }
        return null;
    }
}
