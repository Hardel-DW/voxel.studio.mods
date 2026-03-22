package fr.hardel.asset_editor.client.compose.routes.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.VoxelColors
import fr.hardel.asset_editor.client.compose.VoxelTypography
import fr.hardel.asset_editor.client.compose.components.ui.KeyValueGrid
import fr.hardel.asset_editor.client.compose.components.ui.Section
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import fr.hardel.asset_editor.client.compose.lib.rememberCurrentDestination
import fr.hardel.asset_editor.client.compose.lib.rememberCurrentElementDestination
import fr.hardel.asset_editor.client.compose.lib.rememberNavigationSnapshot
import fr.hardel.asset_editor.client.compose.lib.rememberSelectedPack
import fr.hardel.asset_editor.client.compose.lib.rememberUiSnapshot
import net.minecraft.client.resources.language.I18n

@Composable
fun DebugWorkspacePage(context: StudioContext) {
    val destination = rememberCurrentDestination(context)
    val currentEditor = rememberCurrentElementDestination(context)
    val selectedPack = rememberSelectedPack(context)
    val navigationSnapshot = rememberNavigationSnapshot(context)
    val uiSnapshot = rememberUiSnapshot(context)
    val registrySnapshot = context.elementStore().entryCountsSnapshot()

    val sections = linkedMapOf<String, Any>(
        I18n.get("debug:workspace.section.overview") to OverviewSnapshot(
            destination = destination.toString(),
            selectedPack = selectedPack?.packId() ?: "none",
            currentElement = currentEditor?.elementId ?: "none",
            pendingActions = context.workspaceState().snapshot().pendingActionCount(),
            openTabs = navigationSnapshot.tabs.size,
            registries = registrySnapshot.size,
            totalEntries = registrySnapshot.values.sum()
        ),
        I18n.get("debug:workspace.section.session") to context.sessionState().snapshot(),
        I18n.get("debug:workspace.section.ui") to uiSnapshot,
        "Navigation" to navigationSnapshot,
        "Pack" to context.packState().snapshot(),
        I18n.get("debug:workspace.section.registries") to RegistriesSnapshot(registrySnapshot),
        I18n.get("debug:workspace.section.issues") to context.workspaceState().issueState().snapshot()
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp, vertical = 24.dp)
    ) {
        sections.forEach { (title, snapshot) ->
            Section(title) {
                if (snapshot.javaClass.isRecord) {
                    KeyValueGrid(snapshot)
                } else {
                    Text(
                        text = snapshot.toString(),
                        style = VoxelTypography.regular(12),
                        color = VoxelColors.Zinc400
                    )
                }
            }
        }
    }
}

private data class OverviewSnapshot(
    val destination: String,
    val selectedPack: String,
    val currentElement: String,
    val pendingActions: Int,
    val openTabs: Int,
    val registries: Int,
    val totalEntries: Int
)

private data class RegistriesSnapshot(val counts: Map<String, Int>)
