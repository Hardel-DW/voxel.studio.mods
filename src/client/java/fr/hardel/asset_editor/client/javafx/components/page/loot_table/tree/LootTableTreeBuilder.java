package fr.hardel.asset_editor.client.javafx.components.page.loot_table.tree;

import fr.hardel.asset_editor.client.javafx.components.ui.tree.TreeNodeModel;

import java.util.List;

public final class LootTableTreeBuilder {

    public static TreeNodeModel build(List<String> elementIds) {
        TreeNodeModel root = new TreeNodeModel();
        root.setCount(elementIds.size());

        for (String elementId : elementIds) {
            ParsedIdentifier id = ParsedIdentifier.parse(elementId);
            String[] parts = id.resource().split("/");
            TreeNodeModel current = root;

            current = ensureFolder(current, id.namespace());
            for (int i = 0; i < parts.length - 1; i++) {
                current = ensureFolder(current, parts[i]);
            }

            String elementName = parts.length == 0 ? id.resource() : parts[parts.length - 1];
            TreeNodeModel leaf = new TreeNodeModel();
            leaf.setCount(1);
            leaf.setElementId(id.uniqueKey());
            current.children().put(elementName, leaf);
        }

        root.recalculateCount();
        return root;
    }

    private static TreeNodeModel ensureFolder(TreeNodeModel parent, String name) {
        TreeNodeModel existing = parent.children().get(name);
        if (existing != null) return existing;
        TreeNodeModel folder = new TreeNodeModel();
        folder.setFolder(true);
        parent.children().put(name, folder);
        return folder;
    }

    private record ParsedIdentifier(String namespace, String resource, String uniqueKey) {
        static ParsedIdentifier parse(String id) {
            String value = id;
            int reg = value.indexOf('$');
            if (reg >= 0) value = value.substring(0, reg);
            int sep = value.indexOf(':');
            if (sep < 0) return new ParsedIdentifier("minecraft", value, "minecraft:" + value);
            return new ParsedIdentifier(value.substring(0, sep), value.substring(sep + 1), value);
        }
    }

    private LootTableTreeBuilder() {
    }
}
