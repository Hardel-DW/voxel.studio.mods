package fr.hardel.asset_editor.client.compose.lib.data

import net.minecraft.resources.Identifier

data class ExclusiveSetGroup(val tagId: Identifier) {

    fun id(): String = tagId.path

    fun image(): Identifier =
        tagId.withPath("textures/studio/enchantment/${tagId.path}.png")

    fun value(): String = "#$tagId"

    companion object {
        @JvmField
        val ALL = listOf(
            ExclusiveSetGroup(Identifier.fromNamespaceAndPath("minecraft", "exclusive_set/armor")),
            ExclusiveSetGroup(Identifier.fromNamespaceAndPath("minecraft", "exclusive_set/bow")),
            ExclusiveSetGroup(Identifier.fromNamespaceAndPath("minecraft", "exclusive_set/crossbow")),
            ExclusiveSetGroup(Identifier.fromNamespaceAndPath("minecraft", "exclusive_set/damage")),
            ExclusiveSetGroup(Identifier.fromNamespaceAndPath("minecraft", "exclusive_set/riptide")),
            ExclusiveSetGroup(Identifier.fromNamespaceAndPath("minecraft", "exclusive_set/mining")),
            ExclusiveSetGroup(Identifier.fromNamespaceAndPath("minecraft", "exclusive_set/boots"))
        )
    }
}
