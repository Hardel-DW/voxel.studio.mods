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
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography

private val defaultInputShape = RoundedCornerShape(topEnd = 6.dp, bottomEnd = 6.dp)

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

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(FieldRowHeight)
            .clip(shape)
            .background(StudioColors.Zinc900.copy(alpha = 0.78f), shape)
            .border(1.dp, StudioColors.Zinc700.copy(alpha = 0.75f), shape)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        if (text.text.isEmpty()) {
            Text(
                text = placeholder,
                style = StudioTypography.regular(13),
                color = StudioColors.Zinc500
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
                color = StudioColors.Zinc100,
                fontFamily = if (monospace) FontFamily.Monospace else FontFamily.Default
            ),
            cursorBrush = SolidColor(StudioColors.Zinc100),
            singleLine = true,
            keyboardOptions = keyboardOptions,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focused = it.isFocused }
        )
    }
}
