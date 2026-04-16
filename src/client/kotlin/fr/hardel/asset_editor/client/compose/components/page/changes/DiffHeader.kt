package fr.hardel.asset_editor.client.compose.components.page.changes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography

@Composable
fun DiffHeader(
    name: String,
    file: String,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .background(StudioColors.Zinc950.copy(alpha = 0.2f))
            .border(0.5.dp, StudioColors.Zinc800.copy(alpha = 0.5f), androidx.compose.foundation.shape.RoundedCornerShape(0.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TrafficLights()
            Text(
                text = name,
                style = StudioTypography.regular(13),
                color = StudioColors.Zinc300
            )
        }

        Text(
            text = file,
            style = StudioTypography.regular(11),
            color = StudioColors.Zinc600
        )
    }
}

@Composable
private fun TrafficLights() {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Dot(Color(0xFFEF4444))
        Dot(Color(0xFFFBBF24))
        Dot(Color(0xFF22C55E))
    }
}

@Composable
private fun Dot(color: Color) {
    Box(
        modifier = Modifier
            .size(10.dp)
            .background(color, CircleShape)
    )
}
