package fr.hardel.asset_editor.client.compose.components.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography

/** End-aligned value-over-label pair, sized by the caller's modifier (typically a fixed width). */
@Composable
fun MetricColumn(value: String, label: String, modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.End, modifier = modifier) {
        Text(value, style = StudioTypography.semiBold(13), color = StudioColors.Zinc200)
        Text(label, style = StudioTypography.regular(10), color = StudioColors.Zinc500)
    }
}
