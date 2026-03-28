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
import net.minecraft.client.Minecraft
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier

@Composable
fun rememberRecipeEntries(context: StudioContext): List<RecipeRuntimeEntry> {
    val workspaceEntries = rememberRegistryEntries(context, Registries.RECIPE)
    val recipeCatalog = AssetEditorClient.catalogMemory().getCatalog("minecraft:recipe", RecipeCatalogSyncPayload.Entry::class.java)
    val connection = Minecraft.getInstance().connection
    val runtimeEntries = rememberRuntimeRecipeEntries(context)
    return remember(workspaceEntries, recipeCatalog, runtimeEntries, connection, context.sessionMemory().worldSessionKey()) {
        loadWorkspaceRecipeEntries(workspaceEntries, connection?.registryAccess())
            .ifEmpty { runtimeEntries }
            .ifEmpty { recipeCatalog.map(RecipeCatalogSyncPayload.Entry::toRuntimeEntry) }
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
    val integratedServer = minecraft.singleplayerServer
    return remember(connection, integratedServer, context.sessionMemory().worldSessionKey()) {
        loadRuntimeRecipeEntries(minecraft)
    }
}
