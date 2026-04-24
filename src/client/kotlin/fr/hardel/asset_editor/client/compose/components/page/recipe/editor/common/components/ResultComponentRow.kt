package fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.gson.JsonElement
import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioMotion
import fr.hardel.asset_editor.client.compose.StudioTranslation
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.ui.SvgIcon
import fr.hardel.asset_editor.client.compose.components.codec.WidgetEditor
import fr.hardel.asset_editor.client.compose.components.codec.WidgetHead
import fr.hardel.asset_editor.client.compose.components.codec.isInlineable
import fr.hardel.asset_editor.client.compose.standardCollapseEnter
import fr.hardel.asset_editor.client.compose.standardCollapseExit
import fr.hardel.asset_editor.data.codec.CodecWidget
import kotlinx.coroutines.delay
import net.minecraft.resources.Identifier

private val rowShape = RoundedCornerShape(8.dp)
private val deleteShape = RoundedCornerShape(6.dp)
private val CHEVRON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/chevron-right.svg")
private val TRASH = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/trash.svg")
private const val SAVE_DEBOUNCE_MS = 250L

@Composable
fun ResultComponentRow(
    componentId: Identifier,
    widget: CodecWidget?,
    initialValue: JsonElement?,
    isPending: Boolean,
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    validate: (JsonElement) -> Boolean,
    onSave: (JsonElement) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val displayName = remember(componentId) { StudioTranslation.resolve("component", componentId) }
    var draft by remember(componentId) { mutableStateOf(initialValue) }
    var locallyChanged by remember(componentId) { mutableStateOf(false) }

    LaunchedEffect(initialValue) {
        if (!locallyChanged) {
            draft = initialValue
        } else if (draft == initialValue) {
            locallyChanged = false
        }
    }

    LaunchedEffect(draft, locallyChanged, isPending) {
        val current = draft
        val initialPendingUnit = isPending && widget is CodecWidget.UnitWidget
        val shouldSave = current != null && when {
            isPending -> locallyChanged || initialPendingUnit
            else -> locallyChanged && current != initialValue
        }
        if (shouldSave) {
            delay(SAVE_DEBOUNCE_MS)
            if (validate(current)) onSave(current)
        }
    }

    val inlineHead = widget != null && isInlineable(widget)
    val canExpand = widget != null && !inlineHead
    val rowInteraction = remember { MutableInteractionSource() }
    val rowHovered by rowInteraction.collectIsHoveredAsState()

    val borderColor by animateColorAsState(
        targetValue = when {
            expanded -> StudioColors.Zinc700.copy(alpha = 0.52f)
            rowHovered -> StudioColors.Zinc700.copy(alpha = 0.44f)
            else -> StudioColors.Zinc800.copy(alpha = 0.42f)
        },
        animationSpec = StudioMotion.hoverSpec(),
        label = "row-border"
    )
    val bg by animateColorAsState(
        targetValue = when {
            expanded -> StudioColors.Zinc900.copy(alpha = 0.86f)
            rowHovered -> StudioColors.Zinc900.copy(alpha = 0.76f)
            else -> StudioColors.Zinc900.copy(alpha = 0.62f)
        },
        animationSpec = StudioMotion.hoverSpec(),
        label = "row-bg"
    )
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 90f else 0f,
        animationSpec = StudioMotion.collapseEnterSpec(),
        label = "row-chevron"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(rowShape)
            .background(bg, rowShape)
            .border(1.dp, borderColor, rowShape)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .hoverable(rowInteraction)
                .pointerHoverIcon(if (canExpand) PointerIcon.Hand else PointerIcon.Default)
                .clickable(
                    interactionSource = rowInteraction,
                    indication = null,
                    enabled = canExpand,
                    onClick = { onExpandChange(!expanded) }
                )
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            if (canExpand) {
                SvgIcon(
                    location = CHEVRON,
                    size = 12.dp,
                    tint = StudioColors.Zinc300,
                    modifier = Modifier.rotate(chevronRotation)
                )
                Spacer(Modifier.width(12.dp))
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = displayName,
                    style = StudioTypography.medium(14),
                    color = StudioColors.Zinc100,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = componentId.toString(),
                    style = StudioTypography.regular(11).copy(fontFamily = FontFamily.Monospace),
                    color = StudioColors.Zinc400,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (inlineHead && widget != null) {
                Spacer(Modifier.width(12.dp))
                WidgetHead(
                    widget = widget,
                    value = draft,
                    onValueChange = {
                        draft = it
                        locallyChanged = true
                    },
                    modifier = Modifier.widthIn(max = 360.dp)
                )
                Spacer(Modifier.width(12.dp))
            }

            DeleteButton(onClick = onDelete)
        }

        if (canExpand) {
            AnimatedVisibility(
                visible = expanded,
                enter = standardCollapseEnter(),
                exit = standardCollapseExit()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 4.dp)
                ) {
                    WidgetEditor(
                        widget = widget,
                        value = draft,
                        onValueChange = {
                            draft = it
                            locallyChanged = true
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DeleteButton(onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val bg by animateColorAsState(
        targetValue = if (hovered) StudioColors.Red500.copy(alpha = 0.2f) else Color.Transparent,
        animationSpec = StudioMotion.hoverSpec(),
        label = "delete-bg"
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(26.dp)
            .clip(deleteShape)
            .background(bg, deleteShape)
            .hoverable(interaction)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
    ) {
        SvgIcon(
            location = TRASH,
            size = 13.dp,
            tint = if (hovered) StudioColors.Red400 else StudioColors.Zinc500
        )
    }
}
