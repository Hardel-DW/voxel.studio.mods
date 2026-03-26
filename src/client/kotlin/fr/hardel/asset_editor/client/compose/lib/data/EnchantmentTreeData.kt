package fr.hardel.asset_editor.client.compose.lib.data

import net.minecraft.resources.Identifier

object EnchantmentTreeData {

    @JvmField
    val ITEM_TAGS = listOf(
        ItemTagConfig(Identifier.fromNamespaceAndPath("minecraft", "enchantable/sword")),
        ItemTagConfig(Identifier.fromNamespaceAndPath("minecraft", "enchantable/trident")),
        ItemTagConfig(Identifier.fromNamespaceAndPath("minecraft", "enchantable/mace")),
        ItemTagConfig(Identifier.fromNamespaceAndPath("minecraft", "enchantable/bow")),
        ItemTagConfig(Identifier.fromNamespaceAndPath("minecraft", "enchantable/crossbow")),
        ItemTagConfig(Identifier.fromNamespaceAndPath("voxel", "enchantable/range")),
        ItemTagConfig(Identifier.fromNamespaceAndPath("minecraft", "enchantable/fishing")),
        ItemTagConfig(Identifier.fromNamespaceAndPath("voxel", "enchantable/shield")),
        ItemTagConfig(Identifier.fromNamespaceAndPath("minecraft", "enchantable/weapon")),
        ItemTagConfig(Identifier.fromNamespaceAndPath("voxel", "enchantable/melee")),
        ItemTagConfig(Identifier.fromNamespaceAndPath("minecraft", "enchantable/head_armor")),
        ItemTagConfig(Identifier.fromNamespaceAndPath("minecraft", "enchantable/chest_armor")),
        ItemTagConfig(Identifier.fromNamespaceAndPath("minecraft", "enchantable/leg_armor")),
        ItemTagConfig(Identifier.fromNamespaceAndPath("minecraft", "enchantable/foot_armor")),
        ItemTagConfig(Identifier.fromNamespaceAndPath("voxel", "enchantable/elytra")),
        ItemTagConfig(Identifier.fromNamespaceAndPath("minecraft", "enchantable/armor")),
        ItemTagConfig(Identifier.fromNamespaceAndPath("minecraft", "enchantable/equippable")),
        ItemTagConfig(Identifier.fromNamespaceAndPath("voxel", "enchantable/axes")),
        ItemTagConfig(Identifier.fromNamespaceAndPath("voxel", "enchantable/shovels")),
        ItemTagConfig(Identifier.fromNamespaceAndPath("voxel", "enchantable/hoes")),
        ItemTagConfig(Identifier.fromNamespaceAndPath("voxel", "enchantable/pickaxes")),
        ItemTagConfig(Identifier.fromNamespaceAndPath("minecraft", "enchantable/durability")),
        ItemTagConfig(Identifier.fromNamespaceAndPath("minecraft", "enchantable/mining_loot"))
    )

    data class ItemTagConfig(val tagId: Identifier) {
        fun key(): String = tagId.path

        fun icon(): Identifier =
            tagId.withPath("textures/studio/item/${tagId.path}.png")
    }
}
