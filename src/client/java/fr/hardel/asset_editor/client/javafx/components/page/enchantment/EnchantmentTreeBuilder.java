package fr.hardel.asset_editor.client.javafx.components.page.enchantment;

import fr.hardel.asset_editor.client.javafx.components.ui.tree.TreeNodeModel;
import fr.hardel.asset_editor.client.javafx.lib.data.StudioSidebarView;
import fr.hardel.asset_editor.client.javafx.lib.data.mock.StudioMockEnchantment;
import fr.hardel.asset_editor.client.javafx.lib.utils.TextUtils;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EnchantmentTreeBuilder {

    private static final List<SlotConfig> SLOT_CONFIGS = List.of(
            new SlotConfig("mainhand", List.of("mainhand", "any", "hand", "all")),
            new SlotConfig("offhand", List.of("offhand", "any", "hand", "all")),
            new SlotConfig("body", List.of("body", "any", "all")),
            new SlotConfig("saddle", List.of("saddle", "any", "all")),
            new SlotConfig("head", List.of("head", "any", "armor", "all")),
            new SlotConfig("chest", List.of("chest", "any", "armor", "all")),
            new SlotConfig("legs", List.of("legs", "any", "armor", "all")),
            new SlotConfig("feet", List.of("feet", "any", "armor", "all"))
    );

    private static final List<ItemTagConfig> ITEM_TAGS = List.of(
            new ItemTagConfig("sword", 0, 89),
            new ItemTagConfig("sweeping", 90, Integer.MAX_VALUE),
            new ItemTagConfig("melee_weapon", 90, Integer.MAX_VALUE),
            new ItemTagConfig("lunge", 90, Integer.MAX_VALUE),
            new ItemTagConfig("trident", 0, Integer.MAX_VALUE),
            new ItemTagConfig("mace", 0, Integer.MAX_VALUE),
            new ItemTagConfig("bow", 0, Integer.MAX_VALUE),
            new ItemTagConfig("crossbow", 0, Integer.MAX_VALUE),
            new ItemTagConfig("range", 0, Integer.MAX_VALUE),
            new ItemTagConfig("fishing", 0, Integer.MAX_VALUE),
            new ItemTagConfig("shield", 0, Integer.MAX_VALUE),
            new ItemTagConfig("weapon", 0, Integer.MAX_VALUE),
            new ItemTagConfig("melee", 0, Integer.MAX_VALUE),
            new ItemTagConfig("head_armor", 0, Integer.MAX_VALUE),
            new ItemTagConfig("chest_armor", 0, Integer.MAX_VALUE),
            new ItemTagConfig("leg_armor", 0, Integer.MAX_VALUE),
            new ItemTagConfig("foot_armor", 0, Integer.MAX_VALUE),
            new ItemTagConfig("elytra", 0, Integer.MAX_VALUE),
            new ItemTagConfig("armor", 0, Integer.MAX_VALUE),
            new ItemTagConfig("equippable", 0, Integer.MAX_VALUE),
            new ItemTagConfig("axes", 0, Integer.MAX_VALUE),
            new ItemTagConfig("shovels", 0, Integer.MAX_VALUE),
            new ItemTagConfig("hoes", 0, Integer.MAX_VALUE),
            new ItemTagConfig("pickaxes", 0, Integer.MAX_VALUE),
            new ItemTagConfig("durability", 0, Integer.MAX_VALUE),
            new ItemTagConfig("mining_loot", 0, Integer.MAX_VALUE)
    );

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
        for (SlotConfig config : SLOT_CONFIGS) {
            icons.put(config.id(), icon("textures/features/slots/%s.png".formatted(config.id())));
        }
        return icons;
    }

    public static Map<String, Identifier> itemFolderIcons(int version) {
        LinkedHashMap<String, Identifier> icons = new LinkedHashMap<>();
        for (ItemTagConfig tag : ITEM_TAGS) {
            if (version < tag.min() || version > tag.max()) continue;
            icons.put(tag.key(), icon("textures/features/item/%s.png".formatted(tag.key())));
        }
        return icons;
    }

    private static void buildBySlots(TreeNodeModel root, List<StudioMockEnchantment> enchantments) {
        for (SlotConfig config : SLOT_CONFIGS) {
            ArrayList<StudioMockEnchantment> matching = new ArrayList<>();
            for (StudioMockEnchantment enchantment : enchantments) {
                if (hasOne(enchantment.slots(), config.slots())) {
                    matching.add(enchantment);
                }
            }
            if (!matching.isEmpty()) {
                TreeNodeModel category = createCategoryNode(matching);
                category.setIcon(icon("textures/features/slots/%s.png".formatted(config.id())));
                root.children().put(config.id(), category);
            }
        }
    }

    private static void buildByItems(TreeNodeModel root, List<StudioMockEnchantment> enchantments, int version) {
        for (ItemTagConfig tag : ITEM_TAGS) {
            if (version < tag.min() || version > tag.max()) continue;
            ArrayList<StudioMockEnchantment> matching = new ArrayList<>();
            for (StudioMockEnchantment enchantment : enchantments) {
                if (enchantment.items().contains(tag.key())) {
                    matching.add(enchantment);
                }
            }
            if (!matching.isEmpty()) {
                TreeNodeModel category = createCategoryNode(matching);
                category.setIcon(icon("textures/features/item/%s.png".formatted(tag.key())));
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

    private static Identifier icon(String path) {
        return Identifier.fromNamespaceAndPath("asset_editor", path);
    }

    private record SlotConfig(String id, List<String> slots) {
    }

    private record ItemTagConfig(String key, int min, int max) {
    }

    private EnchantmentTreeBuilder() {
    }
}
