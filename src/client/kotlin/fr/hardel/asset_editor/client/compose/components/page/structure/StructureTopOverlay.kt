package fr.hardel.asset_editor.client.compose.components.page.structure

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.network.structure.StructureTemplateSnapshot
import net.minecraft.client.resources.language.I18n

@Composable
fun StructureTopOverlay(
    template: StructureTemplateSnapshot,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier.padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Text(
            text = template.id().path,
            style = StudioTypography.semiBold(13),
            color = StudioColors.Zinc100
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OverlayMetric("${template.sizeX()}x${template.sizeY()}x${template.sizeZ()}", I18n.get("structure:overlay.size"))
            OverlayDivider()
            OverlayMetric(template.totalBlocks().toString(), I18n.get("structure:overlay.blocks"))
            OverlayDivider()
            OverlayMetric(template.entityCount().toString(), I18n.get("structure:overlay.entities"))
        }
    }
}
