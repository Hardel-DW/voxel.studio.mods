package fr.hardel.asset_editor.client.compose.components.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.VoxelColors
import fr.hardel.asset_editor.client.compose.VoxelTypography

@Composable
fun InputText(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = ""
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(20.dp)
    val textStyle = VoxelTypography.regular(13).copy(color = VoxelColors.Zinc100)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .border(1.dp, if (focused) VoxelColors.Zinc700 else VoxelColors.Zinc800, shape)
            .background(
                color = if (focused) VoxelColors.Zinc700.copy(alpha = 0.2f) else VoxelColors.Zinc800.copy(alpha = 0.3f),
                shape = shape
            )
            .pointerHoverIcon(PointerIcon.Text)
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = textStyle,
            cursorBrush = SolidColor(VoxelColors.Zinc100),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .focusable()
                .onFocusChanged { focused = it.isFocused },
            decorationBox = { innerTextField ->
                Box(
                    contentAlignment = Alignment.CenterStart,
                    modifier = Modifier.padding(horizontal = 12.dp)
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = textStyle.copy(color = VoxelColors.Zinc400)
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}
