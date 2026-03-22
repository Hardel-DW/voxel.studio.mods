package fr.hardel.asset_editor.client.compose.lib.data

import net.minecraft.resources.Identifier

object RecipeTreeData {

    @JvmField
    val RECIPE_BLOCKS = listOf(
        RecipeBlockConfig(Identifier.fromNamespaceAndPath("minecraft", "barrier"), listOf(), true),
        RecipeBlockConfig(
            Identifier.fromNamespaceAndPath("minecraft", "campfire"),
            listOf(Identifier.fromNamespaceAndPath("minecraft", "campfire_cooking")),
            false
        ),
        RecipeBlockConfig(
            Identifier.fromNamespaceAndPath("minecraft", "furnace"),
            listOf(Identifier.fromNamespaceAndPath("minecraft", "smelting")),
            false
        ),
        RecipeBlockConfig(
            Identifier.fromNamespaceAndPath("minecraft", "blast_furnace"),
            listOf(Identifier.fromNamespaceAndPath("minecraft", "blasting")),
            false
        ),
        RecipeBlockConfig(
            Identifier.fromNamespaceAndPath("minecraft", "smoker"),
            listOf(Identifier.fromNamespaceAndPath("minecraft", "smoking")),
            false
        ),
        RecipeBlockConfig(
            Identifier.fromNamespaceAndPath("minecraft", "stonecutter"),
            listOf(Identifier.fromNamespaceAndPath("minecraft", "stonecutting")),
            false
        ),
        RecipeBlockConfig(
            Identifier.fromNamespaceAndPath("minecraft", "crafting_table"),
            listOf(
                Identifier.fromNamespaceAndPath("minecraft", "crafting_shapeless"),
                Identifier.fromNamespaceAndPath("minecraft", "crafting_shaped"),
                Identifier.fromNamespaceAndPath("minecraft", "crafting_decorated_pot"),
                Identifier.fromNamespaceAndPath("minecraft", "crafting_special_armordye"),
                Identifier.fromNamespaceAndPath("minecraft", "crafting_special_bannerduplicate"),
                Identifier.fromNamespaceAndPath("minecraft", "crafting_special_bookcloning"),
                Identifier.fromNamespaceAndPath("minecraft", "crafting_special_firework_rocket"),
                Identifier.fromNamespaceAndPath("minecraft", "crafting_special_firework_star"),
                Identifier.fromNamespaceAndPath("minecraft", "crafting_special_firework_star_fade"),
                Identifier.fromNamespaceAndPath("minecraft", "crafting_special_mapcloning"),
                Identifier.fromNamespaceAndPath("minecraft", "crafting_special_mapextending"),
                Identifier.fromNamespaceAndPath("minecraft", "crafting_special_repairitem"),
                Identifier.fromNamespaceAndPath("minecraft", "crafting_special_shielddecoration"),
                Identifier.fromNamespaceAndPath("minecraft", "crafting_special_tippedarrow"),
                Identifier.fromNamespaceAndPath("minecraft", "crafting_transmute")
            ),
            false
        ),
        RecipeBlockConfig(
            Identifier.fromNamespaceAndPath("minecraft", "smithing_table"),
            listOf(
                Identifier.fromNamespaceAndPath("minecraft", "smithing_transform"),
                Identifier.fromNamespaceAndPath("minecraft", "smithing_trim")
            ),
            false
        )
    )

    data class RecipeBlockConfig(
        val blockId: Identifier,
        val recipeTypes: List<Identifier>,
        val special: Boolean
    ) {
        fun icon(): Identifier =
            blockId.withPath("textures/studio/block/${blockId.path}.png")
    }
}
