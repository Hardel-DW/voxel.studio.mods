package fr.hardel.asset_editor.client.compose.components.codec.widget

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import com.google.gson.JsonElement
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioMotion
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.codec.WidgetEditor
import fr.hardel.asset_editor.client.compose.components.codec.defaultJsonFor
import fr.hardel.asset_editor.data.codec.CodecWidget
import net.minecraft.client.resources.language.I18n

private val tabShape = RoundedCornerShape(6.dp)

@Composable
fun EitherWidget(
    widget: CodecWidget.EitherWidget,
    value: JsonElement?,
    onValueChange: (JsonElement) -> Unit,
    modifier: Modifier = Modifier
) {
    var isLeft by remember(value) { mutableStateOf(detectSide(widget, value)) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SideToggle(
            leftLabel = I18n.get("recipe:components.either.left"),
            rightLabel = I18n.get("recipe:components.either.right"),
            isLeft = isLeft,
            onChange = { left ->
                if (left != isLeft) {
                    isLeft = left
                    onValueChange(defaultJsonFor(if (left) widget.left() else widget.right()))
                }
            }
        )
        WidgetEditor(
            widget = if (isLeft) widget.left() else widget.right(),
            value = value,
            onValueChange = onValueChange
        )
    }
}

@Composable
private fun SideToggle(
    leftLabel: String,
    rightLabel: String,
    isLeft: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .clip(tabShape)
            .background(StudioColors.Zinc900.copy(alpha = 0.6f), tabShape)
            .padding(2.dp)
    ) {
        SideTab(label = leftLabel, selected = isLeft, onClick = { onChange(true) })
        SideTab(label = rightLabel, selected = !isLeft, onClick = { onChange(false) })
    }
}

@Composable
private fun SideTab(label: String, selected: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val bg by animateColorAsState(
        targetValue = when {
            selected -> StudioColors.Zinc700
            hovered -> StudioColors.Zinc800.copy(alpha = 0.6f)
            else -> Color.Transparent
        },
        animationSpec = StudioMotion.hoverSpec(),
        label = "either-tab-bg"
    )
    val fg by animateColorAsState(
        targetValue = if (selected) StudioColors.Zinc100 else StudioColors.Zinc500,
        animationSpec = StudioMotion.hoverSpec(),
        label = "either-tab-fg"
    )
    Box(
        modifier = Modifier
            .height(26.dp)
            .clip(tabShape)
            .background(bg, tabShape)
            .hoverable(interaction)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, style = StudioTypography.medium(11), color = fg)
    }
}

private fun detectSide(widget: CodecWidget.EitherWidget, value: JsonElement?): Boolean {
    if (value == null) return true
    return when (widget.left()) {
        is CodecWidget.HolderWidget -> value.isJsonPrimitive
        is CodecWidget.IdentifierWidget -> value.isJsonPrimitive
        is CodecWidget.ListWidget -> value.isJsonArray
        is CodecWidget.ObjectWidget -> value.isJsonObject
        else -> true
    }
}
