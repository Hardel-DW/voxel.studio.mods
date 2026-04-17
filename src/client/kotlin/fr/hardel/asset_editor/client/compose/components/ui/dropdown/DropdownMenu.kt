package fr.hardel.asset_editor.client.compose.components.ui.dropdown

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.LocalContentColor
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import fr.hardel.asset_editor.DevFlags
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.ui.SvgIcon

// ── Root ─────────────────────────────────────────────────────────────────────

@Composable
fun DropdownMenu(
    state: DropdownMenuState = rememberDropdownMenuState(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalDropdownMenuState provides state,
        LocalDropdownMenuClose provides { state.close() }
    ) {
        Box(
            modifier = Modifier.onGloballyPositioned { coords ->
                state.triggerWidthPx = coords.size.width
            }
        ) { content() }
    }
}

// ── Trigger ──────────────────────────────────────────────────────────────────

@Composable
fun DropdownMenuTrigger(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    val state = LocalDropdownMenuState.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { state.toggle() },
        content = content
    )
}

// ── Content ──────────────────────────────────────────────────────────────────

@Composable
fun DropdownMenuContent(
    modifier: Modifier = Modifier,
    sideOffset: Dp = 4.dp,
    minWidth: Dp = 128.dp,
    matchTriggerWidth: Boolean = false,
    side: DropdownMenuSide = DropdownMenuSide.BOTTOM,
    content: @Composable ColumnScope.() -> Unit
) {
    val state = LocalDropdownMenuState.current
    var showPopup by remember { mutableStateOf(false) }
    val animProgress = remember { Animatable(0f) }

    LaunchedEffect(state.expanded) {
        if (state.expanded) {
            showPopup = true
            animProgress.snapTo(0f)
            animProgress.animateTo(1f, tween(100))
        } else if (showPopup) {
            animProgress.animateTo(0f, tween(75))
            showPopup = false
        }
    }

    if (!showPopup) return

    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val gapPx = with(density) { sideOffset.roundToPx() }
    val triggerWidthDp = with(density) { state.triggerWidthPx.toDp() }
    val openSubMenu = remember { mutableStateOf<DropdownSubMenuState?>(null) }
    val tracker = remember { SafeTriangleTracker() }
    var contentCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }

    val positionProvider = remember(gapPx, side) {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize
            ): IntOffset {
                val x = anchorBounds.left
                val y = when (side) {
                    DropdownMenuSide.BOTTOM -> anchorBounds.bottom + gapPx
                    DropdownMenuSide.TOP -> anchorBounds.top - popupContentSize.height - gapPx
                }
                return IntOffset(x, y)
            }
        }
    }
    val originY = if (side == DropdownMenuSide.TOP) 1f else 0f

    Popup(
        popupPositionProvider = positionProvider,
        onDismissRequest = { state.close() },
        properties = PopupProperties(focusable = true)
    ) {
        Box(
            modifier = modifier
                .then(
                    if (matchTriggerWidth) Modifier.width(triggerWidthDp)
                    else Modifier.widthIn(min = minWidth).width(IntrinsicSize.Max)
                )
                .graphicsLayer {
                    val p = animProgress.value
                    alpha = p
                    scaleX = 0.95f + 0.05f * p
                    scaleY = 0.95f + 0.05f * p
                    transformOrigin = TransformOrigin(0f, originY)
                }
                .shadow(
                    8.dp, contentShape,
                    ambientColor = Color.Black.copy(alpha = 0.3f),
                    spotColor = Color.Black.copy(alpha = 0.3f)
                )
                .border(1.dp, Color.White.copy(alpha = 0.10f), contentShape)
                .background(StudioColors.Zinc900, contentShape)
                .clip(contentShape)
                .onGloballyPositioned { contentCoords = it }
                .pointerInput(tracker) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val local = event.changes.firstOrNull()?.position ?: continue
                            val coords = contentCoords ?: continue
                            tracker.onCursor(coords.localToWindow(local), scope)
                        }
                    }
                }
                .drawWithContent {
                    drawContent()
                    if (DevFlags.SHOW_HOVER_TRIANGLE) drawSafeTriangle(tracker, contentCoords)
                }
                .padding(4.dp)
        ) {
            CompositionLocalProvider(
                LocalOpenSubMenu provides openSubMenu,
                LocalSafeTriangleTracker provides tracker
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    content = content
                )
            }
        }
    }
}

