package fr.hardel.asset_editor.client.compose.components.page.structure

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.ui.Button
import fr.hardel.asset_editor.client.compose.components.ui.ButtonVariant
import fr.hardel.asset_editor.client.compose.components.ui.InputText
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import fr.hardel.asset_editor.client.network.ClientPayloadSender
import fr.hardel.asset_editor.network.structure.StructureReplaceBlocksPayload
import fr.hardel.asset_editor.network.structure.StructureTemplateSnapshot
import net.minecraft.client.resources.language.I18n
import net.minecraft.resources.Identifier

@Composable
fun StructureInspector(
    context: StudioContext,
    template: StructureTemplateSnapshot,
    query: String,
    onQueryChange: (String) -> Unit,
    fromBlock: String,
    onFromBlockChange: (String) -> Unit,
    toBlock: String,
    onToBlockChange: (String) -> Unit
) {
    val filteredCounts = remember(template, query) {
        if (query.isBlank()) template.blockCounts()
        else template.blockCounts().filter { it.blockId().toString().contains(query, ignoreCase = true) }
    }
    val selectedPack = context.packSelectionMemory().selectedPack()
    val canReplace = selectedPack?.writable() == true &&
        Identifier.tryParse(fromBlock) != null &&
        Identifier.tryParse(toBlock) != null &&
        fromBlock != toBlock

    Column(
        modifier = Modifier
            .width(340.dp)
            .fillMaxHeight()
            .background(StudioColors.Zinc925, RoundedCornerShape(8.dp))
            .border(1.dp, StudioColors.Zinc800, RoundedCornerShape(8.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = I18n.get("structure:inspector.blocks"),
            style = StudioTypography.semiBold(16),
            color = StudioColors.Zinc100
        )
        InputText(
            value = query,
            onValueChange = onQueryChange,
            placeholder = I18n.get("structure:inspector.search_blocks"),
            focusExpand = false
        )
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(filteredCounts, key = { it.blockId().toString() }) { count ->
                BlockCountRow(count, onPick = { onFromBlockChange(count.blockId().toString()) })
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Text(
                text = I18n.get("structure:inspector.replace"),
                style = StudioTypography.semiBold(13),
                color = StudioColors.Zinc100
            )
            InputText(fromBlock, onFromBlockChange, placeholder = "minecraft:stone", showSearchIcon = false, focusExpand = false)
            InputText(toBlock, onToBlockChange, placeholder = "minecraft:deepslate", showSearchIcon = false, focusExpand = false)
            Button(
                onClick = {
                    val from = Identifier.tryParse(fromBlock) ?: return@Button
                    val to = Identifier.tryParse(toBlock) ?: return@Button
                    val packId = selectedPack?.packId() ?: return@Button
                    ClientPayloadSender.send(StructureReplaceBlocksPayload(packId, template.id(), from, to))
                },
                text = if (selectedPack == null) {
                    I18n.get("structure:inspector.replace.pack_required")
                } else {
                    I18n.get("structure:inspector.replace.action")
                },
                enabled = canReplace,
                variant = ButtonVariant.DEFAULT,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
