package fr.hardel.asset_editor.client.compose.components.layout.editor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import fr.hardel.asset_editor.client.compose.components.page.changes.ChangesLayout
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import fr.hardel.asset_editor.client.compose.lib.rememberCurrentDestination
import fr.hardel.asset_editor.client.compose.lib.ChangesDestination
import fr.hardel.asset_editor.client.compose.lib.ConceptOverviewDestination
import fr.hardel.asset_editor.client.compose.lib.ConceptSimulationDestination
import fr.hardel.asset_editor.client.compose.lib.DebugDestination
import fr.hardel.asset_editor.client.compose.lib.ElementEditorDestination
import fr.hardel.asset_editor.client.compose.lib.NoPermissionDestination
import fr.hardel.asset_editor.client.compose.components.page.debug.DebugLayout
import fr.hardel.asset_editor.client.compose.lib.StudioUiRegistry

@Composable
fun ContentOutlet(context: StudioContext, modifier: Modifier = Modifier) {
    val destination = rememberCurrentDestination(context)

    // Compose-only: route outlet / switch de pages, équivalent conceptuel du <Outlet /> web.
    Box(modifier = modifier.fillMaxSize()) {
        when (destination) {
            is NoPermissionDestination -> NoPermissionPage()
            is DebugDestination -> DebugLayout(context)
            is ChangesDestination -> ChangesLayout(context, destination)
            is ConceptOverviewDestination -> {
                if (StudioUiRegistry.hasLayout(destination.conceptId)) {
                    StudioUiRegistry.renderLayout(context, destination.conceptId)
                } else NoPermissionPage()
            }

            is ConceptSimulationDestination -> {
                if (StudioUiRegistry.supportsSimulation(destination.conceptId)) {
                    StudioUiRegistry.renderLayout(context, destination.conceptId)
                } else NoPermissionPage()
            }

            is ElementEditorDestination -> {
                if (StudioUiRegistry.hasLayout(destination.conceptId)) {
                    StudioUiRegistry.renderLayout(context, destination.conceptId)
                } else NoPermissionPage()
            }
        }
    }
}
