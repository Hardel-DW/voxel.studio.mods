package fr.hardel.asset_editor.client.compose.components.page.structure

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
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
import fr.hardel.asset_editor.client.compose.components.ui.InputText
import fr.hardel.asset_editor.network.structure.StructureBlockCount
import net.minecraft.client.resources.language.I18n

@Composable
fun StructureInspector(
    blockCounts: List<StructureBlockCount>,
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val filteredCounts = remember(blockCounts, query) {
        if (query.isBlank()) blockCounts
        else blockCounts.filter { it.blockId().toString().contains(query, ignoreCase = true) }
    }

    Column(
        modifier = modifier
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
                BlockCountRow(count, onPick = { onQueryChange(count.blockId().toString()) })
            }
        }
    }
}
