package fr.hardel.asset_editor.client.compose.routes.structure

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import fr.hardel.asset_editor.client.compose.components.page.structure.StructureMessageScreen
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import fr.hardel.asset_editor.client.compose.lib.rememberCurrentElementDestination
import fr.hardel.asset_editor.client.compose.lib.rememberServerData
import fr.hardel.asset_editor.client.memory.core.StudioDataSlots
import net.minecraft.client.resources.language.I18n

@Composable
fun StructureMainPage(context: StudioContext) {
    val destination = rememberCurrentElementDestination(context) ?: return
    val worldgen = rememberServerData(StudioDataSlots.STRUCTURE_WORLDGEN)
    val templates = rememberServerData(StudioDataSlots.STRUCTURE_TEMPLATES)
    val elementId = destination.elementId

    val isWorldgen = remember(worldgen, elementId) {
        worldgen.any { it.id().toString() == elementId }
    }
    val isTemplate = remember(templates, elementId) {
        templates.any { it.id().toString() == elementId }
    }

    when {
        isWorldgen -> StructureViewerPage(context)
        isTemplate -> StructurePiecePage(context)
        else -> StructureMessageScreen(I18n.get("structure:viewer.empty"))
    }
}
