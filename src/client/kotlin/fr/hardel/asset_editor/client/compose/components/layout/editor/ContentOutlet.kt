package fr.hardel.asset_editor.client.compose.components.layout.editor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import fr.hardel.asset_editor.client.compose.components.page.changes.ChangesLayout
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import fr.hardel.asset_editor.client.compose.lib.data.StudioRenderRegistry
import fr.hardel.asset_editor.client.compose.lib.rememberCurrentDestination
import fr.hardel.asset_editor.client.navigation.ConceptChangesDestination
import fr.hardel.asset_editor.client.navigation.ConceptOverviewDestination
import fr.hardel.asset_editor.client.navigation.ConceptSimulationDestination
import fr.hardel.asset_editor.client.navigation.DebugDestination
import fr.hardel.asset_editor.client.navigation.ElementEditorDestination
import fr.hardel.asset_editor.client.navigation.NoPermissionDestination
import fr.hardel.asset_editor.client.compose.routes.debug.DebugLayout

@Composable
fun ContentOutlet(context: StudioContext, modifier: Modifier = Modifier) {
    val destination = rememberCurrentDestination(context)

    // Compose-only: route outlet / switch de pages, équivalent conceptuel du <Outlet /> web.
    Box(modifier = modifier.fillMaxSize()) {
        when (destination) {
            is NoPermissionDestination -> NoPermissionPage()
            is DebugDestination -> DebugLayout(context)
            is ConceptChangesDestination -> ChangesLayout()
            is ConceptOverviewDestination -> {
                if (StudioRenderRegistry.hasLayout(destination.concept)) {
                    StudioRenderRegistry.RenderConceptLayout(context, destination.concept)
                } else NoPermissionPage()
            }

            is ConceptSimulationDestination -> {
                if (StudioRenderRegistry.supportsSimulation(destination.concept)) {
                    StudioRenderRegistry.RenderConceptLayout(context, destination.concept)
                } else NoPermissionPage()
            }

            is ElementEditorDestination -> {
                if (StudioRenderRegistry.hasLayout(destination.concept)) {
                    StudioRenderRegistry.RenderConceptLayout(context, destination.concept)
                } else NoPermissionPage()
            }
        }
    }
}
