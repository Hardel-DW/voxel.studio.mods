package fr.hardel.asset_editor.client.compose.components.layout.editor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import fr.hardel.asset_editor.client.compose.components.page.changes.ChangesLayout
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import fr.hardel.asset_editor.client.compose.lib.rememberCurrentDestination
import fr.hardel.asset_editor.client.navigation.ConceptChangesDestination
import fr.hardel.asset_editor.client.navigation.ConceptOverviewDestination
import fr.hardel.asset_editor.client.navigation.ConceptSimulationDestination
import fr.hardel.asset_editor.client.navigation.DebugDestination
import fr.hardel.asset_editor.client.navigation.ElementEditorDestination
import fr.hardel.asset_editor.client.navigation.NoPermissionDestination
import fr.hardel.asset_editor.client.compose.routes.debug.DebugLayout
import fr.hardel.asset_editor.studio.StudioUiRegistry

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
