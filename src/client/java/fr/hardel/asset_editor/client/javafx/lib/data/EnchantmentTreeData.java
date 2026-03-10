package fr.hardel.asset_editor.client.javafx.lib.data;

import net.minecraft.resources.Identifier;

import java.util.List;

public final class EnchantmentTreeData {

    public static final List<ItemTagConfig> ITEM_TAGS = List.of(
            item("sword"),
            item("trident"),
            item("mace"),
            item("bow"),
            item("crossbow"),
            item("range"),
            item("fishing"),
            item("shield"),
            item("weapon"),
            item("melee"),
            item("head_armor"),
            item("chest_armor"),
            item("leg_armor"),
            item("foot_armor"),
            item("elytra"),
            item("armor"),
            item("equippable"),
            item("axes"),
            item("shovels"),
            item("hoes"),
            item("pickaxes"),
            item("durability"),
            item("mining_loot")
    );

    public record ItemTagConfig(String key, Identifier icon) {}

    private static ItemTagConfig item(String key) {
        return new ItemTagConfig(key, Identifier.fromNamespaceAndPath("asset_editor", "textures/features/item/%s.png".formatted(key)));
    }

    private EnchantmentTreeData() {}
}
