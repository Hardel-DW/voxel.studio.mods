package fr.hardel.asset_editor.client.compose.components.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.StudioMotion
import fr.hardel.asset_editor.client.compose.StudioTypography

private val WIDTH_DEFAULT = 80.dp
private val WIDTH_HOVER = 128.dp
private val HEIGHT = 40.dp
private val ARROW_SIZE = 12.dp
private val ARROW_PADDING = 16.dp
private val CORNER = RoundedCornerShape(24.dp)

@Composable
fun Counter(
    value: Int,
    onValueChange: (Int) -> Unit,
    min: Int,
    max: Int,
    step: Int,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    displayValue: String = value.toString(),
    editValue: String = displayValue,
    sanitizeInput: (String) -> String = { it.filter(Char::isDigit) },
    parseInput: (String) -> Int? = String::toIntOrNull,
    keyboardType: KeyboardType = KeyboardType.Number
) {
    val hoverSource = remember { MutableInteractionSource() }
    val hoverActive = enabled && hoverSource.collectIsHoveredAsState().value
    val animatedWidth by animateDpAsState(if (hoverActive) WIDTH_HOVER else WIDTH_DEFAULT, tween(StudioMotion.Medium4))
    val arrowAlpha by animateFloatAsState(if (hoverActive) 1f else 0f, tween(StudioMotion.Medium4))
    val borderColor = when {
        !enabled -> Color.White.copy(alpha = 0.12f)
        hoverActive -> Color.White
        else -> Color.White.copy(alpha = 0.2f)
    }

    var editText by remember { mutableStateOf(displayValue) }
    var isFocused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(displayValue) { if (!isFocused) editText = displayValue }

    fun commit() { parseInput(editText)?.let { onValueChange(it.coerceIn(min, max)) } }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .width(animatedWidth).height(HEIGHT)
            .alpha(if (enabled) 1f else 0.55f)
            .clip(CORNER).border(2.dp, borderColor, CORNER)
            .then(if (enabled) Modifier.hoverable(hoverSource) else Modifier)
    ) {
        ArrowButton(Alignment.CenterStart, enabled && value > min, arrowAlpha,
            { onValueChange(maxOf(min, value - step)) }) { cx, cy ->
            drawLine(Color.White, Offset(cx + 3, cy - 6), Offset(cx - 3, cy), strokeWidth = 2f, cap = StrokeCap.Round)
            drawLine(Color.White, Offset(cx - 3, cy), Offset(cx + 3, cy + 6), strokeWidth = 2f, cap = StrokeCap.Round)
        }

        BasicTextField(
            value = editText,
            onValueChange = { editText = sanitizeInput(it) },
            textStyle = StudioTypography.bold(20).copy(color = Color.White, textAlign = TextAlign.Center),
            cursorBrush = SolidColor(Color.White),
            singleLine = true,
            enabled = enabled,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { commit(); focusManager.clearFocus() }),
            modifier = Modifier
                .width(60.dp)
                .pointerHoverIcon(PointerIcon.Text)
                .onFocusChanged { state ->
                    if (isFocused && !state.isFocused) {
                        commit()
                        editText = displayValue
                    } else if (!isFocused && state.isFocused && editText != editValue) {
                        editText = editValue
                    }
                    isFocused = state.isFocused
                }
        )

        ArrowButton(Alignment.CenterEnd, enabled && value < max, arrowAlpha,
            { onValueChange(minOf(max, value + step)) }) { cx, cy ->
            drawLine(Color.White, Offset(cx - 3, cy - 6), Offset(cx + 3, cy), strokeWidth = 2f, cap = StrokeCap.Round)
            drawLine(Color.White, Offset(cx + 3, cy), Offset(cx - 3, cy + 6), strokeWidth = 2f, cap = StrokeCap.Round)
        }
    }
}

@Composable
private fun BoxScope.ArrowButton(
    alignment: Alignment,
    enabled: Boolean,
    alpha: Float,
    onClick: () -> Unit,
    draw: DrawScope.(Float, Float) -> Unit
) {
    Box(
        modifier = Modifier
            .align(alignment).padding(horizontal = ARROW_PADDING)
            .width(ARROW_SIZE).height(ARROW_SIZE)
            .alpha(alpha * if (enabled) 1f else 0.3f)
            .then(if (enabled) Modifier.pointerHoverIcon(PointerIcon.Hand) else Modifier)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled
            ) { onClick() }
            .drawBehind { draw(size.width / 2f, size.height / 2f) }
    )
}
