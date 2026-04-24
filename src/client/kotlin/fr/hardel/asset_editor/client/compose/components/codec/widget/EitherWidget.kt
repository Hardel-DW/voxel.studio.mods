package fr.hardel.asset_editor.client.compose.components.codec.widget

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import com.google.gson.JsonElement
import fr.hardel.asset_editor.client.compose.StudioMotion
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.codec.CodecTokens
import fr.hardel.asset_editor.client.compose.components.codec.WidgetBody
import fr.hardel.asset_editor.client.compose.components.codec.WidgetHead
import fr.hardel.asset_editor.client.compose.components.codec.defaultJsonFor
import fr.hardel.asset_editor.client.compose.components.codec.detectEitherSide
import fr.hardel.asset_editor.client.compose.components.codec.hasHead
import fr.hardel.asset_editor.data.codec.CodecWidget
import net.minecraft.client.resources.language.I18n

private val tabShape = RoundedCornerShape(CodecTokens.Radius)

@Composable
fun EitherHead(
    widget: CodecWidget.EitherWidget,
    value: JsonElement?,
    onValueChange: (JsonElement) -> Unit,
    modifier: Modifier = Modifier
) {
    val isLeft = detectEitherSide(widget, value)
    val activeWidget = if (isLeft) widget.left() else widget.right()

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CodecTokens.Gap),
        modifier = modifier.fillMaxWidth()
    ) {
        SideToggle(
            leftLabel = I18n.get("codec:either.left"),
            rightLabel = I18n.get("codec:either.right"),
            isLeft = isLeft,
            onChange = { left ->
                if (left != isLeft) {
                    onValueChange(defaultJsonFor(if (left) widget.left() else widget.right()))
                }
            }
        )
        if (hasHead(activeWidget)) {
            Box(modifier = Modifier.weight(1f)) {
                WidgetHead(widget = activeWidget, value = value, onValueChange = onValueChange)
            }
        }
    }
}

@Composable
fun EitherBody(
    widget: CodecWidget.EitherWidget,
    value: JsonElement?,
    onValueChange: (JsonElement) -> Unit,
    modifier: Modifier = Modifier
) {
    val isLeft = detectEitherSide(widget, value)
    val activeWidget = if (isLeft) widget.left() else widget.right()
    WidgetBody(widget = activeWidget, value = value, onValueChange = onValueChange)
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
            .background(CodecTokens.LabelBg, tabShape)
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
            selected -> CodecTokens.Selected
            hovered -> CodecTokens.HoverBg
            else -> Color.Transparent
        },
        animationSpec = StudioMotion.hoverSpec(),
        label = "either-tab-bg"
    )
    val fg by animateColorAsState(
        targetValue = if (selected) CodecTokens.Text else CodecTokens.TextDimmed,
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
