package fr.hardel.asset_editor.client.compose.components.page.recipe.editor.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import fr.hardel.asset_editor.client.compose.lib.rememberRegistryEntries
import fr.hardel.asset_editor.client.memory.session.StudioDataSlots
import fr.hardel.asset_editor.network.recipe.RecipeCatalogEntry
import net.minecraft.client.Minecraft
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import java.util.LinkedHashMap

@Composable
fun rememberRecipeEntries(context: StudioContext): List<RecipeRuntimeEntry> {
    val workspaceEntries = rememberRegistryEntries(context, Registries.RECIPE)
    val recipeCatalog = StudioDataSlots.RECIPE_CATALOG.memory().snapshot()
    val connection = Minecraft.getInstance().connection
    return remember(workspaceEntries, recipeCatalog, connection, context.sessionMemory().worldSessionKey()) {
        val workspaceRuntimeEntries = loadWorkspaceRecipeEntries(workspaceEntries, connection?.registryAccess())
        val baseEntries = recipeCatalog.map(RecipeCatalogEntry::toRuntimeEntry)
        mergeRecipeEntries(baseEntries, workspaceRuntimeEntries)
    }
}

@Composable
fun rememberRecipeEntry(context: StudioContext, elementId: String?): RecipeRuntimeEntry? {
    val entries = rememberRecipeEntries(context)
    return remember(entries, elementId) {
        val parsed = elementId?.let(Identifier::tryParse) ?: return@remember null
        entries.firstOrNull { it.id == parsed }
    }
}

private fun mergeRecipeEntries(
    baseEntries: List<RecipeRuntimeEntry>,
    workspaceEntries: List<RecipeRuntimeEntry>
): List<RecipeRuntimeEntry> {
    val merged = LinkedHashMap<Identifier, RecipeRuntimeEntry>(baseEntries.size + workspaceEntries.size)
    baseEntries.forEach { entry -> merged[entry.id] = entry }
    workspaceEntries.forEach { entry -> merged[entry.id] = entry }
    return merged.values.toList()
}
