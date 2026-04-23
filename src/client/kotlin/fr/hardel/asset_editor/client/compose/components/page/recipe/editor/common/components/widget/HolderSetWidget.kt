package fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.components.widget

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioMotion
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.components.widget.common.RegistryPicker
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.components.widget.common.RegistryPickerMode
import fr.hardel.asset_editor.client.compose.components.ui.SvgIcon
import fr.hardel.asset_editor.data.component.ComponentWidget
import net.minecraft.client.resources.language.I18n
import net.minecraft.resources.Identifier

private val tabShape = RoundedCornerShape(6.dp)
private val chipShape = RoundedCornerShape(999.dp)
private val addShape = RoundedCornerShape(8.dp)
private val TRASH = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/trash.svg")
private val PLUS = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/plus.svg")

private enum class Mode { TAG, LIST }

@Composable
fun HolderSetWidget(
    widget: ComponentWidget.HolderSetWidget,
    value: JsonElement?,
    onValueChange: (JsonElement) -> Unit,
    modifier: Modifier = Modifier
) {
    val initialMode = remember(value) { detectMode(value) }
    var mode by remember(initialMode) { mutableStateOf(initialMode) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ModeToggle(
            mode = mode,
            onChange = { next ->
                if (next != mode) {
                    mode = next
                    onValueChange(if (next == Mode.TAG) JsonPrimitive("#") else JsonArray())
                }
            }
        )

        when (mode) {
            Mode.TAG -> TagInlineRow(widget = widget, value = value, onValueChange = onValueChange)
            Mode.LIST -> ListModeContent(widget = widget, value = value, onValueChange = onValueChange)
        }
    }
}

@Composable
private fun ModeToggle(mode: Mode, onChange: (Mode) -> Unit) {
    Row(
        modifier = Modifier
            .clip(tabShape)
            .background(StudioColors.Zinc900.copy(alpha = 0.6f), tabShape)
            .padding(2.dp)
    ) {
        ModeTab(I18n.get("recipe:components.holder_set.mode.list"), mode == Mode.LIST) { onChange(Mode.LIST) }
        ModeTab(I18n.get("recipe:components.holder_set.mode.tag"), mode == Mode.TAG) { onChange(Mode.TAG) }
    }
}

@Composable
private fun ModeTab(label: String, selected: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val bg by animateColorAsState(
        targetValue = when {
            selected -> StudioColors.Zinc700
            hovered -> StudioColors.Zinc800.copy(alpha = 0.6f)
            else -> Color.Transparent
        },
        animationSpec = StudioMotion.hoverSpec(),
        label = "holder-set-mode-bg"
    )
    val fg by animateColorAsState(
        targetValue = if (selected) StudioColors.Zinc100 else StudioColors.Zinc500,
        animationSpec = StudioMotion.hoverSpec(),
        label = "holder-set-mode-fg"
    )
    Box(
        modifier = Modifier
            .height(26.dp)
            .clip(tabShape)
            .background(bg, tabShape)
            .hoverable(interaction)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, style = StudioTypography.medium(11), color = fg)
    }
}

@Composable
private fun TagInlineRow(
    widget: ComponentWidget.HolderSetWidget,
    value: JsonElement?,
    onValueChange: (JsonElement) -> Unit
) {
    val currentTag = remember(value) {
        val s = runCatching { value?.asString }.getOrNull().orEmpty()
        if (s.startsWith("#")) Identifier.tryParse(s.removePrefix("#")) else null
    }
    var pickerOpen by remember { mutableStateOf(false) }

    TagChipRow(
        label = currentTag?.let { "#$it" } ?: I18n.get("recipe:components.holder_set.tag.pick"),
        onClick = { pickerOpen = true }
    )

    if (pickerOpen) {
        RegistryPicker(
            registryId = widget.registry(),
            mode = RegistryPickerMode.TAGS,
            selected = currentTag,
            onPick = { id ->
                onValueChange(JsonPrimitive("#$id"))
                pickerOpen = false
            },
            onDismiss = { pickerOpen = false }
        )
    }
}

