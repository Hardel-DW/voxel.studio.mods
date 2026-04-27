package fr.hardel.asset_editor.client.compose.routes.structure

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioMotion
import fr.hardel.asset_editor.client.compose.components.page.structure.STRUCTURE_CONCEPT_ID
import fr.hardel.asset_editor.client.compose.components.page.structure.StructureAssemblyState
import fr.hardel.asset_editor.client.compose.components.page.structure.StructureInspector
import fr.hardel.asset_editor.client.compose.components.page.structure.StructureLoadingScreen
import fr.hardel.asset_editor.client.compose.components.page.structure.StructureMessageScreen
import fr.hardel.asset_editor.client.compose.components.page.structure.StructureSceneScaffold
import fr.hardel.asset_editor.client.compose.components.page.structure.StructureSceneSubject
import fr.hardel.asset_editor.client.compose.components.page.structure.StructureSceneTitleBar
import fr.hardel.asset_editor.client.compose.components.page.structure.StructureUiState
import fr.hardel.asset_editor.client.compose.components.page.structure.StructureVariantControls
import fr.hardel.asset_editor.client.compose.components.page.structure.StructureVariantState
import fr.hardel.asset_editor.client.compose.components.page.structure.StructureViewMode
import fr.hardel.asset_editor.client.compose.components.page.structure.rememberStructureAssemblyState
import fr.hardel.asset_editor.client.compose.components.page.structure.rememberStructureSceneState
import fr.hardel.asset_editor.client.compose.components.page.structure.rememberStructureTemplate
import fr.hardel.asset_editor.client.compose.components.page.structure.rememberStructureVariantState
import fr.hardel.asset_editor.client.compose.lib.ElementEditorDestination
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import fr.hardel.asset_editor.client.compose.lib.rememberCurrentElementDestination
import fr.hardel.asset_editor.client.compose.lib.rememberServerData
import fr.hardel.asset_editor.client.memory.core.StudioDataSlots
import fr.hardel.asset_editor.network.structure.StructureAssemblySnapshot
import fr.hardel.asset_editor.network.structure.StructureWorldgenSnapshot
import net.minecraft.client.resources.language.I18n
import net.minecraft.resources.Identifier

@Composable
fun StructureMainPage(context: StudioContext) {
    val destination = rememberCurrentElementDestination(context) ?: return
    val worldgen = rememberServerData(StudioDataSlots.STRUCTURE_WORLDGEN)
    val templateIndex = rememberServerData(StudioDataSlots.STRUCTURE_TEMPLATE_INDEX)
    val elementId = destination.elementId

    val worldgenEntry = remember(worldgen, elementId) {
        worldgen.firstOrNull { it.id().toString() == elementId }
    }
    val isTemplate = remember(templateIndex, elementId) {
        templateIndex.any { it.id().toString() == elementId }
    }

    when {
        worldgenEntry != null -> StructureViewerPage(context, worldgenEntry)
        isTemplate -> StructurePiecePage(context)
        else -> StructureMessageScreen(I18n.get("structure:viewer.empty"))
    }
}

@Composable
private fun StructureViewerPage(context: StudioContext, entry: StructureWorldgenSnapshot) {
    if (!entry.previewSupported()) {
        StructureMessageScreen(I18n.get("structure:viewer.unsupported_type"))
        return
    }

    val variantState = rememberStructureVariantState(entry.id())
    val assemblyState = rememberStructureAssemblyState(entry.id(), variantState.requestedParameters)

    when (assemblyState) {
        StructureAssemblyState.Idle, StructureAssemblyState.Loading -> StructureLoadingScreen()
        StructureAssemblyState.Empty -> StructureMessageScreen(I18n.get("structure:viewer.no_preview_data"))
        is StructureAssemblyState.Ready -> AssemblyScene(context, assemblyState.snapshot, variantState)
    }
}

@Composable
private fun AssemblyScene(context: StudioContext, assembly: StructureAssemblySnapshot, variantState: StructureVariantState) {
    val subject = remember(assembly) { StructureSceneSubject.Assembly(assembly) }
    StructureSceneWithInspector(
        subject = subject,
        initialShowJigsaws = false,
        title = assembly.id().path,
        metrics = listOf(
            "${assembly.sizeX()}x${assembly.sizeY()}x${assembly.sizeZ()}" to I18n.get("structure:overlay.size"),
            assembly.pieceCount().toString() to I18n.get("structure:overlay.pieces"),
            assembly.totalBlocks().toString() to I18n.get("structure:overlay.blocks")
        ),
        trailingTitleSlot = {
            StructureVariantControls(state = variantState, current = assembly.parameters())
        },
        onSelectPiece = { templateId ->
            StructureUiState.viewMode = StructureViewMode.PIECES
            context.navigationMemory().openElement(
                ElementEditorDestination(STRUCTURE_CONCEPT_ID, templateId.toString(), context.studioDefaultEditorTab(STRUCTURE_CONCEPT_ID))
            )
        }
    )
}

@Composable
private fun StructurePiecePage(context: StudioContext) {
    val destination = rememberCurrentElementDestination(context) ?: return
    val templateId = Identifier.tryParse(destination.elementId)
        ?: return StructureMessageScreen(I18n.get("structure:viewer.empty"))
    val template = rememberStructureTemplate(templateId) ?: return StructureLoadingScreen()

    val subject = remember(template) { StructureSceneSubject.Template(template) }

    StructureSceneWithInspector(
        subject = subject,
        initialShowJigsaws = true,
        title = template.id().path,
        metrics = listOf(
            "${template.sizeX()}x${template.sizeY()}x${template.sizeZ()}" to I18n.get("structure:overlay.size"),
            template.totalBlocks().toString() to I18n.get("structure:overlay.blocks"),
            template.entityCount().toString() to I18n.get("structure:overlay.entities")
        )
    )
}

@Composable
private fun StructureSceneWithInspector(
    subject: StructureSceneSubject,
    initialShowJigsaws: Boolean,
    title: String,
    metrics: List<Pair<String, String>>,
    trailingTitleSlot: (@Composable () -> Unit)? = null,
    onSelectPiece: ((Identifier) -> Unit)? = null
) {
    val state = rememberStructureSceneState(subject)
    val sceneReady = state.frame != null

    var blockQuery by remember(subject.id) { mutableStateOf("") }
    val highlight = blockQuery.takeIf { it.isNotBlank() }?.lowercase()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StudioColors.Zinc950)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StructureSceneScaffold(
                state = state,
                subject = subject,
                initialShowJigsaws = initialShowJigsaws,
                highlight = highlight,
                onSelectPiece = onSelectPiece,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                topOverlay = {
                    StructureSceneTitleBar(
                        title = title,
                        metrics = metrics,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        trailing = trailingTitleSlot
                    )
                }
            )

            StructureInspector(
                blockCounts = subject.blockCounts,
                query = blockQuery,
                onQueryChange = { blockQuery = it }
            )
        }

        AnimatedVisibility(
            visible = !sceneReady,
            modifier = Modifier.matchParentSize(),
            enter = fadeIn(animationSpec = StudioMotion.tabFadeSpec()),
            exit = fadeOut(animationSpec = StudioMotion.collapseExitSpec())
        ) {
            StructureLoadingScreen()
        }
    }
}
