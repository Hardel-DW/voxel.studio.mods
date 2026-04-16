package fr.hardel.asset_editor.client.compose.components.page.changes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography

@Composable
fun CommitMessageInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(12.dp)
    val textStyle = StudioTypography.regular(13).copy(color = StudioColors.Zinc100)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 40.dp)
            .border(1.dp, if (focused) StudioColors.Zinc700 else StudioColors.Zinc800, shape)
            .background(
                color = if (focused) StudioColors.Zinc700.copy(alpha = 0.2f) else StudioColors.Zinc800.copy(alpha = 0.3f),
                shape = shape
            )
            .pointerHoverIcon(PointerIcon.Text)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = textStyle,
            cursorBrush = SolidColor(StudioColors.Zinc100),
            singleLine = false,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focused = it.isFocused }
        )
        if (value.isEmpty()) {
            Text(
                text = placeholder,
                style = textStyle.copy(color = StudioColors.Zinc500),
                modifier = Modifier.padding(start = 2.dp)
            )
        }
    }
}
