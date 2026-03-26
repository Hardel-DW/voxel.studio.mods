package fr.hardel.asset_editor.client.compose.lib.data

import fr.hardel.asset_editor.tag.TagSeed
import net.minecraft.resources.Identifier

object EnchantmentTreeData {

    @JvmField
    val ITEM_TAGS = listOf(
        ItemTagConfig(id("voxel", "enchantable/swords"), seed("#minecraft:swords")),
        ItemTagConfig(id("minecraft", "enchantable/trident")),
        ItemTagConfig(id("minecraft", "enchantable/mace")),
        ItemTagConfig(id("minecraft", "enchantable/bow")),
        ItemTagConfig(id("minecraft", "enchantable/crossbow")),
        ItemTagConfig(
            id("voxel", "enchantable/range"),
            seed("#minecraft:enchantable/crossbow", "#minecraft:enchantable/bow")
        ),
        ItemTagConfig(id("minecraft", "enchantable/fishing")),
        ItemTagConfig(id("voxel", "enchantable/shield"), seed("minecraft:shield")),
        ItemTagConfig(id("minecraft", "enchantable/weapon")),
        ItemTagConfig(
            id("voxel", "enchantable/melee"),
            seed("#minecraft:enchantable/weapon", "#minecraft:enchantable/trident")
        ),
        ItemTagConfig(id("minecraft", "enchantable/head_armor")),
        ItemTagConfig(id("minecraft", "enchantable/chest_armor")),
        ItemTagConfig(id("minecraft", "enchantable/leg_armor")),
        ItemTagConfig(id("minecraft", "enchantable/foot_armor")),
        ItemTagConfig(id("voxel", "enchantable/elytra"), seed("minecraft:elytra")),
        ItemTagConfig(id("minecraft", "enchantable/armor")),
        ItemTagConfig(id("minecraft", "enchantable/equippable")),
        ItemTagConfig(id("voxel", "enchantable/axes"), seed("#minecraft:axes")),
        ItemTagConfig(id("voxel", "enchantable/shovels"), seed("#minecraft:shovels")),
        ItemTagConfig(id("voxel", "enchantable/hoes"), seed("#minecraft:hoes")),
        ItemTagConfig(id("voxel", "enchantable/pickaxes"), seed("#minecraft:pickaxes")),
        ItemTagConfig(id("minecraft", "enchantable/durability")),
        ItemTagConfig(id("minecraft", "enchantable/mining_loot"))
    )

    private fun id(namespace: String, path: String) = Identifier.fromNamespaceAndPath(namespace, path)
    private fun seed(vararg values: String) = TagSeed.fromValueLiterals(values.toList())

    data class ItemTagConfig(val tagId: Identifier, val seed: TagSeed? = null) {
        fun key(): String = tagId.path
        fun icon(): Identifier = tagId.withPath("textures/studio/item/${tagId.path}.png")
    }
}
