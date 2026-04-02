package fr.hardel.asset_editor.client.compose.lib.data

import net.minecraft.resources.Identifier

object RecipeTreeData {

    enum class RecipeTemplateKind {
        CRAFTING, SMELTING, SMITHING, STONECUTTING
    }

    @JvmField
    val RECIPE_BLOCKS = listOf(
        RecipeBlockConfig(
            Identifier.fromNamespaceAndPath("minecraft", "barrier"),
            listOf(),
            true,
            RecipeTemplateKind.CRAFTING
        ),
        RecipeBlockConfig(
            Identifier.fromNamespaceAndPath("minecraft", "campfire"),
            listOf(Identifier.fromNamespaceAndPath("minecraft", "campfire_cooking")),
            false,
            RecipeTemplateKind.SMELTING
        ),
        RecipeBlockConfig(
            Identifier.fromNamespaceAndPath("minecraft", "furnace"),
            listOf(Identifier.fromNamespaceAndPath("minecraft", "smelting")),
            false,
            RecipeTemplateKind.SMELTING
        ),
        RecipeBlockConfig(
            Identifier.fromNamespaceAndPath("minecraft", "blast_furnace"),
            listOf(Identifier.fromNamespaceAndPath("minecraft", "blasting")),
            false,
            RecipeTemplateKind.SMELTING
        ),
        RecipeBlockConfig(
            Identifier.fromNamespaceAndPath("minecraft", "smoker"),
            listOf(Identifier.fromNamespaceAndPath("minecraft", "smoking")),
            false,
            RecipeTemplateKind.SMELTING
        ),
        RecipeBlockConfig(
            Identifier.fromNamespaceAndPath("minecraft", "stonecutter"),
            listOf(Identifier.fromNamespaceAndPath("minecraft", "stonecutting")),
            false,
            RecipeTemplateKind.STONECUTTING
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
            false,
            RecipeTemplateKind.CRAFTING,
            showRecipeTypesInAdvanced = true
        ),
        RecipeBlockConfig(
            Identifier.fromNamespaceAndPath("minecraft", "smithing_table"),
            listOf(
                Identifier.fromNamespaceAndPath("minecraft", "smithing_transform"),
                Identifier.fromNamespaceAndPath("minecraft", "smithing_trim")
            ),
            false,
            RecipeTemplateKind.SMITHING,
            showRecipeTypesInAdvanced = true
        )
    )

    class RecipeBlockConfig(
        val blockId: Identifier,
        val recipeTypes: List<Identifier>,
        val special: Boolean,
        val templateKind: RecipeTemplateKind,
        val showRecipeTypesInAdvanced: Boolean = false
    ) {
        fun icon(): Identifier =
            blockId.withPath("textures/studio/block/${blockId.path}.png")
    }

    @JvmStatic
    fun getBlockConfig(id: String): RecipeBlockConfig? =
        RECIPE_BLOCKS.firstOrNull { it.blockId.toString() == id }

    @JvmStatic
    fun getBlockByRecipeType(type: String): RecipeBlockConfig =
        RECIPE_BLOCKS.firstOrNull { block -> block.recipeTypes.any { it.toString() == type } }
            ?: RECIPE_BLOCKS.first()

    @JvmStatic
    fun getTemplateKind(type: String): RecipeTemplateKind =
        getBlockByRecipeType(type).templateKind

    @JvmStatic
    fun getAllBlockIds(includeSpecial: Boolean = true): List<String> =
        RECIPE_BLOCKS.filter { includeSpecial || !it.special }.map { it.blockId.toString() }

    @JvmStatic
    fun getAllRecipeTypes(): List<String> =
        RECIPE_BLOCKS.filter { !it.special }.flatMap { it.recipeTypes.map(Identifier::toString) }

    @JvmStatic
    fun getAdvancedRecipeTypes(): List<String> =
        RECIPE_BLOCKS
            .filter { it.showRecipeTypesInAdvanced }
            .flatMap { it.recipeTypes.map(Identifier::toString) }

    @JvmStatic
    fun isBlockId(id: String): Boolean =
        RECIPE_BLOCKS.any { it.blockId.toString() == id }

    @JvmStatic
    fun canBlockHandleRecipeType(id: String, type: String): Boolean =
        id == "minecraft:barrier" || (getBlockConfig(id)?.recipeTypes?.any { type.contains(it.toString()) } == true)
}
