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

@Composable
fun StructureSceneTitleBar(
    title: String,
    metrics: List<Pair<String, String>>,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier.padding(horizontal = 2.dp)
    ) {
        Text(
            text = title,
            style = StudioTypography.semiBold(13),
            color = StudioColors.Zinc100
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            metrics.forEachIndexed { index, (value, label) ->
                if (index > 0) OverlayDivider()
                OverlayMetric(value, label)
            }
        }
    }
}
