package fr.hardel.asset_editor.client.compose.components.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.VoxelColors
import fr.hardel.asset_editor.client.compose.VoxelTypography
import kotlin.math.roundToInt

@Composable
fun Range(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    min: Int,
    max: Int,
    step: Int,
    modifier: Modifier = Modifier,
    locked: Boolean = false,
    lockText: String? = null
) {
    val displayLabel = if (locked && lockText != null) lockText else label
    val steps = if (step > 0) ((max - min) / step) - 1 else 0

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (locked) 0.5f else 1f)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 2.dp)
        ) {
            Text(
                text = displayLabel,
                style = VoxelTypography.medium(13),
                color = VoxelColors.Zinc400
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = value.toString(),
                style = VoxelTypography.medium(13),
                color = VoxelColors.Zinc400
            )
        }

        Slider(
            value = value.toFloat(),
            onValueChange = { raw ->
                val rounded = (raw / step).roundToInt() * step
                onValueChange(rounded.coerceIn(min, max))
            },
            valueRange = min.toFloat()..max.toFloat(),
            steps = steps.coerceAtLeast(0),
            enabled = !locked,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = VoxelColors.Zinc800
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
