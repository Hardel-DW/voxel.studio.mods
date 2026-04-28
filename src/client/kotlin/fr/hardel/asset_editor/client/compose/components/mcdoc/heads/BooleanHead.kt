package fr.hardel.asset_editor.client.compose.components.mcdoc.heads

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.mcdoc.widget.McdocTokens

@Composable
fun BooleanHead(
    value: JsonElement?,
    onValueChange: (JsonElement) -> Unit,
    modifier: Modifier = Modifier,
    onClear: (() -> Unit)? = null
) {
    val current = (value as? JsonPrimitive)?.let { runCatching { it.asBoolean }.getOrNull() }
    Row(modifier = modifier.height(McdocTokens.RowHeight)) {
        ToggleSegment(label = "true", selected = current == true) {
            if (current == true) onClear?.invoke() else onValueChange(JsonPrimitive(true))
        }
        ToggleSegment(label = "false", selected = current == false) {
            if (current == false) onClear?.invoke() else onValueChange(JsonPrimitive(false))
        }
    }
}

@Composable
private fun ToggleSegment(label: String, selected: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val shape = RoundedCornerShape(McdocTokens.Radius)
    val bg = when {
        selected -> McdocTokens.Selected
        hovered -> McdocTokens.HoverBg
        else -> McdocTokens.InputBg
    }
    Box(
        modifier = Modifier
            .height(McdocTokens.RowHeight)
            .clip(shape)
            .background(bg, shape)
            .border(1.dp, if (selected) McdocTokens.SelectedBorder else McdocTokens.Border, shape)
            .hoverable(interaction)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = StudioTypography.medium(12),
            color = if (selected) McdocTokens.Text else McdocTokens.TextDimmed
        )
    }
}
