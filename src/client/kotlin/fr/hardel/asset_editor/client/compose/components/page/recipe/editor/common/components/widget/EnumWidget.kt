package fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.components.widget

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioMotion
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.ui.SvgIcon
import fr.hardel.asset_editor.client.compose.popupEnterTransform
import fr.hardel.asset_editor.data.component.ComponentWidget
import net.minecraft.resources.Identifier

private val triggerShape = RoundedCornerShape(8.dp)
private val popupShape = RoundedCornerShape(10.dp)
private val itemShape = RoundedCornerShape(6.dp)
private val CHEVRON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/chevron-down.svg")

@Composable
fun EnumWidget(
    widget: ComponentWidget.EnumWidget,
    value: JsonElement?,
    onValueChange: (JsonElement) -> Unit,
    modifier: Modifier = Modifier
) {
    val current = remember(value) { runCatching { value?.asString }.getOrNull() }
    val label = current ?: widget.defaultValue().orElse(widget.values().firstOrNull().orEmpty())
    var open by remember { mutableStateOf(false) }

    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val border by animateColorAsState(
        targetValue = when {
            open -> StudioColors.Zinc700
            hovered -> StudioColors.Zinc800
            else -> StudioColors.Zinc800.copy(alpha = 0.7f)
        },
        animationSpec = StudioMotion.hoverSpec(),
        label = "enum-trigger-border"
    )

    Box(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .widthIn(min = 160.dp)
                .height(32.dp)
                .clip(triggerShape)
                .background(StudioColors.Zinc950.copy(alpha = 0.5f), triggerShape)
                .border(1.dp, border, triggerShape)
                .hoverable(interaction)
                .pointerHoverIcon(PointerIcon.Hand)
                .clickable(interactionSource = interaction, indication = null, onClick = { open = !open })
                .padding(horizontal = 12.dp)
        ) {
            Text(
                text = humanize(label),
                style = StudioTypography.regular(13),
                color = StudioColors.Zinc100,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            SvgIcon(
                location = CHEVRON,
                size = 12.dp,
                tint = StudioColors.Zinc500,
                modifier = Modifier.rotate(if (open) 180f else 0f)
            )
        }

        if (open) {
            EnumPopup(
                values = widget.values(),
                selected = label,
                onPick = { picked ->
                    onValueChange(JsonPrimitive(picked))
                    open = false
                },
                onDismiss = { open = false }
            )
        }
    }
}

@Composable
private fun EnumPopup(
    values: List<String>,
    selected: String,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(Unit) { anim.animateTo(1f, StudioMotion.popupEnterSpec()) }

    Popup(
        offset = IntOffset(0, 40),
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier
                .widthIn(min = 180.dp, max = 280.dp)
                .heightIn(max = 260.dp)
                .popupEnterTransform(
                    anim.value,
                    transformOrigin = TransformOrigin(0f, 0f),
                    translateY = 8.dp
                )
                .clip(popupShape)
                .background(StudioColors.Zinc900, popupShape)
                .border(1.dp, Color.White.copy(alpha = 0.08f), popupShape)
                .padding(6.dp)
        ) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                items(items = values, key = { it }) { value ->
                    EnumItem(
                        label = humanize(value),
                        selected = value == selected,
                        onClick = { onPick(value) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EnumItem(label: String, selected: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val bg by animateColorAsState(
        targetValue = when {
            hovered -> StudioColors.Zinc800.copy(alpha = 0.75f)
            selected -> StudioColors.Zinc800.copy(alpha = 0.35f)
            else -> Color.Transparent
        },
        animationSpec = StudioMotion.hoverSpec(),
        label = "enum-item-bg"
    )
    val fg by animateColorAsState(
        targetValue = when {
            hovered -> StudioColors.Zinc50
            selected -> StudioColors.Violet500
            else -> StudioColors.Zinc300
        },
        animationSpec = StudioMotion.hoverSpec(),
        label = "enum-item-fg"
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(itemShape)
            .background(bg, itemShape)
            .hoverable(interaction)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp)
    ) {
        Text(
            text = label,
            style = StudioTypography.regular(13),
            color = fg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun humanize(value: String): String =
    value.split('_', '/').joinToString(" ") { part ->
        part.replaceFirstChar { ch -> ch.uppercase() }
    }
