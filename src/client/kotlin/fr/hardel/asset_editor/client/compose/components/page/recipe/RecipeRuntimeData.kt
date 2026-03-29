package fr.hardel.asset_editor.client.compose.components.page.recipe

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import fr.hardel.asset_editor.client.AssetEditorClient
import fr.hardel.asset_editor.client.compose.components.page.recipe.model.RecipeRuntimeEntry
import fr.hardel.asset_editor.client.compose.components.page.recipe.model.loadRuntimeRecipeEntries
import fr.hardel.asset_editor.client.compose.components.page.recipe.model.loadWorkspaceRecipeEntries
import fr.hardel.asset_editor.client.compose.components.page.recipe.model.toRuntimeEntry
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import fr.hardel.asset_editor.client.compose.lib.rememberRegistryEntries
import fr.hardel.asset_editor.network.recipe.RecipeCatalogSyncPayload
import fr.hardel.asset_editor.store.CustomFields
import fr.hardel.asset_editor.store.ElementEntry
import net.minecraft.client.Minecraft
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.world.item.crafting.Recipe
import java.util.LinkedHashMap

@Composable
fun rememberRecipeEntries(context: StudioContext): List<RecipeRuntimeEntry> {
    val workspaceEntries = rememberRegistryEntries(context, Registries.RECIPE)
    val recipeCatalog = AssetEditorClient.catalogMemory().getCatalog("minecraft:recipe", RecipeCatalogSyncPayload.Entry::class.java)
    val connection = Minecraft.getInstance().connection
    val runtimeEntries = rememberRuntimeRecipeEntries(context)
    return remember(workspaceEntries, recipeCatalog, runtimeEntries, connection, context.sessionMemory().worldSessionKey()) {
        val workspaceRuntimeEntries = loadWorkspaceRecipeEntries(workspaceEntries, connection?.registryAccess())
        val baseEntries = runtimeEntries.ifEmpty { recipeCatalog.map(RecipeCatalogSyncPayload.Entry::toRuntimeEntry) }
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

@Composable
private fun rememberRuntimeRecipeEntries(context: StudioContext): List<RecipeRuntimeEntry> {
    val minecraft = Minecraft.getInstance()
    val connection = minecraft.connection
    return remember(connection, context.sessionMemory().worldSessionKey()) {
        loadRuntimeRecipeEntries(minecraft)
    }
}

fun resolveRuntimeRecipeElement(elementId: String?): ElementEntry<Recipe<*>>? {
    val parsed = elementId?.let(Identifier::tryParse) ?: return null
    val minecraft = Minecraft.getInstance()
    val registry = minecraft.connection?.registryAccess()?.lookup(Registries.RECIPE)?.orElse(null) ?: return null
    val holder = registry.listElements().toList().firstOrNull { reference -> reference.key().identifier() == parsed } ?: return null
    return ElementEntry(parsed, holder.value(), emptySet(), CustomFields.EMPTY)
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
