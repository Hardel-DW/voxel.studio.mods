package fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.components.widget

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioMotion
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.components.WidgetEditor
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.components.defaultJsonFor
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.components.widget.common.FieldLabel
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.components.widget.common.FieldRowHeight
import fr.hardel.asset_editor.client.compose.components.ui.SvgIcon
import fr.hardel.asset_editor.client.compose.standardCollapseEnter
import fr.hardel.asset_editor.client.compose.standardCollapseExit
import fr.hardel.asset_editor.data.component.ComponentWidget
import net.minecraft.client.resources.language.I18n
import net.minecraft.resources.Identifier

private val PLUS = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/plus.svg")
private val TRASH = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/trash.svg")

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
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        widget.fields().forEach { field ->
            val key = field.key()
            val fieldValue = obj.get(key)
            val isPresent = fieldValue != null && !fieldValue.isJsonNull
            val isComplex = field.widget() is ComponentWidget.ObjectWidget ||
                field.widget() is ComponentWidget.ListWidget ||
                field.widget() is ComponentWidget.MapWidget ||
                field.widget() is ComponentWidget.DispatchedWidget

            if (field.optional() && isComplex) {
                OptionalComplexFieldRow(
                    label = localizedFieldLabel(key),
                    present = isPresent,
                    onAdd = {
                        val next = obj.deepCopy()
                        next.add(key, defaultJsonFor(field.widget()))
                        onValueChange(next)
                    },
                    onRemove = {
                        val next = obj.deepCopy()
                        next.remove(key)
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
            } else {
                InlineFieldRow(
                    label = localizedFieldLabel(key),
                    optional = field.optional()
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
}

/**
 * Row for a flat field (primitive, enum, holder). Label on the left, input on the right,
 * always visible — even when optional and absent (the nested widget shows its own "unset" state).
 */
@Composable
private fun InlineFieldRow(
    label: String,
    optional: Boolean,
    content: @Composable () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        FieldLabel(
            text = label,
            color = if (optional) StudioColors.Zinc500 else StudioColors.Zinc300
        )
        Box(modifier = Modifier.weight(1f)) { content() }
    }
}

/**
 * Row for a complex optional field (nested object/list/map/dispatched). When absent, shows
 * a "+" button to create the default. When present, shows a trash to remove, and the nested
 * content indented below.
 */
@Composable
private fun OptionalComplexFieldRow(
    label: String,
    present: Boolean,
    onAdd: () -> Unit,
    onRemove: () -> Unit,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            FieldLabel(
                text = label,
                color = if (present) StudioColors.Zinc300 else StudioColors.Zinc500
            )
            Box(modifier = Modifier.weight(1f)) {
                if (present) PresenceButton(iconId = TRASH, accent = StudioColors.Red500, onClick = onRemove)
                else PresenceButton(iconId = PLUS, accent = StudioColors.Emerald400, onClick = onAdd)
            }
        }

        AnimatedVisibility(
            visible = present,
            enter = standardCollapseEnter(),
            exit = standardCollapseExit()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp)
                    .border(
                        width = 0.dp,
                        color = Color.Transparent,
                        shape = RoundedCornerShape(0.dp)
                    )
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    // indent border (left)
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .padding(vertical = 2.dp)
                            .background(StudioColors.Zinc800)
                    )
                    Box(modifier = Modifier.weight(1f).padding(start = 10.dp, top = 4.dp)) {
                        content()
                    }
                }
            }
        }
    }
}

@Composable
private fun PresenceButton(
    iconId: Identifier,
    accent: Color,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val shape = RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp)
    val bg by animateColorAsState(
        targetValue = when {
            hovered -> accent.copy(alpha = 0.2f)
            else -> StudioColors.Zinc900.copy(alpha = 0.6f)
        },
        animationSpec = StudioMotion.hoverSpec(),
        label = "presence-btn-bg"
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(FieldRowHeight)
            .clip(shape)
            .background(bg, shape)
            .border(1.dp, StudioColors.Zinc800, shape)
            .hoverable(interaction)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
    ) {
        SvgIcon(
            location = iconId,
            size = 12.dp,
            tint = if (hovered) accent else StudioColors.Zinc500
        )
    }
}

private fun localizedFieldLabel(key: String): String {
    val translationKey = "recipe:components.field.$key"
    val translated = I18n.get(translationKey)
    return if (translated == translationKey) humanizeField(key) else translated
}

private fun humanizeField(key: String): String =
    key.split('_').joinToString(" ") { it.replaceFirstChar { ch -> ch.uppercase() } }
