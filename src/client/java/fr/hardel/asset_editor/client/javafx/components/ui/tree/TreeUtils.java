package fr.hardel.asset_editor.client.javafx.components.ui.tree;

public final class TreeUtils {

    public static boolean hasActiveDescendant(TreeNodeModel node, String activeId) {
        if (activeId == null || activeId.isBlank()) return false;
        if (activeId.equals(node.elementId())) return true;
        for (TreeNodeModel child : node.children().values()) {
            if (hasActiveDescendant(child, activeId)) return true;
        }
        return false;
    }

    public static int calculateCount(TreeNodeModel node) {
        if (node.elementId() != null && !node.elementId().isBlank()) return 1;
        int count = 0;
        for (TreeNodeModel child : node.children().values()) {
            count += calculateCount(child);
        }
        node.setCount(count);
        return count;
    }

    private TreeUtils() {
    }
}
