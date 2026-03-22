package fr.hardel.asset_editor.client.compose.lib.data

import net.minecraft.resources.Identifier

object SlotConfigs {

    data class SlotConfig(val id: String, val slots: List<String>) {
        fun image(): Identifier = slotImage(id)
    }

    @JvmField
    val ALL = listOf(
        SlotConfig("mainhand", listOf("mainhand", "any", "hand", "all")),
        SlotConfig("offhand", listOf("offhand", "any", "hand", "all")),
        SlotConfig("body", listOf("body", "any", "all")),
        SlotConfig("saddle", listOf("saddle", "any", "all")),
        SlotConfig("head", listOf("head", "any", "armor", "all")),
        SlotConfig("chest", listOf("chest", "any", "armor", "all")),
        SlotConfig("legs", listOf("legs", "any", "armor", "all")),
        SlotConfig("feet", listOf("feet", "any", "armor", "all"))
    )

    @JvmField
    val GROUPS = listOf(
        listOf("mainhand", "offhand"),
        listOf("body", "saddle"),
        listOf("head", "chest", "legs", "feet")
    )

    @JvmField
    val BY_ID = ALL.associateBy(SlotConfig::id)

    private fun slotImage(name: String): Identifier =
        Identifier.fromNamespaceAndPath("minecraft", "textures/studio/slots/$name.png")
}
