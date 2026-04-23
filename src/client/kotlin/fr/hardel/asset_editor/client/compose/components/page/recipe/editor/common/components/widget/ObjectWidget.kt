package fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.components.widget

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioMotion
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.components.WidgetEditor
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.components.defaultJsonFor
import fr.hardel.asset_editor.client.compose.standardCollapseEnter
import fr.hardel.asset_editor.client.compose.standardCollapseExit
import fr.hardel.asset_editor.data.component.ComponentWidget

@Composable
fun ObjectWidget(
    widget: ComponentWidget.ObjectWidget,
    value: JsonElement?,
    onValueChange: (JsonElement) -> Unit,
    modifier: Modifier = Modifier
) {
    val obj = remember(value) { (value as? JsonObject) ?: JsonObject() }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        widget.fields().forEach { field ->
            val key = field.key()
            val fieldValue = obj.get(key)
            val isPresent = fieldValue != null && !fieldValue.isJsonNull
            FieldRow(
                label = humanizeField(key),
                optional = field.optional(),
                included = isPresent || !field.optional(),
                onIncludeChange = { include ->
                    val next = obj.deepCopy()
                    if (include) {
                        next.add(key, defaultJsonFor(field.widget()))
                    } else {
                        next.remove(key)
                    }
                    onValueChange(next)
                }
            ) {
                WidgetEditor(
                    widget = field.widget(),
                    value = fieldValue,
                    onValueChange = { newVal ->
                        val next = obj.deepCopy()
                        next.add(key, newVal)
                        onValueChange(next)
                    }
                )
            }
        }
    }
}

@Composable
private fun FieldRow(
    label: String,
    optional: Boolean,
    included: Boolean,
    onIncludeChange: (Boolean) -> Unit,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (optional) {
                OptionalToggle(included = included, onToggle = { onIncludeChange(!included) })
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = label,
                style = StudioTypography.medium(12),
                color = if (included) StudioColors.Zinc200 else StudioColors.Zinc500
            )
        }
        AnimatedVisibility(
            visible = included,
            enter = standardCollapseEnter(),
            exit = standardCollapseExit()
        ) {
            Box(modifier = Modifier.padding(start = if (optional) 24.dp else 0.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun OptionalToggle(included: Boolean, onToggle: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val bg by animateColorAsState(
        targetValue = when {
            included -> StudioColors.Violet500
            hovered -> StudioColors.Zinc700
            else -> StudioColors.Zinc800
        },
        animationSpec = StudioMotion.hoverSpec(),
        label = "optional-toggle"
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(16.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(bg, RoundedCornerShape(4.dp))
            .hoverable(interaction)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(interactionSource = interaction, indication = null, onClick = onToggle)
    ) {
        if (included) {
            Text("✓", style = StudioTypography.medium(10), color = Color.White)
        }
    }
}

private fun humanizeField(key: String): String =
    key.split('_').joinToString(" ") { it.replaceFirstChar { ch -> ch.uppercase() } }
