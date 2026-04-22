package fr.hardel.asset_editor.client.compose.components.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import fr.hardel.asset_editor.client.compose.StudioMotion
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.StudioTypography

private val WIDTH_DEFAULT = 80.dp
private val WIDTH_HOVER = 128.dp
private val HEIGHT = 40.dp
private const val ANIMATION_MS = StudioMotion.Medium4

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
    sanitizeInput: (String) -> String = { input -> input.filter(Char::isDigit) },
    parseInput: (String) -> Int? = String::toIntOrNull,
    keyboardType: KeyboardType = KeyboardType.Number
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val hoverActive = enabled && isHovered
    val canDecrement = enabled && value > min
    val canIncrement = enabled && value < max

    val animatedWidth by animateDpAsState(
        targetValue = if (hoverActive) WIDTH_HOVER else WIDTH_DEFAULT,
        animationSpec = tween(ANIMATION_MS)
    )
    val arrowAlpha by animateFloatAsState(
        targetValue = if (hoverActive) 1f else 0f,
        animationSpec = tween(ANIMATION_MS)
    )
    val borderColor = when {
        !enabled -> Color.White.copy(alpha = 0.12f)
        hoverActive -> Color.White
        else -> Color.White.copy(alpha = 0.2f)
    }

    var isEditing by remember { mutableStateOf(false) }
    var editText by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .width(animatedWidth)
            .height(HEIGHT)
            .alpha(if (enabled) 1f else 0.55f)
            .clip(RoundedCornerShape(24.dp))
            .border(2.dp, borderColor, RoundedCornerShape(24.dp))
            .then(if (enabled) Modifier.hoverable(interactionSource) else Modifier)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 16.dp)
                .width(12.dp)
                .height(12.dp)
                .alpha(arrowAlpha * if (canDecrement) 1f else 0.3f)
                .then(if (canDecrement) Modifier.pointerHoverIcon(PointerIcon.Hand) else Modifier)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { if (canDecrement) onValueChange(maxOf(min, value - step)) }
                .drawBehind {
                    val cx = size.width / 2
                    val cy = size.height / 2
                    drawLine(Color.White, Offset(cx + 3, cy - 6), Offset(cx - 3, cy), strokeWidth = 2f, cap = StrokeCap.Round)
                    drawLine(Color.White, Offset(cx - 3, cy), Offset(cx + 3, cy + 6), strokeWidth = 2f, cap = StrokeCap.Round)
                }
        )

        if (isEditing) {
            BasicTextField(
                value = editText,
                onValueChange = { editText = sanitizeInput(it) },
                textStyle = StudioTypography.bold(20).copy(color = Color.White, textAlign = TextAlign.Center),
                cursorBrush = SolidColor(Color.White),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    val parsed = parseInput(editText)
                    if (parsed != null) onValueChange(parsed.coerceIn(min, max))
                    isEditing = false
                    focusManager.clearFocus()
                }),
                modifier = Modifier
                    .width(60.dp)
                    .focusRequester(focusRequester)
                    .onFocusChanged { if (!it.isFocused && isEditing) {
                        val parsed = parseInput(editText)
                        if (parsed != null) onValueChange(parsed.coerceIn(min, max))
                        isEditing = false
                    }}
            )
        } else {
            Text(
                text = displayValue,
                style = StudioTypography.bold(20),
                color = Color.White,
                modifier = Modifier
                    .then(if (enabled) Modifier.pointerHoverIcon(PointerIcon.Text) else Modifier)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (enabled) {
                            editText = editValue
                            isEditing = true
                        }
                    }
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp)
                .width(12.dp)
                .height(12.dp)
                .alpha(arrowAlpha * if (canIncrement) 1f else 0.3f)
                .then(if (canIncrement) Modifier.pointerHoverIcon(PointerIcon.Hand) else Modifier)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { if (canIncrement) onValueChange(minOf(max, value + step)) }
                .drawBehind {
                    val cx = size.width / 2
                    val cy = size.height / 2
                    drawLine(Color.White, Offset(cx - 3, cy - 6), Offset(cx + 3, cy), strokeWidth = 2f, cap = StrokeCap.Round)
                    drawLine(Color.White, Offset(cx + 3, cy), Offset(cx - 3, cy + 6), strokeWidth = 2f, cap = StrokeCap.Round)
                }
        )
    }
}
