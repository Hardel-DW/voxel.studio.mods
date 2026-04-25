package fr.hardel.asset_editor.client.compose.routes.structure

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
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
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.page.structure.StructureAssemblySceneArea
import fr.hardel.asset_editor.client.compose.components.page.structure.StructureAssemblyTopOverlay
import fr.hardel.asset_editor.client.compose.components.page.structure.StructureBottomOverlay
import fr.hardel.asset_editor.client.compose.components.page.structure.StructureCameraReset
import fr.hardel.asset_editor.client.compose.components.page.structure.StructureViewMode
import fr.hardel.asset_editor.client.compose.components.page.structure.StructureYSlider
import fr.hardel.asset_editor.client.compose.components.page.structure.rememberStructureAssembly
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import fr.hardel.asset_editor.client.compose.lib.rememberCurrentElementDestination
import fr.hardel.asset_editor.client.compose.lib.rememberServerData
import fr.hardel.asset_editor.client.memory.core.StudioDataSlots
import fr.hardel.asset_editor.network.structure.StructureWorldgenSnapshot
import net.minecraft.client.resources.language.I18n
import net.minecraft.resources.Identifier

@Composable
fun StructureViewerPage(context: StudioContext) {
    val destination = rememberCurrentElementDestination(context)
    val structureId = destination?.elementId?.let(Identifier::tryParse)

    if (structureId == null) {
        StructureViewerEmpty()
        return
    }

    val worldgen = rememberServerData(StudioDataSlots.STRUCTURE_WORLDGEN)
    val info = remember(worldgen, structureId) { worldgen.firstOrNull { it.id() == structureId } }
    val assembly = rememberStructureAssembly(structureId)

    if (assembly == null) {
        StructureViewerLoading(structureId, info)
        return
    }

    var selectedStep by remember(structureId) { mutableIntStateOf(assembly.pieceCount()) }
    var animations by remember { mutableStateOf(true) }
    var showJigsaws by remember { mutableStateOf(false) }
    var sliceY by remember(structureId) { mutableIntStateOf(assembly.sizeY()) }

    val maxStep = assembly.pieceCount()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StudioColors.Zinc950)
            .padding(16.dp)
    ) {
        StructureAssemblySceneArea(
            assembly = assembly,
            selectedStep = selectedStep.coerceIn(0, maxStep),
            animations = animations,
            showJigsaws = showJigsaws,
            sliceY = sliceY,
            highlight = null,
            modifier = Modifier.fillMaxSize()
        )
        StructureAssemblyTopOverlay(
            assembly = assembly,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        )
        StructureBottomOverlay(
            viewMode = StructureViewMode.STRUCTURE,
            step = selectedStep,
            maxStep = maxStep,
            onStepChange = { selectedStep = it.coerceIn(0, maxStep) },
            animations = animations,
            onAnimationsChange = { animations = it },
            showJigsaws = showJigsaws,
            onShowJigsawsChange = { showJigsaws = it },
            onReset = { StructureCameraReset.requestReset() },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        )
        StructureYSlider(
            value = sliceY,
            max = assembly.sizeY(),
            onValueChange = { sliceY = it },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp)
        )
    }
}

@Composable
private fun StructureViewerEmpty() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StudioColors.Zinc950)
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = I18n.get("structure:viewer.empty"),
            style = StudioTypography.regular(14),
            color = StudioColors.Zinc400
        )
    }
}

@Composable
private fun StructureViewerLoading(structureId: Identifier, info: StructureWorldgenSnapshot?) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StudioColors.Zinc950)
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = structureId.toString(),
            style = StudioTypography.semiBold(18),
            color = StudioColors.Zinc100
        )
        info?.let {
            Text(
                text = it.type(),
                style = StudioTypography.regular(11),
                color = StudioColors.Zinc500,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
        Text(
            text = I18n.get("structure:viewer.assembling"),
            style = StudioTypography.regular(13),
            color = StudioColors.Zinc400,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}
