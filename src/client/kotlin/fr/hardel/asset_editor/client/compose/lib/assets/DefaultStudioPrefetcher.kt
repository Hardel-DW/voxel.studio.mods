package fr.hardel.asset_editor.client.compose.lib.assets

import fr.hardel.asset_editor.client.compose.lib.ItemAtlasGenerator
import fr.hardel.asset_editor.client.compose.lib.data.StudioConcept
import fr.hardel.asset_editor.client.compose.lib.data.StudioRenderRegistry
import fr.hardel.asset_editor.client.navigation.ConceptChangesDestination
import fr.hardel.asset_editor.client.navigation.ConceptOverviewDestination
import fr.hardel.asset_editor.client.navigation.ElementEditorDestination
import fr.hardel.asset_editor.client.navigation.StudioDestination
import java.util.concurrent.CompletableFuture

class DefaultStudioPrefetcher(
    private val assetCache: StudioAssetCache
) : StudioPrefetcher {

    override fun prefetch(destination: StudioDestination) {
        CompletableFuture.runAsync {
            when (destination) {
                is ConceptOverviewDestination -> prefetchConcept(destination.concept)
                is ConceptChangesDestination -> prefetchConcept(destination.concept)
                is ElementEditorDestination -> {
                    prefetchConcept(destination.concept)
                    if (StudioRenderRegistry.shouldPrefetchAtlas(destination.concept)) {
                        ItemAtlasGenerator.getAtlasImage()
                    }
                }

                else -> Unit
            }
        }
    }

    private fun prefetchConcept(concept: StudioConcept) {
        assetCache.bitmap(concept.icon)
        if (StudioRenderRegistry.shouldPrefetchAtlas(concept)) {
            ItemAtlasGenerator.getAtlasImage()
        }
    }
}
