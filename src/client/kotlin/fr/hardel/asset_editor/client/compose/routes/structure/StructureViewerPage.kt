package fr.hardel.asset_editor.client.compose.routes.structure

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.page.structure.StructureSceneScaffold
import fr.hardel.asset_editor.client.compose.components.page.structure.StructureSceneSubject
import fr.hardel.asset_editor.client.compose.components.page.structure.StructureSceneTitleBar
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
    val structureId = destination?.elementId?.let(Identifier::tryParse) ?: return StructureViewerMessage(I18n.get("structure:viewer.empty"))

    val worldgen = rememberServerData(StudioDataSlots.STRUCTURE_WORLDGEN)
    val info = remember(worldgen, structureId) { worldgen.firstOrNull { it.id() == structureId } }
    val assembly = rememberStructureAssembly(structureId) ?: return StructureViewerLoading(structureId, info)

    val subject = remember(assembly) { StructureSceneSubject.Assembly(assembly) }

    StructureSceneScaffold(
        subject = subject,
        initialShowJigsaws = false,
        modifier = Modifier
            .fillMaxSize()
            .background(StudioColors.Zinc950)
            .padding(16.dp),
        topOverlay = {
            StructureSceneTitleBar(
                title = assembly.id().path,
                metrics = listOf(
                    "${assembly.sizeX()}x${assembly.sizeY()}x${assembly.sizeZ()}" to I18n.get("structure:overlay.size"),
                    assembly.pieceCount().toString() to I18n.get("structure:overlay.pieces"),
                    assembly.voxels().size.toString() to I18n.get("structure:overlay.voxels")
                ),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    )
}

@Composable
private fun StructureViewerMessage(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StudioColors.Zinc950)
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = message, style = StudioTypography.regular(14), color = StudioColors.Zinc400)
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
        Text(structureId.toString(), style = StudioTypography.semiBold(18), color = StudioColors.Zinc100)
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
