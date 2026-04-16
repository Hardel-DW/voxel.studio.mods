package fr.hardel.asset_editor.client.compose.components.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.StudioTypography

@Composable
fun Badge(
    text: String,
    modifier: Modifier = Modifier,
    accent: Color = Color(0xFF38BDF8),
    textColor: Color = Color.White
) {
    val shape = RoundedCornerShape(6.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(accent.copy(alpha = 0.15f))
            .border(1.dp, accent.copy(alpha = 0.35f), shape)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = StudioTypography.bold(10),
            color = textColor
        )
    }
}