// ── Group ────────────────────────────────────────────────────────────────────

@Composable
fun DropdownMenuGroup(content: @Composable ColumnScope.() -> Unit) {
    Column(content = content)
}

// ── Label ────────────────────────────────────────────────────────────────────

@Composable
fun DropdownMenuLabel(text: String, inset: Boolean = false) {
    Text(
        text = text,
        style = StudioTypography.medium(11),
        color = StudioColors.Zinc500,
        modifier = Modifier.padding(
            start = if (inset) 28.dp else 6.dp,
            end = 6.dp,
            top = 4.dp,
            bottom = 4.dp
        )
    )
}

// ── Item ─────────────────────────────────────────────────────────────────────

@Composable
fun DropdownMenuItem(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    variant: DropdownItemVariant = DropdownItemVariant.DEFAULT,
    inset: Boolean = false,
    closeOnClick: Boolean = true,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val closeMenu = LocalDropdownMenuClose.current

    val focusBg = when (variant) {
        DropdownItemVariant.DEFAULT -> StudioColors.Zinc800
        DropdownItemVariant.DESTRUCTIVE -> StudioColors.Red400.copy(alpha = 0.15f)
    }
    val textColor = when {
        !enabled -> StudioColors.Zinc600
        variant == DropdownItemVariant.DESTRUCTIVE -> StudioColors.Red400
        else -> StudioColors.Zinc200
    }

    CompositionLocalProvider(LocalContentColor provides textColor) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = modifier
                .fillMaxWidth()
                .clip(itemShape)
                .then(if (isHovered && enabled) Modifier.background(focusBg, itemShape) else Modifier)
                .hoverable(interactionSource, enabled)
                .pointerHoverIcon(if (enabled) PointerIcon.Hand else PointerIcon.Default)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled
                ) {
                    onClick()
                    if (closeOnClick) closeMenu()
                }
                .padding(horizontal = 6.dp, vertical = 4.dp)
                .then(if (inset) Modifier.padding(start = 22.dp) else Modifier)
                .graphicsLayer { alpha = if (enabled) 1f else 0.5f }
        ) {
            if (leading != null) leading()
            content()
            if (trailing != null) {
                Spacer(modifier = Modifier.weight(1f))
                trailing()
            }
        }
    }
}

// ── Checkbox item ────────────────────────────────────────────────────────────

@Composable
fun DropdownMenuCheckboxItem(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    inset: Boolean = false,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val textColor = if (enabled) StudioColors.Zinc200 else StudioColors.Zinc600

    CompositionLocalProvider(LocalContentColor provides textColor) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = modifier
                .fillMaxWidth()
                .clip(itemShape)
                .then(if (isHovered && enabled) Modifier.background(StudioColors.Zinc800, itemShape) else Modifier)
                .hoverable(interactionSource, enabled)
                .pointerHoverIcon(if (enabled) PointerIcon.Hand else PointerIcon.Default)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled
                ) { onCheckedChange(!checked) }
                .padding(start = if (inset) 28.dp else 6.dp, end = 6.dp, top = 4.dp, bottom = 4.dp)
                .graphicsLayer { alpha = if (enabled) 1f else 0.5f }
        ) {
            content()
            Spacer(modifier = Modifier.weight(1f))
            if (checked) {
                SvgIcon(CHECK_ICON, 16.dp, textColor)
            }
        }
    }
}

// ── Radio group ──────────────────────────────────────────────────────────────

@Composable
fun DropdownMenuRadioGroup(
    value: String,
    onValueChange: (String) -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val state = remember(value, onValueChange) { RadioGroupState(value, onValueChange) }
    CompositionLocalProvider(LocalRadioGroupState provides state) {
        Column(content = content)
    }
}

// ── Radio item ───────────────────────────────────────────────────────────────