@Composable
private fun TagChipRow(label: String, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Box(
        modifier = Modifier
            .clip(addShape)
            .background(StudioColors.Zinc950.copy(alpha = 0.5f), addShape)
            .border(1.dp, if (hovered) StudioColors.Zinc700 else StudioColors.Zinc800, addShape)
            .hoverable(interaction)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = StudioTypography.regular(13).copy(fontFamily = FontFamily.Monospace),
            color = StudioColors.Zinc100,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ListModeContent(
    widget: ComponentWidget.HolderSetWidget,
    value: JsonElement?,
    onValueChange: (JsonElement) -> Unit
) {
    val array = remember(value) {
        when {
            value is JsonArray -> value
            else -> JsonArray()
        }
    }
    val ids = remember(array) {
        (0 until array.size()).mapNotNull { i ->
            runCatching { array.get(i).asString?.let(Identifier::tryParse) }.getOrNull()
        }
    }
    var pickerOpen by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (ids.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // simple flow: chips wrap via manual row is overkill — we just emit chips in a column
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ids.forEachIndexed { index, id ->
                    key(id.toString()) {
                        ChipRow(
                            id = id,
                            onRemove = {
                                val next = JsonArray()
                                ids.forEachIndexed { i, v -> if (i != index) next.add(v.toString()) }
                                onValueChange(next)
                            }
                        )
                    }
                }
            }
        }

        AddChipRow(
            label = I18n.get("recipe:components.holder_set.list.add"),
            onClick = { pickerOpen = true }
        )
    }

    if (pickerOpen) {
        RegistryPicker(
            registryId = widget.registry(),
            mode = RegistryPickerMode.ELEMENTS,
            selected = null,
            onPick = { id ->
                val next = JsonArray()
                ids.forEach { next.add(it.toString()) }
                if (id !in ids) next.add(id.toString())
                onValueChange(next)
                pickerOpen = false
            },
            onDismiss = { pickerOpen = false }
        )
    }
}

@Composable
private fun ChipRow(id: Identifier, onRemove: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(chipShape)
            .background(
                StudioColors.Zinc800.copy(alpha = if (hovered) 0.9f else 0.6f),
                chipShape
            )
            .hoverable(interaction)
            .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)
    ) {
        Text(
            text = id.toString(),
            style = StudioTypography.regular(12).copy(fontFamily = FontFamily.Monospace),
            color = StudioColors.Zinc200
        )
        Spacer(Modifier.width(6.dp))
        ChipRemove(onRemove)
    }
}

@Composable
private fun ChipRemove(onRemove: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(20.dp)
            .clip(chipShape)
            .background(
                if (hovered) StudioColors.Red500.copy(alpha = 0.25f) else Color.Transparent,
                chipShape
            )
            .hoverable(interaction)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(interactionSource = interaction, indication = null, onClick = onRemove)
    ) {
        SvgIcon(TRASH, 10.dp, tint = if (hovered) StudioColors.Red400 else StudioColors.Zinc500)
    }
}

@Composable
private fun AddChipRow(label: String, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val border by animateColorAsState(
        targetValue = if (hovered) StudioColors.Zinc700 else StudioColors.Zinc800.copy(alpha = 0.6f),
        animationSpec = StudioMotion.hoverSpec(),
        label = "holder-set-add-border"
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(addShape)
            .background(StudioColors.Zinc900.copy(alpha = 0.2f), addShape)
            .border(1.dp, border, addShape)
            .hoverable(interaction)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        SvgIcon(PLUS, 12.dp, tint = if (hovered) StudioColors.Zinc200 else StudioColors.Zinc500)
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            style = StudioTypography.regular(12),
            color = if (hovered) StudioColors.Zinc100 else StudioColors.Zinc400
        )
    }
}

private fun detectMode(value: JsonElement?): Mode =
    if (value != null && value.isJsonPrimitive && value.asJsonPrimitive.isString &&
        value.asString.startsWith("#")
    ) Mode.TAG else Mode.LIST
