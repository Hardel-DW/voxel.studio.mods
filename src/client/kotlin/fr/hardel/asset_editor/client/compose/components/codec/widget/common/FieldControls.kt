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
import androidx.compose.foundation.layout.fillMaxWidth
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
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioMotion
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.ui.SvgIcon
import net.minecraft.client.resources.language.I18n
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
            .padding(horizontal = if (label == null) 0.dp else 10.dp)
    ) {
        SvgIcon(icon, 12.dp, tint = colors.icon)
        if (label != null) {
            Spacer(Modifier.width(8.dp))
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
fun ListEntryHeader(
    index: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val shape = FieldControlShape
    val background by animateColorAsState(
        targetValue = if (hovered) StudioColors.Zinc800.copy(alpha = 0.42f) else StudioColors.Zinc900.copy(alpha = 0.42f),
        animationSpec = StudioMotion.hoverSpec(),
        label = "entry-header-bg"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(FieldRowHeight)
            .clip(shape)
            .background(background, shape)
            .hoverable(interaction)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(interactionSource = interaction, indication = null, onClick = onToggle)
            .padding(start = 8.dp, end = 4.dp)
    ) {
        SvgIcon(
            CHEVRON,
            12.dp,
            tint = if (hovered) StudioColors.Zinc200 else StudioColors.Zinc500,
            modifier = Modifier.rotate(if (expanded) 0f else -90f)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = I18n.get("codec:list.entry", index + 1),
            style = StudioTypography.medium(12),
            color = if (hovered) StudioColors.Zinc100 else StudioColors.Zinc300
        )
        Spacer(Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(StudioColors.Zinc800.copy(alpha = 0.55f))
        )
        Spacer(Modifier.width(6.dp))
        RemoveIconButton(onClick = onRemove)
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
            background = StudioColors.Zinc900.copy(alpha = 0.35f),
            border = StudioColors.Zinc800.copy(alpha = 0.35f),
            icon = StudioColors.Zinc600,
            text = StudioColors.Zinc600
        )
    }

    val accent = when (tone) {
        FieldActionTone.ADD -> StudioColors.Emerald400
        FieldActionTone.REMOVE -> StudioColors.Red500
        FieldActionTone.NEUTRAL -> StudioColors.Zinc400
    }

    return ActionColors(
        background = if (hovered) accent.copy(alpha = 0.12f) else StudioColors.Zinc900.copy(alpha = 0.48f),
        border = if (hovered) accent.copy(alpha = 0.42f) else StudioColors.Zinc800.copy(alpha = 0.55f),
        icon = if (hovered) accent else StudioColors.Zinc400,
        text = if (hovered) StudioColors.Zinc50 else StudioColors.Zinc300
    )
}
