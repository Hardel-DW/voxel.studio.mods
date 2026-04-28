package fr.hardel.asset_editor.client.compose.components.mcdoc.widget

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.StudioMotion
import fr.hardel.asset_editor.client.compose.StudioTypography

@Composable
fun NumberStepper(
    value: String,
    onValueChange: (String) -> Unit,
    onStep: (delta: Long) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    error: String? = null
) {
    var focused by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf(TextFieldValue(value)) }

    LaunchedEffect(value, focused) {
        if (!focused && value != text.text) text = TextFieldValue(value)
    }

    val borderColor = when {
        error != null -> McdocTokens.Error
        focused -> McdocTokens.BorderStrong
        else -> McdocTokens.Border
    }
    val outerShape = RoundedCornerShape(McdocTokens.Radius)
    val leftShape = RoundedCornerShape(topStart = McdocTokens.Radius, bottomStart = McdocTokens.Radius)
    val rightShape = RoundedCornerShape(topEnd = McdocTokens.Radius, bottomEnd = McdocTokens.Radius)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(0.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(McdocTokens.RowHeight)
            .clip(outerShape)
            .border(1.dp, borderColor, outerShape)
    ) {
        StepButton(symbol = "−", shape = leftShape, onClick = { onStep(-1) })
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .height(McdocTokens.RowHeight)
                .background(McdocTokens.InputBg)
                .padding(horizontal = 4.dp)
        ) {
            if (text.text.isEmpty()) {
                Text(
                    text = placeholder,
                    style = StudioTypography.regular(13).copy(textAlign = TextAlign.Center),
                    color = McdocTokens.TextMuted
                )
            }
            BasicTextField(
                value = text,
                onValueChange = { next ->
                    text = next
                    if (next.text != value) onValueChange(next.text)
                },
                textStyle = StudioTypography.medium(13).copy(
                    color = McdocTokens.Text,
                    textAlign = TextAlign.Center
                ),
                cursorBrush = SolidColor(McdocTokens.Text),
                singleLine = true,
                keyboardOptions = keyboardOptions,
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged {
                        focused = it.isFocused
                        if (focused) text = text.copy(selection = TextRange(0, text.text.length))
                    }
            )
        }
        StepButton(symbol = "+", shape = rightShape, onClick = { onStep(1) })
    }
}

@Composable
private fun StepButton(symbol: String, shape: RoundedCornerShape, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val bg by animateColorAsState(
        targetValue = if (hovered) McdocTokens.HoverBg else McdocTokens.LabelBg,
        animationSpec = StudioMotion.hoverSpec(),
        label = "stepper-bg"
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(width = McdocTokens.RowHeight, height = McdocTokens.RowHeight)
            .clip(shape)
            .background(bg, shape)
            .hoverable(interaction)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
    ) {
        Text(
            text = symbol,
            style = StudioTypography.medium(15),
            color = if (hovered) McdocTokens.Text else McdocTokens.TextDimmed
        )
    }
}