@Composable
fun DropdownMenuRadioItem(
    value: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    inset: Boolean = false,
    content: @Composable RowScope.() -> Unit
) {
    val radioState = LocalRadioGroupState.current ?: return
    val isSelected = radioState.value == value
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val textColor = if (enabled) StudioColors.Zinc200 else StudioColors.Zinc600

    CompositionLocalProvider(LocalContentColor provides textColor) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = modifier
                .fillMaxWidth()
                .clip(itemShape)
                .then(if (isHovered && enabled) Modifier.background(StudioColors.Zinc800, itemShape) else Modifier)
                .hoverable(interactionSource, enabled)
                .pointerHoverIcon(if (enabled) PointerIcon.Hand else PointerIcon.Default)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled
                ) { radioState.onValueChange(value) }
                .padding(start = if (inset) 28.dp else 6.dp, end = 6.dp, top = 4.dp, bottom = 4.dp)
                .graphicsLayer { alpha = if (enabled) 1f else 0.5f }
        ) {
            content()
            Spacer(modifier = Modifier.weight(1f))
            if (isSelected) {
                SvgIcon(CHECK_ICON, 16.dp, textColor)
            }
        }
    }
}

// ── Separator ────────────────────────────────────────────────────────────────

@Composable
fun DropdownMenuSeparator() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .height(1.dp)
            .background(StudioColors.Zinc800)
    )
}

// ── Shortcut ─────────────────────────────────────────────────────────────────

@Composable
fun DropdownMenuShortcut(text: String) {
    Text(
        text = text,
        style = StudioTypography.regular(11),
        color = StudioColors.Zinc500,
        letterSpacing = TextUnit(1.2f, TextUnitType.Sp)
    )
}

// ── Sub-menu ─────────────────────────────────────────────────────────────────

@Composable
fun DropdownMenuSub(content: @Composable () -> Unit) {
    val subState = remember { DropdownSubMenuState() }
    CompositionLocalProvider(LocalDropdownSubMenuState provides subState) {
        Box { content() }
    }
}

// ── Sub-trigger ──────────────────────────────────────────────────────────────

@Composable
fun DropdownMenuSubTrigger(
    modifier: Modifier = Modifier,
    inset: Boolean = false,
    leading: (@Composable () -> Unit)? = null,
    content: @Composable RowScope.() -> Unit
) {
    val subState = LocalDropdownSubMenuState.current ?: return
    val openSubMenu = LocalOpenSubMenu.current
    val tracker = LocalSafeTriangleTracker.current
    val scope = rememberCoroutineScope()
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val textColor = StudioColors.Zinc200

    LaunchedEffect(isHovered) {
        if (!isHovered && subState.expanded) {
            tracker?.onTriggerLeave(scope)
        }
    }

    LaunchedEffect(isHovered, tracker?.graceApex) {
        if (isHovered) {
            val t = tracker
            if (t?.blocksOpeningOf(subState) != true) {
                val prev = openSubMenu?.value
                if (prev != null && prev !== subState) prev.close()
                openSubMenu?.value = subState
                t?.clearGrace(scope)
                subState.setTriggerHovered(true, scope)
            }
        } else {
            subState.setTriggerHovered(false, scope)
        }
    }

    LaunchedEffect(subState.expanded) {
        if (subState.expanded) tracker?.onSubMenuOpened(subState)
        else tracker?.onSubMenuClosed(subState, scope)
    }

    CompositionLocalProvider(LocalContentColor provides textColor) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = modifier
                .fillMaxWidth()
                .clip(itemShape)
                .then(
                    if (isHovered || subState.expanded)
                        Modifier.background(StudioColors.Zinc800, itemShape)
                    else Modifier
                )
                .hoverable(interactionSource)
                .pointerHoverIcon(PointerIcon.Hand)
                .padding(horizontal = 6.dp, vertical = 4.dp)
                .then(if (inset) Modifier.padding(start = 22.dp) else Modifier)
        ) {
            if (leading != null) leading()
            content()
            Spacer(modifier = Modifier.weight(1f))
            SvgIcon(CHEVRON_RIGHT_ICON, 12.dp, StudioColors.Zinc500)
        }
    }
}

// ── Sub-content ──────────────────────────────────────────────────────────────

