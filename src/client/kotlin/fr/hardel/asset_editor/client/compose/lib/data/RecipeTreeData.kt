package fr.hardel.asset_editor.client.compose.lib.data

import fr.hardel.asset_editor.client.AssetEditorClient
import fr.hardel.asset_editor.studio.RecipeEntryDefinition
import net.minecraft.resources.Identifier

object RecipeTreeData {

    val RECIPE_ENTRIES: List<RecipeEntryConfig>
        get() = AssetEditorClient.studioConfigMemory().snapshot().recipeEntries().map { def -> toConfig(def) }

    private fun toConfig(def: RecipeEntryDefinition): RecipeEntryConfig {
        val translationKey = "block.${def.entryId().getNamespace()}.${def.entryId().getPath()}"
        return RecipeEntryConfig(
            entryId = def.entryId(),
            assetId = def.entryId(),
            translationKey = translationKey,
            recipeTypes = def.recipeTypes(),
            special = def.special(),
            templateKind = def.templateKind(),
            showRecipeTypesInAdvanced = def.showRecipeTypesInAdvanced()
        )
    }

    class RecipeEntryConfig(
        val entryId: Identifier,
        val assetId: Identifier,
        val translationKey: String,
        val recipeTypes: List<Identifier>,
        val special: Boolean,
        val templateKind: Identifier,
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
    fun getTemplateKind(type: String): Identifier =
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
