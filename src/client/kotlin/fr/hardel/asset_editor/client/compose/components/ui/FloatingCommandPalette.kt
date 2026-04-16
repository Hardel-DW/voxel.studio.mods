package fr.hardel.asset_editor.client.compose.components.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.LaunchedEffect
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography

@Composable
fun FloatingCommandPalette(
    visible: Boolean,
    title: String,
    searchValue: String,
    onSearchChange: (String) -> Unit,
    searchPlaceholder: String,
    onDismiss: () -> Unit,
    onSubmit: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    if (!visible) return

    val overlayShape = RoundedCornerShape(12.dp)
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        runCatching { focusRequester.requestFocus() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 64.dp)
                .widthIn(min = 480.dp, max = 640.dp)
                .shadow(32.dp, overlayShape, ambientColor = Color.Black, spotColor = Color.Black)
                .clip(overlayShape)
                .border(1.dp, StudioColors.Zinc800, overlayShape)
                .background(StudioColors.Zinc950)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = StudioTypography.bold(13),
                color = StudioColors.Zinc100
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, StudioColors.Zinc800, RoundedCornerShape(8.dp))
                    .background(StudioColors.Zinc900.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .pointerHoverIcon(PointerIcon.Text)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                BasicTextField(
                    value = searchValue,
                    onValueChange = onSearchChange,
                    textStyle = StudioTypography.regular(13).copy(color = StudioColors.Zinc100),
                    cursorBrush = SolidColor(StudioColors.Zinc100),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = if (onSubmit != null) ImeAction.Done else ImeAction.Default),
                    keyboardActions = KeyboardActions(onDone = { onSubmit?.invoke() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )
                if (searchValue.isEmpty()) {
                    Text(
                        text = searchPlaceholder,
                        style = StudioTypography.regular(13),
                        color = StudioColors.Zinc500
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 0.dp, max = 320.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
fun CommandPaletteRow(
    label: String,
    description: String? = null,
    trailing: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = StudioTypography.regular(13),
                color = if (enabled) StudioColors.Zinc100 else StudioColors.Zinc600
            )
            if (!description.isNullOrBlank()) {
                Text(
                    text = description,
                    style = StudioTypography.regular(11),
                    color = StudioColors.Zinc500
                )
            }
        }
        if (trailing != null) trailing()
    }
}
