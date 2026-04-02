package fr.hardel.asset_editor.client.compose.lib.data

import net.minecraft.resources.Identifier

object RecipeTreeData {

    enum class RecipeTemplateKind {
        CRAFTING, SMELTING, SMITHING, STONECUTTING
    }

    @JvmField
    val RECIPE_ENTRIES = listOf(
        RecipeEntryConfig(
            entryId = Identifier.fromNamespaceAndPath("minecraft", "barrier"),
            assetId = Identifier.fromNamespaceAndPath("minecraft", "barrier"),
            translationKey = "recipe:block.all",
            listOf(),
            true,
            RecipeTemplateKind.CRAFTING
        ),
        RecipeEntryConfig(
            entryId = Identifier.fromNamespaceAndPath("minecraft", "campfire"),
            assetId = Identifier.fromNamespaceAndPath("minecraft", "campfire"),
            translationKey = "block.minecraft.campfire",
            listOf(Identifier.fromNamespaceAndPath("minecraft", "campfire_cooking")),
            false,
            RecipeTemplateKind.SMELTING
        ),
        RecipeEntryConfig(
            entryId = Identifier.fromNamespaceAndPath("minecraft", "furnace"),
            assetId = Identifier.fromNamespaceAndPath("minecraft", "furnace"),
            translationKey = "block.minecraft.furnace",
            listOf(Identifier.fromNamespaceAndPath("minecraft", "smelting")),
            false,
            RecipeTemplateKind.SMELTING
        ),
        RecipeEntryConfig(
            entryId = Identifier.fromNamespaceAndPath("minecraft", "blast_furnace"),
            assetId = Identifier.fromNamespaceAndPath("minecraft", "blast_furnace"),
            translationKey = "block.minecraft.blast_furnace",
            listOf(Identifier.fromNamespaceAndPath("minecraft", "blasting")),
            false,
            RecipeTemplateKind.SMELTING
        ),
        RecipeEntryConfig(
            entryId = Identifier.fromNamespaceAndPath("minecraft", "smoker"),
            assetId = Identifier.fromNamespaceAndPath("minecraft", "smoker"),
            translationKey = "block.minecraft.smoker",
            listOf(Identifier.fromNamespaceAndPath("minecraft", "smoking")),
            false,
            RecipeTemplateKind.SMELTING
        ),
        RecipeEntryConfig(
            entryId = Identifier.fromNamespaceAndPath("minecraft", "stonecutter"),
            assetId = Identifier.fromNamespaceAndPath("minecraft", "stonecutter"),
            translationKey = "block.minecraft.stonecutter",
            listOf(Identifier.fromNamespaceAndPath("minecraft", "stonecutting")),
            false,
            RecipeTemplateKind.STONECUTTING
        ),
        RecipeEntryConfig(
            entryId = Identifier.fromNamespaceAndPath("minecraft", "crafting_table"),
            assetId = Identifier.fromNamespaceAndPath("minecraft", "crafting_table"),
            translationKey = "block.minecraft.crafting_table",
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
        RecipeEntryConfig(
            entryId = Identifier.fromNamespaceAndPath("minecraft", "smithing_table"),
            assetId = Identifier.fromNamespaceAndPath("minecraft", "smithing_table"),
            translationKey = "block.minecraft.smithing_table",
            listOf(
                Identifier.fromNamespaceAndPath("minecraft", "smithing_transform"),
                Identifier.fromNamespaceAndPath("minecraft", "smithing_trim")
            ),
            false,
            RecipeTemplateKind.SMITHING,
            showRecipeTypesInAdvanced = true
        )
    )

    class RecipeEntryConfig(
        val entryId: Identifier,
        val assetId: Identifier,
        val translationKey: String,
        val recipeTypes: List<Identifier>,
        val special: Boolean,
        val templateKind: RecipeTemplateKind,
        val showRecipeTypesInAdvanced: Boolean = false
    ) {
        fun folderIcon(): Identifier =
            assetId.withPath("textures/studio/block/${assetId.path}.png")
    }

    @JvmStatic
    fun getEntryConfig(id: String): RecipeEntryConfig? =
        RECIPE_ENTRIES.firstOrNull { it.entryId.toString() == id }

    @JvmStatic
    fun getEntryByRecipeType(type: String): RecipeEntryConfig =
        RECIPE_ENTRIES.firstOrNull { entry -> entry.recipeTypes.any { it.toString() == type } }
            ?: RECIPE_ENTRIES.first()

    @JvmStatic
    fun getTemplateKind(type: String): RecipeTemplateKind =
        getEntryByRecipeType(type).templateKind

    @JvmStatic
    fun getAllEntryIds(includeSpecial: Boolean = true): List<String> =
        RECIPE_ENTRIES.filter { includeSpecial || !it.special }.map { it.entryId.toString() }

    @JvmStatic
    fun getAllRecipeTypes(): List<String> =
        RECIPE_ENTRIES.filter { !it.special }.flatMap { it.recipeTypes.map(Identifier::toString) }

    @JvmStatic
    fun getAdvancedRecipeTypes(): List<String> =
        RECIPE_ENTRIES
            .filter { it.showRecipeTypesInAdvanced }
            .flatMap { it.recipeTypes.map(Identifier::toString) }

    @JvmStatic
    fun isEntryId(id: String): Boolean =
        RECIPE_ENTRIES.any { it.entryId.toString() == id }

    @JvmStatic
    fun canEntryHandleRecipeType(id: String, type: String): Boolean =
        id == "minecraft:barrier" || (getEntryConfig(id)?.recipeTypes?.any { type.contains(it.toString()) } == true)
}
