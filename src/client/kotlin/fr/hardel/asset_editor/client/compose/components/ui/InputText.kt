package fr.hardel.asset_editor.client.compose.components.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioMotion
import fr.hardel.asset_editor.client.compose.StudioTypography
import net.minecraft.resources.Identifier

private val SEARCH_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/search.svg")

/** Fraction of [maxWidth] used when the input is idle and [focusExpand] is enabled. */
private const val REST_WIDTH_FRACTION = 0.55f

/**
 * Standard search-style text input.
 *
 * When [maxWidth] is set and [focusExpand] is true (default), the field occupies
 * [REST_WIDTH_FRACTION] of [maxWidth] at rest and expands smoothly to the full width
 * on focus. Callers that need a fixed width (e.g. embedded in a tight layout) can
 * pass `focusExpand = false`.
 */
@Composable
fun InputText(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    maxWidth: Dp = Dp.Unspecified,
    showSearchIcon: Boolean = true,
    focusExpand: Boolean = true
) {
    var focused by remember { mutableStateOf(false) }
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val shape = RoundedCornerShape(20.dp)
    val textStyle = StudioTypography.regular(13).copy(color = StudioColors.Zinc100)

    val borderColor by animateColorAsState(
        targetValue = when {
            focused -> StudioColors.Zinc700
            hovered -> StudioColors.Zinc800.copy(alpha = 0.85f)
            else -> StudioColors.Zinc800
        },
        animationSpec = StudioMotion.hoverSpec(),
        label = "input-border"
    )
    val backgroundColor by animateColorAsState(
        targetValue = when {
            focused -> StudioColors.Zinc700.copy(alpha = 0.2f)
            hovered -> StudioColors.Zinc800.copy(alpha = 0.4f)
            else -> StudioColors.Zinc800.copy(alpha = 0.3f)
        },
        animationSpec = StudioMotion.hoverSpec(),
        label = "input-bg"
    )

    val widthModifier: Modifier = when {
        maxWidth == Dp.Unspecified -> Modifier.fillMaxWidth()
        !focusExpand -> Modifier.widthIn(max = maxWidth)
        else -> {
            val restWidth = maxWidth * REST_WIDTH_FRACTION
            val animatedWidth by animateDpAsState(
                targetValue = if (focused) maxWidth else restWidth,
                animationSpec = StudioMotion.hoverSpec(),
                label = "input-width"
            )
            Modifier.width(animatedWidth)
        }
    }

    Box(
        modifier = modifier
            .then(widthModifier)
            .height(40.dp)
            .border(1.dp, borderColor, shape)
            .background(backgroundColor, shape)
            .hoverable(interaction)
            .pointerHoverIcon(PointerIcon.Text)
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = textStyle,
            cursorBrush = SolidColor(StudioColors.Zinc100),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .onFocusChanged { focused = it.isFocused },
            decorationBox = { innerTextField ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp)
                ) {
                    if (showSearchIcon) {
                        SvgIcon(
                            location = SEARCH_ICON,
                            size = 16.dp,
                            tint = StudioColors.Zinc400
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Box(
                        contentAlignment = Alignment.CenterStart,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                style = textStyle.copy(color = StudioColors.Zinc400)
                            )
                        }
                        innerTextField()
                    }
                }
            }
        )
    }
}
