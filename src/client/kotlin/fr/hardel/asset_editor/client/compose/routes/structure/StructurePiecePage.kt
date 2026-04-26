package fr.hardel.asset_editor.client.compose.routes.structure

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.components.page.structure.StructureInspector
import fr.hardel.asset_editor.client.compose.components.page.structure.StructureSceneScaffold
import fr.hardel.asset_editor.client.compose.components.page.structure.StructureSceneSubject
import fr.hardel.asset_editor.client.compose.components.page.structure.StructureSceneTitleBar
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import fr.hardel.asset_editor.client.compose.lib.rememberCurrentElementDestination
import fr.hardel.asset_editor.client.compose.lib.rememberServerData
import fr.hardel.asset_editor.client.memory.core.StudioDataSlots
import net.minecraft.client.resources.language.I18n

@Composable
fun StructurePiecePage(context: StudioContext) {
    val destination = rememberCurrentElementDestination(context) ?: return
    val templates = rememberServerData(StudioDataSlots.STRUCTURE_TEMPLATES)
    val template = remember(templates, destination.elementId) {
        templates.firstOrNull { it.id().toString() == destination.elementId }
    } ?: return

    val subject = remember(template) { StructureSceneSubject.Template(template) }
    var blockQuery by remember(template.id()) { mutableStateOf("") }
    var fromBlock by remember(template.id()) { mutableStateOf("") }
    var toBlock by remember(template.id()) { mutableStateOf("") }

    val highlight = (fromBlock.takeIf { it.isNotBlank() } ?: blockQuery.takeIf { it.isNotBlank() })?.lowercase()

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(StudioColors.Zinc950)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StructureSceneScaffold(
            subject = subject,
            initialShowJigsaws = true,
            highlight = highlight,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            topOverlay = {
                StructureSceneTitleBar(
                    title = template.id().path,
                    metrics = listOf(
                        "${template.sizeX()}x${template.sizeY()}x${template.sizeZ()}" to I18n.get("structure:overlay.size"),
                        template.totalBlocks().toString() to I18n.get("structure:overlay.blocks"),
                        template.entityCount().toString() to I18n.get("structure:overlay.entities")
                    ),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        )

        StructureInspector(
            context = context,
            template = template,
            query = blockQuery,
            onQueryChange = { blockQuery = it },
            fromBlock = fromBlock,
            onFromBlockChange = { fromBlock = it },
            toBlock = toBlock,
            onToBlockChange = { toBlock = it }
        )
    }
}