@Composable
fun DropdownMenuSubContent(
    modifier: Modifier = Modifier,
    minWidth: Dp = 128.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val subState = LocalDropdownSubMenuState.current ?: return
    val parentTracker = LocalSafeTriangleTracker.current
    if (!subState.expanded) return

    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val gapPx = with(density) { 8.dp.roundToPx() }
    val animProgress = remember { Animatable(0f) }
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val tracker = remember { SafeTriangleTracker() }
    var boxCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }

    LaunchedEffect(Unit) {
        animProgress.animateTo(1f, tween(100))
    }

    LaunchedEffect(isHovered) {
        subState.setContentHovered(isHovered, scope)
        if (isHovered) parentTracker?.clearGrace(scope)
    }

    val openSubMenu = remember { mutableStateOf<DropdownSubMenuState?>(null) }

    val positionProvider = remember(gapPx) {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize
            ): IntOffset {
                val preferredX = anchorBounds.right + gapPx
                val x = if (preferredX + popupContentSize.width > windowSize.width) {
                    anchorBounds.left - gapPx - popupContentSize.width
                } else preferredX
                val y = anchorBounds.top.coerceAtMost(windowSize.height - popupContentSize.height)
                return IntOffset(x, y)
            }
        }
    }

    Popup(
        popupPositionProvider = positionProvider,
        properties = PopupProperties(focusable = false)
    ) {
        Box(
            modifier = modifier
                .widthIn(min = minWidth)
                .width(IntrinsicSize.Max)
                .graphicsLayer {
                    val p = animProgress.value
                    alpha = p
                    scaleX = 0.95f + 0.05f * p
                    scaleY = 0.95f + 0.05f * p
                    transformOrigin = TransformOrigin(0f, 0f)
                }
                .shadow(
                    12.dp, contentShape,
                    ambientColor = Color.Black.copy(alpha = 0.3f),
                    spotColor = Color.Black.copy(alpha = 0.3f)
                )
                .border(1.dp, Color.White.copy(alpha = 0.10f), contentShape)
                .background(StudioColors.Zinc900, contentShape)
                .clip(contentShape)
                .onGloballyPositioned { lc ->
                    boxCoords = lc
                    val tl = lc.localToWindow(Offset.Zero)
                    val sz = lc.size
                    parentTracker?.onSubMenuBounds(
                        Rect(tl.x, tl.y, tl.x + sz.width, tl.y + sz.height),
                        subState
                    )
                }
                .pointerInput(tracker) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val local = event.changes.firstOrNull()?.position ?: continue
                            val coords = boxCoords ?: continue
                            tracker.onCursor(coords.localToWindow(local), scope)
                        }
                    }
                }
                .drawWithContent {
                    drawContent()
                    if (DevFlags.SHOW_HOVER_TRIANGLE) {
                        drawSafeTriangle(tracker, boxCoords)
                        parentTracker?.let { drawSafeTriangle(it, boxCoords) }
                    }
                }
                .padding(4.dp)
                .hoverable(interactionSource)
        ) {
            CompositionLocalProvider(
                LocalOpenSubMenu provides openSubMenu,
                LocalSafeTriangleTracker provides tracker
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    content = content
                )
            }
        }
    }
}

// ── Select trigger (pill helper) ─────────────────────────────────────────────

@Composable
fun DropdownMenuSelectTrigger(
    label: String,
    modifier: Modifier = Modifier
) {
    val state = LocalDropdownMenuState.current
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val borderColor = if (state.expanded || isHovered) StudioColors.Zinc700 else StudioColors.Zinc800
    val bgColor = if (state.expanded || isHovered) StudioColors.Zinc700.copy(alpha = 0.2f) else StudioColors.Zinc800.copy(alpha = 0.3f)

    DropdownMenuTrigger(
        modifier = modifier
            .height(40.dp)
            .clip(selectTriggerShape)
            .border(1.dp, borderColor, selectTriggerShape)
            .background(bgColor, selectTriggerShape)
            .hoverable(interactionSource)
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = label,
            style = StudioTypography.regular(13),
            color = StudioColors.Zinc100
        )
        Spacer(modifier = Modifier.width(8.dp))
        SvgIcon(CHEVRON_DOWN_ICON, 12.dp, StudioColors.Zinc400)
    }
}
