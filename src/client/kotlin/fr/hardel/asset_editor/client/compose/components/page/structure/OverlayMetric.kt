package fr.hardel.asset_editor.client.compose.components.page.structure

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography

@Composable
fun OverlayMetric(value: String, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(value, style = StudioTypography.semiBold(12), color = StudioColors.Zinc100)
        Text(label, style = StudioTypography.regular(10), color = StudioColors.Zinc500)
    }
}
