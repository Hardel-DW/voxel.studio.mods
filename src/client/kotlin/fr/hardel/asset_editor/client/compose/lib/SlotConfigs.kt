package fr.hardel.asset_editor.client.compose.lib

import fr.hardel.asset_editor.AssetEditor
import net.minecraft.resources.Identifier

object SlotConfigs {

    val MAPPINGS: Map<String, List<String>> = linkedMapOf(
        "any" to listOf("mainhand", "offhand", "head", "chest", "legs", "feet", "body", "saddle"),
        "armor" to listOf("head", "chest", "legs", "feet"),
        "hand" to listOf("mainhand", "offhand"),
        "mainhand" to listOf("mainhand"),
        "offhand" to listOf("offhand"),
        "head" to listOf("head"),
        "chest" to listOf("chest"),
        "legs" to listOf("legs"),
        "feet" to listOf("feet"),
        "body" to listOf("body"),
        "saddle" to listOf("saddle")
    )

    val PHYSICAL: List<String> = MAPPINGS.entries
        .filter { (_, children) -> children.size == 1 }
        .map { (key, _) -> key }

    val GROUPS: List<List<String>> = listOf(
        listOf("mainhand", "offhand"),
        listOf("body", "saddle"),
        listOf("head", "chest", "legs", "feet")
    )

    fun expandsTo(group: String, physicalSlot: String): Boolean =
        MAPPINGS[group]?.contains(physicalSlot) == true

    fun slotImage(name: String): Identifier =
        Identifier.withDefaultNamespace("textures/studio/slots/$name.png")
}
