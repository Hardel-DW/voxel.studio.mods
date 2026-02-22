package fr.hardel.asset_editor.client.javafx.components.page.enchantment;

import fr.hardel.asset_editor.client.javafx.components.ui.tree.TreeNodeModel;
import fr.hardel.asset_editor.client.javafx.lib.data.EnchantmentTreeData;
import fr.hardel.asset_editor.client.javafx.lib.data.StudioSidebarView;
import fr.hardel.asset_editor.client.javafx.lib.data.mock.StudioMockEnchantment;
import fr.hardel.asset_editor.client.javafx.lib.utils.TextUtils;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EnchantmentTreeBuilder {

    public static TreeNodeModel build(List<StudioMockEnchantment> enchantments, StudioSidebarView view, int version) {
        TreeNodeModel root = new TreeNodeModel();
        root.setCount(enchantments.size());
        if (view == StudioSidebarView.SLOTS) {
            buildBySlots(root, enchantments);
            return root;
        }
        if (view == StudioSidebarView.ITEMS) {
            buildByItems(root, enchantments, version);
            return root;
        }
        buildByExclusive(root, enchantments);
        return root;
    }

    public static Map<String, Identifier> slotFolderIcons() {
        LinkedHashMap<String, Identifier> icons = new LinkedHashMap<>();
        for (EnchantmentTreeData.SlotConfig config : EnchantmentTreeData.SLOT_CONFIGS) {
            icons.put(config.id(), config.icon());
        }
        return icons;
    }

    public static Map<String, Identifier> itemFolderIcons(int version) {
        LinkedHashMap<String, Identifier> icons = new LinkedHashMap<>();
        for (EnchantmentTreeData.ItemTagConfig tag : EnchantmentTreeData.ITEM_TAGS) {
            if (version < tag.min() || version > tag.max()) continue;
            icons.put(tag.key(), tag.icon());
        }
        return icons;
    }

    private static void buildBySlots(TreeNodeModel root, List<StudioMockEnchantment> enchantments) {
        for (EnchantmentTreeData.SlotConfig config : EnchantmentTreeData.SLOT_CONFIGS) {
            ArrayList<StudioMockEnchantment> matching = new ArrayList<>();
            for (StudioMockEnchantment enchantment : enchantments) {
                if (hasOne(enchantment.slots(), config.slots())) {
                    matching.add(enchantment);
                }
            }
            if (!matching.isEmpty()) {
                TreeNodeModel category = createCategoryNode(matching);
                category.setIcon(config.icon());
                root.children().put(config.id(), category);
            }
        }
    }

    private static void buildByItems(TreeNodeModel root, List<StudioMockEnchantment> enchantments, int version) {
        for (EnchantmentTreeData.ItemTagConfig tag : EnchantmentTreeData.ITEM_TAGS) {
            if (version < tag.min() || version > tag.max()) continue;
            ArrayList<StudioMockEnchantment> matching = new ArrayList<>();
            for (StudioMockEnchantment enchantment : enchantments) {
                if (enchantment.items().contains(tag.key())) {
                    matching.add(enchantment);
                }
            }
            if (!matching.isEmpty()) {
                TreeNodeModel category = createCategoryNode(matching);
                category.setIcon(tag.icon());
                root.children().put(tag.key(), category);
            }
        }
    }

    private static void buildByExclusive(TreeNodeModel root, List<StudioMockEnchantment> enchantments) {
        LinkedHashMap<String, ArrayList<StudioMockEnchantment>> grouped = new LinkedHashMap<>();
        for (StudioMockEnchantment enchantment : enchantments) {
            String set = enchantment.exclusiveGroup();
            grouped.computeIfAbsent(set, key -> new ArrayList<>()).add(enchantment);
        }
        for (Map.Entry<String, ArrayList<StudioMockEnchantment>> entry : grouped.entrySet()) {
            String key = entry.getKey();
            String name = key.startsWith("#") ? key : TextUtils.toDisplay(key);
            root.children().put(name, createCategoryNode(entry.getValue()));
        }
    }

    private static TreeNodeModel createCategoryNode(List<StudioMockEnchantment> enchantments) {
        TreeNodeModel node = new TreeNodeModel();
        node.setCount(enchantments.size());
        for (StudioMockEnchantment enchantment : enchantments) {
            TreeNodeModel leaf = new TreeNodeModel();
            leaf.setCount(0);
            leaf.setElementId(enchantment.uniqueKey());
            node.children().put(enchantment.resource(), leaf);
        }
        return node;
    }

    private static boolean hasOne(List<String> source, List<String> targets) {
        for (String value : source) {
            if (targets.contains(value)) return true;
        }
        return false;
    }
    private EnchantmentTreeBuilder() {
    }
}
