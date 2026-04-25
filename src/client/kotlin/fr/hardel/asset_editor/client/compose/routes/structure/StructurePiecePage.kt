package fr.hardel.asset_editor.client.compose.routes.structure

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.components.page.structure.StructureBottomOverlay
import fr.hardel.asset_editor.client.compose.components.page.structure.StructureCameraReset
import fr.hardel.asset_editor.client.compose.components.page.structure.StructureInspector
import fr.hardel.asset_editor.client.compose.components.page.structure.StructureSceneArea
import fr.hardel.asset_editor.client.compose.components.page.structure.StructureTopOverlay
import fr.hardel.asset_editor.client.compose.components.page.structure.StructureViewMode
import fr.hardel.asset_editor.client.compose.components.page.structure.StructureYSlider
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import fr.hardel.asset_editor.client.compose.lib.rememberCurrentElementDestination
import fr.hardel.asset_editor.client.compose.lib.rememberServerData
import fr.hardel.asset_editor.client.memory.core.StudioDataSlots

@Composable
fun StructurePiecePage(context: StudioContext) {
    val destination = rememberCurrentElementDestination(context) ?: return
    val templates = rememberServerData(StudioDataSlots.STRUCTURE_TEMPLATES)
    val template = remember(templates, destination.elementId) {
        templates.firstOrNull { it.id().toString() == destination.elementId }
    } ?: return

    var showJigsaws by remember { mutableStateOf(true) }
    var sliceY by remember(template.id()) { mutableIntStateOf(template.sizeY()) }
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
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            StructureSceneArea(
                template = template,
                viewMode = StructureViewMode.PIECES,
                selectedStep = 0,
                animations = false,
                showJigsaws = showJigsaws,
                sliceY = sliceY,
                highlight = highlight,
                modifier = Modifier.fillMaxSize()
            )
            StructureTopOverlay(
                template = template,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            )
            StructureBottomOverlay(
                viewMode = StructureViewMode.PIECES,
                step = 0,
                maxStep = 0,
                onStepChange = {},
                animations = false,
                onAnimationsChange = {},
                showJigsaws = showJigsaws,
                onShowJigsawsChange = { showJigsaws = it },
                onReset = { StructureCameraReset.requestReset() },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            )
            StructureYSlider(
                value = sliceY,
                max = template.sizeY(),
                onValueChange = { sliceY = it },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp)
            )
        }

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
