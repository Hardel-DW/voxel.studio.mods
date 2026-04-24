package fr.hardel.asset_editor.client.compose.components.codec.widget.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.StudioMotion
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.codec.CodecTokens
import fr.hardel.asset_editor.client.compose.components.ui.SvgIcon
import net.minecraft.resources.Identifier

enum class FieldActionTone { ADD, REMOVE, NEUTRAL }

val FieldControlShape = RoundedCornerShape(FieldRowRadius)
private val RightControlShape = RoundedCornerShape(topEnd = FieldRowRadius, bottomEnd = FieldRowRadius)

private val PLUS = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/plus.svg")
private val TRASH = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/trash.svg")
private val CHEVRON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/chevron-down.svg")

@Composable
fun InlineFieldActionButton(
    label: String?,
    icon: Identifier,
    tone: FieldActionTone,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: RoundedCornerShape = RightControlShape
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val colors = actionColors(tone, hovered, enabled)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (label == null) Arrangement.Center else Arrangement.Start,
        modifier = modifier
            .height(FieldRowHeight)
            .clip(shape)
            .background(colors.background, shape)
            .border(1.dp, colors.border, shape)
            .hoverable(interaction, enabled)
            .pointerHoverIcon(if (enabled) PointerIcon.Hand else PointerIcon.Default)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = if (label == null) 0.dp else CodecTokens.PaddingX)
    ) {
        SvgIcon(icon, 12.dp, tint = colors.icon)
        if (label != null) {
            Spacer(Modifier.width(CodecTokens.GapLg))
            Text(text = label, style = StudioTypography.regular(12), color = colors.text)
        }
    }
}

@Composable
fun AddFieldButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: RoundedCornerShape = RightControlShape
) {
    InlineFieldActionButton(
        label = label,
        icon = PLUS,
        tone = FieldActionTone.ADD,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape
    )
}

@Composable
fun RemoveIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = FieldControlShape
) {
    InlineFieldActionButton(
        label = null,
        icon = TRASH,
        tone = FieldActionTone.REMOVE,
        onClick = onClick,
        modifier = modifier.size(FieldRowHeight),
        shape = shape
    )
}

@Composable
fun ToggleIconButton(
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = FieldControlShape
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val bg by animateColorAsState(
        targetValue = if (hovered) CodecTokens.HoverBg else CodecTokens.InputBg,
        animationSpec = StudioMotion.hoverSpec(),
        label = "toggle-bg"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(FieldRowHeight)
            .clip(shape)
            .background(bg, shape)
            .border(1.dp, CodecTokens.Border, shape)
            .hoverable(interaction)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(interactionSource = interaction, indication = null, onClick = onToggle)
    ) {
        SvgIcon(
            CHEVRON,
            12.dp,
            tint = if (hovered) CodecTokens.Text else CodecTokens.TextDimmed,
            modifier = Modifier.rotate(if (expanded) 0f else -90f)
        )
    }
}

private data class ActionColors(
    val background: Color,
    val border: Color,
    val icon: Color,
    val text: Color
)

private fun actionColors(tone: FieldActionTone, hovered: Boolean, enabled: Boolean): ActionColors {
    if (!enabled) {
        return ActionColors(
            background = CodecTokens.LabelBg,
            border = CodecTokens.Border.copy(alpha = 0.5f),
            icon = CodecTokens.TextMuted,
            text = CodecTokens.TextMuted
        )
    }

    return when (tone) {
        FieldActionTone.ADD -> ActionColors(
            background = if (hovered) CodecTokens.Add.copy(alpha = 0.10f) else CodecTokens.LabelBg,
            border = if (hovered) CodecTokens.Add.copy(alpha = 0.55f) else CodecTokens.Border,
            icon = if (hovered) CodecTokens.Add else CodecTokens.TextDimmed,
            text = if (hovered) CodecTokens.Text else CodecTokens.TextDimmed
        )
        FieldActionTone.REMOVE -> ActionColors(
            background = if (hovered) CodecTokens.Remove else Color.Transparent,
            border = if (hovered) CodecTokens.RemoveBorder else CodecTokens.Border,
            icon = if (hovered) CodecTokens.Text else CodecTokens.TextDimmed,
            text = if (hovered) CodecTokens.Text else CodecTokens.TextDimmed
        )
        FieldActionTone.NEUTRAL -> ActionColors(
            background = if (hovered) CodecTokens.HoverBg else CodecTokens.InputBg,
            border = CodecTokens.Border,
            icon = if (hovered) CodecTokens.Text else CodecTokens.TextDimmed,
            text = if (hovered) CodecTokens.Text else CodecTokens.TextDimmed
        )
    }
}
