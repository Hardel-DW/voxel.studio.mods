package fr.hardel.asset_editor.client.compose.components.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography
import net.minecraft.resources.Identifier

private val CHEVRON_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/chevron-down.svg")
private val triggerShape = RoundedCornerShape(20.dp)
private val contentShape = RoundedCornerShape(8.dp)
private val itemShape = RoundedCornerShape(6.dp)

@Composable
fun <T> Dropdown(
    items: List<T>,
    selected: T?,
    labelExtractor: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null
) {
    var expanded by remember { mutableStateOf(false) }
    var showPopup by remember { mutableStateOf(false) }
    val animProgress = remember { Animatable(0f) }
    var triggerHeightPx by remember { mutableIntStateOf(0) }
    var triggerWidthPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val gapPx = with(density) { 4.dp.roundToPx() }

    val triggerInteraction = remember { MutableInteractionSource() }
    val isHovered by triggerInteraction.collectIsHoveredAsState()
    val borderColor = if (expanded || isHovered) StudioColors.Zinc700 else StudioColors.Zinc800
    val bgColor = if (expanded || isHovered) StudioColors.Zinc700.copy(alpha = 0.2f) else StudioColors.Zinc800.copy(alpha = 0.3f)

    LaunchedEffect(expanded) {
        if (expanded) {
            showPopup = true
            animProgress.animateTo(1f, tween(150))
        } else if (showPopup) {
            animProgress.animateTo(0f, tween(100))
            showPopup = false
        }
    }

    Box(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .height(40.dp)
                .clip(triggerShape)
                .border(1.dp, borderColor, triggerShape)
                .background(bgColor, triggerShape)
                .hoverable(triggerInteraction)
                .pointerHoverIcon(PointerIcon.Hand)
                .clickable(
                    interactionSource = triggerInteraction,
                    indication = null
                ) { expanded = !expanded }
                .padding(horizontal = 16.dp)
                .onGloballyPositioned { coords ->
                    triggerHeightPx = coords.size.height
                    triggerWidthPx = coords.size.width
                }
        ) {
            Text(
                text = if (selected != null) labelExtractor(selected) else "",
                style = StudioTypography.regular(13),
                color = StudioColors.Zinc100
            )
            SvgIcon(CHEVRON_ICON, 12.dp, StudioColors.Zinc400)
        }

        if (showPopup) {
            val minWidth = with(density) { triggerWidthPx.toDp() }

            Popup(
                alignment = Alignment.TopStart,
                offset = IntOffset(0, triggerHeightPx + gapPx),
                onDismissRequest = { expanded = false },
                properties = PopupProperties(focusable = true)
            ) {
                Box(
                    modifier = Modifier
                        .widthIn(min = minWidth)
                        .graphicsLayer {
                            val p = animProgress.value
                            alpha = p
                            scaleX = 0.95f + 0.05f * p
                            scaleY = 0.95f + 0.05f * p
                            transformOrigin = TransformOrigin(0f, 0f)
                        }
                        .shadow(
                            16.dp, contentShape,
                            ambientColor = Color.Black.copy(alpha = 0.4f),
                            spotColor = Color.Black.copy(alpha = 0.4f)
                        )
                        .border(1.dp, StudioColors.Zinc800, contentShape)
                        .background(StudioColors.Zinc950, contentShape)
                        .clip(contentShape)
                        .padding(4.dp)
                ) {
                    Column(modifier = Modifier.width(IntrinsicSize.Max)) {
                        if (label != null) {
                            Text(
                                text = label,
                                style = StudioTypography.medium(11),
                                color = StudioColors.Zinc500,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                        for (item in items) {
                            DropdownItem(
                                label = labelExtractor(item),
                                isSelected = item == selected,
                                onClick = {
                                    onSelect(item)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DropdownItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val bgModifier = if (isHovered) Modifier.background(StudioColors.Zinc800, itemShape) else Modifier

    Text(
        text = label,
        style = StudioTypography.regular(13),
        color = if (isSelected) StudioColors.Zinc100 else StudioColors.Zinc400,
        modifier = Modifier
            .fillMaxWidth()
            .clip(itemShape)
            .then(bgModifier)
            .hoverable(interactionSource)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() }
            .padding(vertical = 6.dp, horizontal = 8.dp)
    )
}
