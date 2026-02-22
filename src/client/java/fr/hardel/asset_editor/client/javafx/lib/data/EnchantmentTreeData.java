package fr.hardel.asset_editor.client.javafx.lib.data;

import net.minecraft.resources.Identifier;

import java.util.List;

public final class EnchantmentTreeData {

    public static final List<SlotConfig> SLOT_CONFIGS = List.of(
            slot("mainhand", List.of("mainhand", "any", "hand", "all")),
            slot("offhand", List.of("offhand", "any", "hand", "all")),
            slot("body", List.of("body", "any", "all")),
            slot("saddle", List.of("saddle", "any", "all")),
            slot("head", List.of("head", "any", "armor", "all")),
            slot("chest", List.of("chest", "any", "armor", "all")),
            slot("legs", List.of("legs", "any", "armor", "all")),
            slot("feet", List.of("feet", "any", "armor", "all"))
    );

    public static final List<ItemTagConfig> ITEM_TAGS = List.of(
            item("sword", 0, 89),
            item("sweeping", 90, Integer.MAX_VALUE),
            item("melee_weapon", 90, Integer.MAX_VALUE),
            item("lunge", 90, Integer.MAX_VALUE),
            item("trident", 0, Integer.MAX_VALUE),
            item("mace", 0, Integer.MAX_VALUE),
            item("bow", 0, Integer.MAX_VALUE),
            item("crossbow", 0, Integer.MAX_VALUE),
            item("range", 0, Integer.MAX_VALUE),
            item("fishing", 0, Integer.MAX_VALUE),
            item("shield", 0, Integer.MAX_VALUE),
            item("weapon", 0, Integer.MAX_VALUE),
            item("melee", 0, Integer.MAX_VALUE),
            item("head_armor", 0, Integer.MAX_VALUE),
            item("chest_armor", 0, Integer.MAX_VALUE),
            item("leg_armor", 0, Integer.MAX_VALUE),
            item("foot_armor", 0, Integer.MAX_VALUE),
            item("elytra", 0, Integer.MAX_VALUE),
            item("armor", 0, Integer.MAX_VALUE),
            item("equippable", 0, Integer.MAX_VALUE),
            item("axes", 0, Integer.MAX_VALUE),
            item("shovels", 0, Integer.MAX_VALUE),
            item("hoes", 0, Integer.MAX_VALUE),
            item("pickaxes", 0, Integer.MAX_VALUE),
            item("durability", 0, Integer.MAX_VALUE),
            item("mining_loot", 0, Integer.MAX_VALUE)
    );

    public record SlotConfig(String id, List<String> slots, Identifier icon) {
    }

    public record ItemTagConfig(String key, int min, int max, Identifier icon) {
    }

    private static SlotConfig slot(String id, List<String> slots) {
        return new SlotConfig(id, slots, icon("textures/features/slots/%s.png".formatted(id)));
    }

    private static ItemTagConfig item(String key, int min, int max) {
        return new ItemTagConfig(key, min, max, icon("textures/features/item/%s.png".formatted(key)));
    }

    private static Identifier icon(String path) {
        return Identifier.fromNamespaceAndPath("asset_editor", path);
    }

    private EnchantmentTreeData() {
    }
}
