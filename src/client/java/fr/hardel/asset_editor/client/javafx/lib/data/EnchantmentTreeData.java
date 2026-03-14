package fr.hardel.asset_editor.client.javafx.lib.data;

import net.minecraft.resources.Identifier;

import java.util.List;

public final class EnchantmentTreeData {

    public static final List<ItemTagConfig> ITEM_TAGS = List.of(
            new ItemTagConfig(Identifier.fromNamespaceAndPath("minecraft", "enchantable/sword")),
            new ItemTagConfig(Identifier.fromNamespaceAndPath("minecraft", "enchantable/trident")),
            new ItemTagConfig(Identifier.fromNamespaceAndPath("minecraft", "enchantable/mace")),
            new ItemTagConfig(Identifier.fromNamespaceAndPath("minecraft", "enchantable/bow")),
            new ItemTagConfig(Identifier.fromNamespaceAndPath("minecraft", "enchantable/crossbow")),
            new ItemTagConfig(Identifier.fromNamespaceAndPath("minecraft", "enchantable/range")),
            new ItemTagConfig(Identifier.fromNamespaceAndPath("minecraft", "enchantable/fishing")),
            new ItemTagConfig(Identifier.fromNamespaceAndPath("minecraft", "enchantable/shield")),
            new ItemTagConfig(Identifier.fromNamespaceAndPath("minecraft", "enchantable/weapon")),
            new ItemTagConfig(Identifier.fromNamespaceAndPath("minecraft", "enchantable/melee")),
            new ItemTagConfig(Identifier.fromNamespaceAndPath("minecraft", "enchantable/head_armor")),
            new ItemTagConfig(Identifier.fromNamespaceAndPath("minecraft", "enchantable/chest_armor")),
            new ItemTagConfig(Identifier.fromNamespaceAndPath("minecraft", "enchantable/leg_armor")),
            new ItemTagConfig(Identifier.fromNamespaceAndPath("minecraft", "enchantable/foot_armor")),
            new ItemTagConfig(Identifier.fromNamespaceAndPath("minecraft", "enchantable/elytra")),
            new ItemTagConfig(Identifier.fromNamespaceAndPath("minecraft", "enchantable/armor")),
            new ItemTagConfig(Identifier.fromNamespaceAndPath("minecraft", "enchantable/equippable")),
            new ItemTagConfig(Identifier.fromNamespaceAndPath("minecraft", "enchantable/axes")),
            new ItemTagConfig(Identifier.fromNamespaceAndPath("minecraft", "enchantable/shovels")),
            new ItemTagConfig(Identifier.fromNamespaceAndPath("minecraft", "enchantable/hoes")),
            new ItemTagConfig(Identifier.fromNamespaceAndPath("minecraft", "enchantable/pickaxes")),
            new ItemTagConfig(Identifier.fromNamespaceAndPath("minecraft", "enchantable/durability")),
            new ItemTagConfig(Identifier.fromNamespaceAndPath("minecraft", "enchantable/mining_loot")));

    public record ItemTagConfig(Identifier tagId) {

        public String key() {
            return tagId.getPath();
        }

        public Identifier icon() {
            return tagId.withPath("textures/tags/item/" + tagId.getPath() + ".png");
        }
    }

    private EnchantmentTreeData() {
    }
}
