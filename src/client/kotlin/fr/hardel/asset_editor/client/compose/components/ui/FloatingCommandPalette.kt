package fr.hardel.asset_editor.client.compose.components.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography
import net.minecraft.resources.Identifier

private val SEARCH_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/search.svg")

private val containerShape = RoundedCornerShape(14.dp)
private val inputShape = RoundedCornerShape(10.dp)
private val itemShape = RoundedCornerShape(8.dp)
private val hintShape = RoundedCornerShape(8.dp)

private const val ANIM_ENTER_MS = 180
private const val HOVER_FADE_MS = 120

// ── State ────────────────────────────────────────────────────────────────────

class CommandPaletteState internal constructor() {
    internal val items = mutableStateListOf<CommandItemData>()
    var selectedKey: Any? by mutableStateOf(null)
        internal set

    fun moveSelection(delta: Int) {
        val enabled = items.filter { it.enabled }
        if (enabled.isEmpty()) {
            selectedKey = null
            return
        }
        val current = enabled.indexOfFirst { it.key == selectedKey }
        val target = when {
            current < 0 -> if (delta > 0) 0 else enabled.lastIndex
            else -> (current + delta).coerceIn(0, enabled.lastIndex)
        }
        selectedKey = enabled[target].key
    }

    internal fun ensureSelection() {
        if (items.none { it.key == selectedKey && it.enabled }) {
            selectedKey = items.firstOrNull { it.enabled }?.key
        }
    }

    internal fun activateSelected(): Boolean {
        val item = items.firstOrNull { it.key == selectedKey && it.enabled } ?: return false
        item.onActivate()
        return true
    }
}

internal class CommandItemData(
    val key: Any,
    var onActivate: () -> Unit,
    var enabled: Boolean
)

private val LocalCommandPaletteState = staticCompositionLocalOf<CommandPaletteState?> { null }

// ── Root ─────────────────────────────────────────────────────────────────────

@Composable
fun CommandPalette(
    visible: Boolean,
    onDismiss: () -> Unit,
    value: String,
    onValueChange: (String) -> Unit,
    title: String? = null,
    placeholder: String = "",
    leadingIcon: Identifier? = SEARCH_ICON,
    onSubmit: (() -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    if (!visible) return

    val state = remember { CommandPaletteState() }
    val focusRequester = remember { FocusRequester() }
    val animProgress = remember { Animatable(0f) }
    var inputFocused by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        animProgress.animateTo(1f, tween(ANIM_ENTER_MS, easing = FastOutSlowInEasing))
        runCatching { focusRequester.requestFocus() }
    }

    LaunchedEffect(value, state.items.size) {
        state.ensureSelection()
    }

    Popup(
        alignment = Alignment.TopCenter,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = animProgress.value }
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 96.dp)
                    .widthIn(min = 480.dp, max = 560.dp)
                    .graphicsLayer {
                        val p = animProgress.value
                        alpha = p
                        translationY = (1f - p) * 12.dp.toPx()
                        transformOrigin = TransformOrigin(0.5f, 0f)
                    }
                    .shadow(
                        24.dp, containerShape,
                        ambientColor = Color.Black.copy(alpha = 0.5f),
                        spotColor = Color.Black.copy(alpha = 0.5f)
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.08f), containerShape)
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
                        text = title.uppercase(),
                        style = StudioTypography.medium(10).copy(
                            letterSpacing = TextUnit(1.4f, TextUnitType.Sp)
                        ),
                        color = StudioColors.Zinc500,
                        modifier = Modifier.padding(
                            start = 20.dp, end = 20.dp,
                            top = 18.dp, bottom = 8.dp
                        )
                    )
                }

                PaletteInput(
                    value = value,
                    onValueChange = onValueChange,
                    placeholder = placeholder,
                    leadingIcon = leadingIcon,
                    focusRequester = focusRequester,
                    focused = inputFocused,
                    onFocusChange = { inputFocused = it },
                    onSubmit = onSubmit,
                    onDismiss = onDismiss,
                    state = state,
                    topSpacing = if (title != null) 4.dp else 18.dp
                )

                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(StudioColors.Zinc800.copy(alpha = 0.6f))
                )

                CompositionLocalProvider(LocalCommandPaletteState provides state) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                            .padding(10.dp)
                    ) {
                        content()
                    }
                }

                if (actions != null) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(StudioColors.Zinc800.copy(alpha = 0.6f))
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Spacer(Modifier.weight(1f))
                        actions()
                    }
                }
            }
        }
    }
}

