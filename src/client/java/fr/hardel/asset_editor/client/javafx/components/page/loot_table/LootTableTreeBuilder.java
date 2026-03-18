package fr.hardel.asset_editor.client.javafx.components.page.loot_table;

import fr.hardel.asset_editor.client.javafx.components.ui.tree.TreeNodeModel;
import fr.hardel.asset_editor.client.javafx.lib.data.StudioElementId;
import fr.hardel.asset_editor.client.javafx.lib.utils.TreeUtils;
import net.minecraft.resources.Identifier;

import java.util.List;

public final class LootTableTreeBuilder {

    public static TreeNodeModel build(List<String> elementIds) {
        TreeNodeModel root = new TreeNodeModel();
        root.setCount(elementIds.size());

        for (String elementId : elementIds) {
            StudioElementId parsed = StudioElementId.parse(elementId);
            if (parsed == null)
                continue;
            Identifier identifier = parsed.identifier();

            String[] parts = identifier.getPath().split("/");
            TreeNodeModel current = root;

            current = ensureFolder(current, identifier.getNamespace());
            for (int i = 0; i < parts.length - 1; i++) {
                current = ensureFolder(current, parts[i]);
            }

            String elementName = parts.length == 0 ? identifier.getPath() : parts[parts.length - 1];
            TreeNodeModel leaf = new TreeNodeModel();
            leaf.setCount(1);
            leaf.setElementId(identifier.toString());
            current.children().put(elementName, leaf);
        }

        TreeUtils.recalculateCount(root);
        return root;
    }

    private static TreeNodeModel ensureFolder(TreeNodeModel parent, String name) {
        TreeNodeModel existing = parent.children().get(name);
        if (existing != null)
            return existing;
        TreeNodeModel folder = new TreeNodeModel();
        folder.setFolder(true);
        parent.children().put(name, folder);
        return folder;
    }

    private LootTableTreeBuilder() {}
}
