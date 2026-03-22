package fr.hardel.asset_editor.client.compose.routes.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.VoxelColors
import fr.hardel.asset_editor.client.compose.VoxelTypography
import fr.hardel.asset_editor.client.compose.components.ui.KeyValueGrid
import fr.hardel.asset_editor.client.compose.components.ui.Section
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import net.minecraft.client.resources.language.I18n

@Composable
fun DebugWorkspacePage(context: StudioContext) {
    var version by remember { mutableIntStateOf(0) }

    DisposableEffect(context) {
        val subscriptions = listOf(
            context.sessionState().select({ snapshot -> snapshot }).subscribe({ version++ }, true),
            context.workspaceState().select({ snapshot -> snapshot }).subscribe({ version++ }, true),
            context.packState().select({ snapshot -> snapshot }).subscribe({ version++ }, true),
            context.tabsState().select({ snapshot -> snapshot }).subscribe({ version++ }, true),
            context.uiState().select({ snapshot -> snapshot }).subscribe({ version++ }, true),
            context.workspaceState().issueState().select({ snapshot -> snapshot }).subscribe({ version++ }, true)
        )
        val listener = Runnable { version++ }
        context.elementStore().subscribeAll(listener)
        onDispose {
            subscriptions.forEach { it.unsubscribe() }
            context.elementStore().unsubscribeAll(listener)
        }
    }

    val sections = remember(version) {
        linkedMapOf<String, Any>(
            I18n.get("debug:workspace.section.overview") to OverviewSnapshot(
                route = context.router.currentRoute.toString(),
                selectedPack = context.selectedPack?.packId() ?: "none",
                currentElement = context.currentElementId.ifBlank { "none" },
                pendingActions = context.workspaceState().snapshot().pendingActionCount(),
                openTabs = context.openTabs.size,
                registries = context.elementStore().entryCountsSnapshot().size,
                totalEntries = context.elementStore().entryCountsSnapshot().values.sum()
            ),
            I18n.get("debug:workspace.section.session") to context.sessionState().snapshot(),
            I18n.get("debug:workspace.section.ui") to context.uiState().snapshot(),
            "Tabs" to context.tabsState().snapshot(),
            "Pack" to context.packState().snapshot(),
            I18n.get("debug:workspace.section.registries") to RegistriesSnapshot(context.elementStore().entryCountsSnapshot()),
            I18n.get("debug:workspace.section.issues") to context.workspaceState().issueState().snapshot()
        )
    }

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
    val route: String,
    val selectedPack: String,
    val currentElement: String,
    val pendingActions: Int,
    val openTabs: Int,
    val registries: Int,
    val totalEntries: Int
)

private data class RegistriesSnapshot(val counts: Map<String, Int>)