@Composable
private fun PaletteInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: Identifier?,
    focusRequester: FocusRequester,
    focused: Boolean,
    onFocusChange: (Boolean) -> Unit,
    onSubmit: (() -> Unit)?,
    onDismiss: () -> Unit,
    state: CommandPaletteState,
    topSpacing: Dp
) {
    val borderColor by animateColorAsState(
        targetValue = if (focused) StudioColors.Zinc700 else StudioColors.Zinc800.copy(alpha = 0.7f),
        animationSpec = tween(HOVER_FADE_MS),
        label = "palette-input-border"
    )
    val iconTint by animateColorAsState(
        targetValue = if (focused) StudioColors.Zinc300 else StudioColors.Zinc500,
        animationSpec = tween(HOVER_FADE_MS),
        label = "palette-input-icon"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = topSpacing, bottom = 14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
                .clip(inputShape)
                .background(StudioColors.Zinc950.copy(alpha = 0.5f), inputShape)
                .border(1.dp, borderColor, inputShape)
                .pointerHoverIcon(PointerIcon.Text)
                .padding(horizontal = 12.dp)
        ) {
            if (leadingIcon != null) {
                SvgIcon(leadingIcon, 15.dp, iconTint)
                Spacer(Modifier.width(10.dp))
            }
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
                        .onFocusChanged { onFocusChange(it.isFocused) }
                        .onPreviewKeyEvent { event ->
                            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                            when (event.key) {
                                Key.Escape -> {
                                    onDismiss(); true
                                }
                                Key.DirectionDown -> {
                                    state.moveSelection(1); true
                                }
                                Key.DirectionUp -> {
                                    state.moveSelection(-1); true
                                }
                                Key.Enter, Key.NumPadEnter -> {
                                    if (state.activateSelected()) true
                                    else {
                                        onSubmit?.invoke()
                                        onSubmit != null
                                    }
                                }
                                else -> false
                            }
                        }
                )
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = StudioTypography.regular(13),
                        color = StudioColors.Zinc600
                    )
                }
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
                text = heading.uppercase(),
                style = StudioTypography.medium(10).copy(
                    letterSpacing = TextUnit(1.2f, TextUnitType.Sp)
                ),
                color = StudioColors.Zinc500,
                modifier = Modifier.padding(start = 12.dp, top = 6.dp, bottom = 6.dp)
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
    key: Any = label,
    description: String? = null,
    enabled: Boolean = true,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null
) {
    val state = LocalCommandPaletteState.current
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val data = remember(key) { CommandItemData(key, onClick, enabled) }
    data.onActivate = onClick
    data.enabled = enabled

    if (state != null) {
        DisposableEffect(key) {
            state.items.add(data)
            if (state.selectedKey == null && enabled) state.selectedKey = key
            onDispose { state.items.remove(data) }
        }

        LaunchedEffect(isHovered) {
            if (isHovered && enabled) state.selectedKey = key
        }
    }

    val isSelected = enabled && (state?.selectedKey == key)

    val bgColor by animateColorAsState(
        targetValue = when {
            !enabled && isHovered -> StudioColors.Zinc800.copy(alpha = 0.25f)
            !enabled -> Color.Transparent
            isHovered -> StudioColors.Zinc800.copy(alpha = 0.75f)
            isSelected -> StudioColors.Zinc800.copy(alpha = 0.35f)
            else -> Color.Transparent
        },
        animationSpec = tween(HOVER_FADE_MS),
        label = "palette-item-bg"
    )
    val textColor by animateColorAsState(
        targetValue = when {
            !enabled -> StudioColors.Zinc600
            isHovered -> StudioColors.Zinc50
            isSelected -> StudioColors.Zinc100
            else -> StudioColors.Zinc300
        },
        animationSpec = tween(HOVER_FADE_MS),
        label = "palette-item-text"
    )
    val descColor by animateColorAsState(
        targetValue = when {
            !enabled -> StudioColors.Zinc700
            isHovered -> StudioColors.Zinc400
            isSelected -> StudioColors.Zinc500
            else -> StudioColors.Zinc500
        },
        animationSpec = tween(HOVER_FADE_MS),
        label = "palette-item-desc"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(itemShape)
            .background(bgColor, itemShape)
            .hoverable(interactionSource)
            .pointerHoverIcon(if (enabled) PointerIcon.Hand else PointerIcon.Default)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .graphicsLayer { alpha = if (enabled) 1f else 0.55f }
    ) {
        if (leading != null) leading()
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = label,
                style = StudioTypography.regular(13),
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!description.isNullOrBlank()) {
                Text(
                    text = description,
                    style = StudioTypography.regular(11),
                    color = descColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (trailing != null) {
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
            .padding(vertical = 36.dp)
    ) {
        Text(
            text = text,
            style = StudioTypography.regular(12),
            color = StudioColors.Zinc600
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
            .background(StudioColors.Zinc800.copy(alpha = 0.6f))
    )
}

// ── Hint ─────────────────────────────────────────────────────────────────────

@Composable
fun CommandPaletteHint(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(hintShape)
            .background(StudioColors.Zinc950.copy(alpha = 0.4f), hintShape)
            .border(1.dp, StudioColors.Zinc800.copy(alpha = 0.5f), hintShape)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = text,
            style = StudioTypography.regular(12),
            color = StudioColors.Zinc400
        )
    }
}
