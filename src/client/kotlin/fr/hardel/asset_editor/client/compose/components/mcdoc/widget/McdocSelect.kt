package fr.hardel.asset_editor.client.compose.components.mcdoc.widget

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.StudioMotion
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.ui.SvgIcon
import fr.hardel.asset_editor.client.compose.popupEnterTransform
import net.minecraft.resources.Identifier

data class SelectOption(val value: String, val label: String)

private val CHEVRON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/chevron-down.svg")

@Composable
fun McdocSelect(
    options: List<SelectOption>,
    selected: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Select…",
    shape: RoundedCornerShape = RoundedCornerShape(McdocTokens.Radius)
) {
    var open by remember { mutableStateOf(false) }
    val selectedOption = options.firstOrNull { it.value == selected }

    Box(modifier = modifier) {
        SelectTrigger(
            label = selectedOption?.label,
            placeholder = placeholder,
            open = open,
            onClick = { open = !open },
            shape = shape
        )

        if (open) {
            SelectPopup(
                options = options,
                selectedValue = selected,
                onPick = { v ->
                    onSelect(v)
                    open = false
                },
                onDismiss = { open = false }
            )
        }
    }
}

@Composable
private fun SelectTrigger(
    label: String?,
    placeholder: String,
    open: Boolean,
    onClick: () -> Unit,
    shape: RoundedCornerShape
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val border by animateColorAsState(
        targetValue = if (open || hovered) McdocTokens.BorderStrong else McdocTokens.Border,
        animationSpec = StudioMotion.hoverSpec(),
        label = "select-border"
    )
    val bg by animateColorAsState(
        targetValue = if (hovered || open) McdocTokens.HoverBg else McdocTokens.InputBg,
        animationSpec = StudioMotion.hoverSpec(),
        label = "select-bg"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(McdocTokens.RowHeight)
            .clip(shape)
            .background(bg, shape)
            .border(1.dp, border, shape)
            .hoverable(interaction)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = McdocTokens.PaddingX)
    ) {
        Text(
            text = label ?: placeholder,
            style = StudioTypography.regular(13),
            color = if (label != null) McdocTokens.Text else McdocTokens.TextMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        SvgIcon(
            location = CHEVRON,
            size = 12.dp,
            tint = McdocTokens.TextDimmed,
            modifier = Modifier.rotate(if (open) 180f else 0f)
        )
    }
}

@Composable
private fun SelectPopup(
    options: List<SelectOption>,
    selectedValue: String?,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(Unit) { anim.animateTo(1f, StudioMotion.popupEnterSpec()) }
    val popupShape = RoundedCornerShape(McdocTokens.RadiusLg)

    Popup(
        offset = IntOffset(0, (McdocTokens.RowHeight.value + 4).toInt()),
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier
                .widthIn(min = 160.dp, max = 280.dp)
                .heightIn(max = 260.dp)
                .popupEnterTransform(
                    anim.value,
                    transformOrigin = TransformOrigin(0f, 0f),
                    translateY = 8.dp
                )
                .clip(popupShape)
                .background(McdocTokens.PopupBg, popupShape)
                .border(1.dp, McdocTokens.Border, popupShape)
                .padding(6.dp)
        ) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                items(items = options, key = { it.value }) { option ->
                    SelectItem(
                        label = option.label,
                        selected = option.value == selectedValue,
                        onClick = { onPick(option.value) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectItem(label: String, selected: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val itemShape = RoundedCornerShape(McdocTokens.Radius)
    val bg by animateColorAsState(
        targetValue = when {
            hovered -> McdocTokens.HoverBg
            selected -> McdocTokens.Selected.copy(alpha = 0.32f)
            else -> Color.Transparent
        },
        animationSpec = StudioMotion.hoverSpec(),
        label = "select-item-bg"
    )
    val fg = when {
        hovered -> McdocTokens.Text
        selected -> McdocTokens.Text
        else -> McdocTokens.TextDimmed
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(itemShape)
            .background(bg, itemShape)
            .hoverable(interaction)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = McdocTokens.PaddingX, vertical = 7.dp)
    ) {
        Text(
            text = label,
            style = StudioTypography.regular(13),
            color = fg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
