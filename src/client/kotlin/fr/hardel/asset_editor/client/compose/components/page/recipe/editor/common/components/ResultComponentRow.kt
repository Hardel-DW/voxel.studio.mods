package fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import fr.hardel.asset_editor.client.compose.standardCollapseEnter
import fr.hardel.asset_editor.client.compose.standardCollapseExit
import fr.hardel.asset_editor.data.component.ComponentWidget
import kotlinx.coroutines.delay
import net.minecraft.client.resources.language.I18n
import net.minecraft.resources.Identifier

private val rowShape = RoundedCornerShape(10.dp)
private val CHEVRON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/chevron-right.svg")
private val TRASH = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/trash.svg")
private const val SAVE_DEBOUNCE_MS = 250L

@Composable
fun ResultComponentRow(
    componentId: Identifier,
    widget: ComponentWidget?,
    initialValue: JsonElement?,
    isPending: Boolean,
    validate: (JsonElement) -> Boolean,
    onSave: (JsonElement) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val displayName = remember(componentId) { StudioTranslation.resolve("component", componentId) }
    var expanded by remember(componentId) { mutableStateOf(isPending) }
    var draft by remember(componentId, initialValue) { mutableStateOf(initialValue) }

    LaunchedEffect(draft) {
        val current = draft
        if (current != null && current != initialValue) {
            delay(SAVE_DEBOUNCE_MS)
            if (validate(current)) onSave(current)
        }
    }

    val rowInteraction = remember { MutableInteractionSource() }
    val rowHovered by rowInteraction.collectIsHoveredAsState()
    val borderColor by animateColorAsState(
        targetValue = when {
            expanded -> StudioColors.Zinc700
            rowHovered -> StudioColors.Zinc800
            else -> StudioColors.Zinc900
        },
        animationSpec = StudioMotion.hoverSpec(),
        label = "component-row-border"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(rowShape)
            .background(StudioColors.Zinc900.copy(alpha = 0.35f), rowShape)
            .border(1.dp, borderColor, rowShape)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .hoverable(rowInteraction)
                .pointerHoverIcon(if (widget != null) PointerIcon.Hand else PointerIcon.Default)
                .clickable(
                    interactionSource = rowInteraction,
                    indication = null,
                    enabled = widget != null,
                    onClick = { expanded = !expanded }
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            if (widget != null) {
                SvgIcon(
                    location = CHEVRON,
                    size = 14.dp,
                    tint = if (expanded) StudioColors.Zinc300 else StudioColors.Zinc500,
                    modifier = Modifier.rotate(if (expanded) 90f else 0f)
                )
                Spacer(Modifier.width(10.dp))
            } else {
                Spacer(Modifier.width(24.dp))
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = displayName,
                    style = StudioTypography.medium(13),
                    color = StudioColors.Zinc100,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = componentId.toString(),
                    style = StudioTypography.regular(11).copy(fontFamily = FontFamily.Monospace),
                    color = StudioColors.Zinc500,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (widget == null) {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = I18n.get("recipe:components.unknown"),
                    style = StudioTypography.regular(10),
                    color = StudioColors.Amber400
                )
            }
            if (isPending) {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = I18n.get("recipe:components.pending"),
                    style = StudioTypography.regular(10),
                    color = StudioColors.Violet500
                )
            }

            Spacer(Modifier.width(12.dp))
            DeleteButton(onClick = onDelete)
        }

        if (widget != null) {
            AnimatedVisibility(
                visible = expanded,
                enter = standardCollapseEnter(),
                exit = standardCollapseExit()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    WidgetEditor(
                        widget = widget,
                        value = draft,
                        onValueChange = { draft = it }
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
            .size(24.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(bg, RoundedCornerShape(6.dp))
            .hoverable(interaction)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
    ) {
        SvgIcon(
            location = TRASH,
            size = 12.dp,
            tint = if (hovered) StudioColors.Red400 else StudioColors.Zinc500
        )
    }
}
