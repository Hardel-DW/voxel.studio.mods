package fr.hardel.asset_editor.client.compose.routes.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.components.page.debug.DebugSidebar
import fr.hardel.asset_editor.client.compose.components.page.debug.DebugSidebarEntry
import fr.hardel.asset_editor.client.compose.components.page.debug.workspace.DebugWorkspaceRegistry
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import net.minecraft.client.resources.language.I18n

private val SIDEBAR_WIDTH = 260.dp

@Composable
fun DebugWorkspacePage(context: StudioContext) {
    var selectedId by remember { mutableStateOf(DebugWorkspaceRegistry.first()) }
    val selectedPanel = DebugWorkspaceRegistry.get(selectedId) ?: return

    val entries = DebugWorkspaceRegistry.all().map { panel ->
        DebugSidebarEntry(
            id = panel.id,
            icon = panel.icon,
            label = I18n.get(panel.labelKey),
            description = I18n.get(panel.descriptionKey),
            count = panel.count(context)
        )
    }

    Row(modifier = Modifier.fillMaxSize()) {
        DebugSidebar(
            entries = entries,
            selectedId = selectedId,
            onSelect = { selectedId = it },
            sectionLabel = I18n.get("debug:workspace.nav.section"),
            modifier = Modifier
                .width(SIDEBAR_WIDTH)
                .fillMaxHeight()
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(StudioColors.Zinc950)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp, vertical = 24.dp)
            ) {
                selectedPanel.render(context, Modifier.fillMaxWidth())
            }
        }
    }
}
