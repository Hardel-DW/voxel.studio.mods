package fr.hardel.asset_editor.client.compose.components.page.structure

import androidx.compose.runtime.Composable
import fr.hardel.asset_editor.client.compose.lib.rememberServerDataItem
import fr.hardel.asset_editor.client.memory.core.StudioDataSlots
import fr.hardel.asset_editor.network.structure.StructureTemplateSnapshot
import net.minecraft.resources.Identifier

@Composable
fun rememberStructureTemplate(id: Identifier): StructureTemplateSnapshot? =
    rememberServerDataItem(
        slot = StudioDataSlots.STRUCTURE_TEMPLATES,
        id = id,
        idOf = { it.id() }
    )
