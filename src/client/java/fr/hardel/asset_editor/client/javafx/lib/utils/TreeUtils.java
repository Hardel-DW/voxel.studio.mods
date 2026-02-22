package fr.hardel.asset_editor.client.javafx.lib.utils;

import fr.hardel.asset_editor.client.javafx.components.ui.tree.TreeNodeModel;

public final class TreeUtils {

    public static boolean hasActiveDescendant(TreeNodeModel node, String activeId) {
        if (activeId == null || activeId.isBlank()) return false;
        if (activeId.equals(node.elementId())) return true;
        for (TreeNodeModel child : node.children().values()) {
            if (hasActiveDescendant(child, activeId)) return true;
        }
        return false;
    }

    public static int recalculateCount(TreeNodeModel node) {
        if (node.elementId() != null && !node.elementId().isBlank()) return 1;
        int value = 0;
        for (TreeNodeModel child : node.children().values()) {
            value += recalculateCount(child);
        }
        node.setCount(value);
        return value;
    }

    private TreeUtils() {
    }
}
