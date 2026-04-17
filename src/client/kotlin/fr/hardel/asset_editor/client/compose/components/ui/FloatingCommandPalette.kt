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
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography
import net.minecraft.resources.Identifier

private val SEARCH_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/search.svg")

private val containerShape = RoundedCornerShape(12.dp)
private val itemShape = RoundedCornerShape(6.dp)

private const val ANIM_ENTER_MS = 150

// ── Root ─────────────────────────────────────────────────────────────────────

@Composable
fun CommandPalette(
    visible: Boolean,
    onDismiss: () -> Unit,
    title: String? = null,
    value: String = "",
    onValueChange: (String) -> Unit = {},
    placeholder: String = "",
    onSubmit: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    if (!visible) return

    val focusRequester = remember { FocusRequester() }
    val animProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        animProgress.animateTo(1f, tween(ANIM_ENTER_MS))
        runCatching { focusRequester.requestFocus() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = animProgress.value }
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 80.dp)
                .widthIn(min = 480.dp, max = 560.dp)
                .graphicsLayer {
                    val p = animProgress.value
                    scaleX = 0.97f + 0.03f * p
                    scaleY = 0.97f + 0.03f * p
                    alpha = p
                    transformOrigin = TransformOrigin(0.5f, 0f)
                }
                .shadow(
                    16.dp, containerShape,
                    ambientColor = Color.Black.copy(alpha = 0.4f),
                    spotColor = Color.Black.copy(alpha = 0.4f)
                )
                .border(1.dp, Color.White.copy(alpha = 0.10f), containerShape)
                .background(StudioColors.Zinc900, containerShape)
                .clip(containerShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
        ) {
            if (title != null) {
                Text(
                    text = title,
                    style = StudioTypography.semiBold(13),
                    color = StudioColors.Zinc200,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 16.dp, end = 16.dp,
                        top = if (title != null) 4.dp else 16.dp,
                        bottom = 12.dp
                    )
            ) {
                SvgIcon(SEARCH_ICON, 16.dp, StudioColors.Zinc500)
                Spacer(Modifier.width(10.dp))
                Box(modifier = Modifier.weight(1f)) {
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        textStyle = StudioTypography.regular(13).copy(color = StudioColors.Zinc100),
                        cursorBrush = SolidColor(StudioColors.Zinc100),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            imeAction = if (onSubmit != null) ImeAction.Done else ImeAction.Default
                        ),
                        keyboardActions = KeyboardActions(onDone = { onSubmit?.invoke() }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .onPreviewKeyEvent { event ->
                                if (event.type == KeyEventType.KeyDown && event.key == Key.Escape) {
                                    onDismiss()
                                    true
                                } else false
                            }
                    )
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = StudioTypography.regular(13),
                            color = StudioColors.Zinc500
                        )
                    }
                }
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.06f))
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .padding(8.dp)
            ) {
                content()
            }
        }
    }
}

// ── Group ────────────────────────────────────────────────────────────────────

@Composable
fun CommandPaletteGroup(
    heading: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        if (heading != null) {
            Text(
                text = heading,
                style = StudioTypography.medium(11),
                color = StudioColors.Zinc500,
                modifier = Modifier.padding(start = 8.dp, top = 12.dp, bottom = 4.dp)
            )
        }
        content()
    }
}

// ── Item ─────────────────────────────────────────────────────────────────────

@Composable
fun CommandPaletteItem(
    label: String,
    onClick: () -> Unit,
    description: String? = null,
    enabled: Boolean = true,
    selected: Boolean = false,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val showHighlight = enabled && (selected || isHovered)
    val textColor = when {
        !enabled -> StudioColors.Zinc600
        else -> StudioColors.Zinc200
    }
    val descColor = when {
        !enabled -> StudioColors.Zinc700
        else -> StudioColors.Zinc500
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(itemShape)
            .then(if (showHighlight) Modifier.background(StudioColors.Zinc800, itemShape) else Modifier)
            .hoverable(interactionSource, enabled)
            .pointerHoverIcon(if (enabled) PointerIcon.Hand else PointerIcon.Default)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .graphicsLayer { alpha = if (enabled) 1f else 0.5f }
    ) {
        if (leading != null) leading()
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = label,
                style = StudioTypography.regular(13),
                color = textColor
            )
            if (!description.isNullOrBlank()) {
                Text(
                    text = description,
                    style = StudioTypography.regular(11),
                    color = descColor
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) { trailing() }
        }
    }
}

// ── Empty ────────────────────────────────────────────────────────────────────

@Composable
fun CommandPaletteEmpty(text: String) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp)
    ) {
        Text(
            text = text,
            style = StudioTypography.regular(12),
            color = StudioColors.Zinc500
        )
    }
}

// ── Separator ────────────────────────────────────────────────────────────────

@Composable
fun CommandPaletteSeparator() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .height(1.dp)
            .background(Color.White.copy(alpha = 0.06f))
    )
}

// ── Hint ─────────────────────────────────────────────────────────────────────

@Composable
fun CommandPaletteHint(text: String) {
    Text(
        text = text,
        style = StudioTypography.regular(11),
        color = StudioColors.Zinc500,
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)
    )
}
