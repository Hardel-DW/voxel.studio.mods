package fr.hardel.asset_editor.client.compose.components.page.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography

private val SHAPE = RoundedCornerShape(10.dp)

@Composable
fun DebugWorkspaceEmptyState(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .clip(SHAPE)
            .background(StudioColors.Zinc900.copy(alpha = 0.2f))
            .border(1.dp, StudioColors.Zinc800.copy(alpha = 0.5f), SHAPE)
            .padding(vertical = 32.dp, horizontal = 16.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = StudioTypography.medium(13),
                color = StudioColors.Zinc400,
                textAlign = TextAlign.Center
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = StudioTypography.regular(11),
                    color = StudioColors.Zinc600,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
