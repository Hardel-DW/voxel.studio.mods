package fr.hardel.asset_editor.client.compose.lib.assets

import fr.hardel.asset_editor.client.compose.lib.ItemAtlasGenerator
import fr.hardel.asset_editor.client.compose.lib.ConceptChangesDestination
import fr.hardel.asset_editor.client.compose.lib.ConceptOverviewDestination
import fr.hardel.asset_editor.client.compose.lib.ElementEditorDestination
import fr.hardel.asset_editor.client.compose.lib.StudioDestination
import net.minecraft.client.Minecraft
import net.minecraft.resources.Identifier
import fr.hardel.asset_editor.studio.StudioRegistryResolver
import fr.hardel.asset_editor.studio.StudioUiRegistry
import java.util.concurrent.CompletableFuture

class DefaultStudioPrefetcher(
    private val assetCache: StudioAssetCache
) : StudioPrefetcher {

    override fun prefetch(destination: StudioDestination) {
        CompletableFuture.runAsync {
            when (destination) {
                is ConceptOverviewDestination -> prefetchConcept(destination.conceptId)
                is ConceptChangesDestination -> prefetchConcept(destination.conceptId)
                is ElementEditorDestination -> {
                    prefetchConcept(destination.conceptId)
                    if (StudioUiRegistry.shouldPrefetchAtlas(destination.conceptId)) {
                        ItemAtlasGenerator.getAtlasImage()
                    }
                }

                else -> Unit
            }
        }
    }

    private fun prefetchConcept(conceptId: Identifier) {
        val registries = Minecraft.getInstance().connection?.registryAccess() ?: return
        assetCache.bitmap(StudioRegistryResolver.icon(registries, conceptId))
        if (StudioUiRegistry.shouldPrefetchAtlas(conceptId)) {
            ItemAtlasGenerator.getAtlasImage()
        }
    }
}
