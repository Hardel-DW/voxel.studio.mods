package fr.hardel.asset_editor.client.compose.components.codec.widget.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.codec.CodecTokens

private val defaultInputShape = RoundedCornerShape(topEnd = CodecTokens.Radius, bottomEnd = CodecTokens.Radius)

@Composable
internal fun CodecTextInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    monospace: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    normalize: (String) -> String = { it },
    shape: RoundedCornerShape = defaultInputShape
) {
    var focused by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf(TextFieldValue(value)) }

    LaunchedEffect(value, focused) {
        if (!focused && value != text.text) {
            text = TextFieldValue(value)
        }
    }

    val borderColor = if (focused) CodecTokens.BorderStrong else CodecTokens.Border

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(CodecTokens.RowHeight)
            .clip(shape)
            .background(CodecTokens.InputBg, shape)
            .border(1.dp, borderColor, shape)
            .padding(horizontal = CodecTokens.PaddingX),
        contentAlignment = Alignment.CenterStart
    ) {
        if (text.text.isEmpty()) {
            Text(
                text = placeholder,
                style = StudioTypography.regular(13),
                color = CodecTokens.TextMuted
            )
        }
        BasicTextField(
            value = text,
            onValueChange = { next ->
                val normalized = normalize(next.text)
                text = if (normalized == next.text) {
                    next
                } else {
                    next.copy(text = normalized, selection = TextRange(normalized.length))
                }
                onValueChange(normalized)
            },
            textStyle = StudioTypography.regular(13).copy(
                color = CodecTokens.Text,
                fontFamily = if (monospace) FontFamily.Monospace else FontFamily.Default
            ),
            cursorBrush = SolidColor(CodecTokens.Text),
            singleLine = true,
            keyboardOptions = keyboardOptions,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focused = it.isFocused }
        )
    }
}
