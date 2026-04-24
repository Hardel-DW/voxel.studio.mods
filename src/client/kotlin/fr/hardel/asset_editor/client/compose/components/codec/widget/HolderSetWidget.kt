package fr.hardel.asset_editor.client.compose.components.codec.widget

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import fr.hardel.asset_editor.client.compose.components.codec.widget.common.AddFieldButton
import fr.hardel.asset_editor.client.compose.components.codec.widget.common.FieldRowHeight
import fr.hardel.asset_editor.client.compose.components.codec.widget.common.RegistryCommandPalette
import fr.hardel.asset_editor.client.compose.components.codec.widget.common.RegistryPickerMode
import fr.hardel.asset_editor.client.compose.components.codec.widget.common.RequiredFieldFrame
import fr.hardel.asset_editor.client.compose.components.ui.SvgIcon
import fr.hardel.asset_editor.client.compose.components.ui.dropdown.DropdownMenu
import fr.hardel.asset_editor.client.compose.components.ui.dropdown.DropdownMenuContent
import fr.hardel.asset_editor.client.compose.components.ui.dropdown.DropdownMenuRadioGroup
import fr.hardel.asset_editor.client.compose.components.ui.dropdown.DropdownMenuRadioItem
import fr.hardel.asset_editor.client.compose.components.ui.dropdown.DropdownMenuTrigger
import fr.hardel.asset_editor.data.codec.CodecWidget
import net.minecraft.client.resources.language.I18n
import net.minecraft.resources.Identifier

private enum class HolderSetMode { TAG, LIST }

private val modeShape = RoundedCornerShape(0.dp)
private val trailingShape = RoundedCornerShape(topEnd = 6.dp, bottomEnd = 6.dp)
private val chipShape = RoundedCornerShape(999.dp)
private val TRASH = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/trash.svg")
private val CHEVRON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/chevron-down.svg")

@Composable
fun HolderSetWidget(
    widget: CodecWidget.HolderSetWidget,
    value: JsonElement?,
    onValueChange: (JsonElement) -> Unit,
    modifier: Modifier = Modifier,
    requiredMissing: Boolean = false
) {
    val initialMode = remember(value) { detectMode(value) }
    var mode by remember(initialMode) { mutableStateOf(initialMode) }

    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(0.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        ModeDropdown(
            mode = mode,
            onChange = { next ->
                if (next == mode) return@ModeDropdown
                mode = next
                onValueChange(if (next == HolderSetMode.TAG) JsonPrimitive("#") else JsonArray())
            }
        )

        Box(modifier = Modifier.weight(1f)) {
            when (mode) {
                HolderSetMode.TAG -> TagModeContent(
                    registry = widget.registry(),
                    value = value,
                    requiredMissing = requiredMissing,
                    onValueChange = onValueChange
                )

                HolderSetMode.LIST -> ListModeContent(
                    registry = widget.registry(),
                    value = value,
                    requiredMissing = requiredMissing,
                    onValueChange = onValueChange
                )
            }
        }
    }
}

@Composable
private fun ModeDropdown(mode: HolderSetMode, onChange: (HolderSetMode) -> Unit) {
    DropdownMenu {
        ModeTrigger(label = modeLabel(mode))

        DropdownMenuContent(matchTriggerWidth = true, minWidth = 104.dp) {
            DropdownMenuRadioGroup(
                value = mode.name,
                onValueChange = { selected -> onChange(HolderSetMode.valueOf(selected)) }
            ) {
                DropdownMenuRadioItem(HolderSetMode.TAG.name) {
                    Text(modeLabel(HolderSetMode.TAG), style = StudioTypography.regular(13))
                }
                DropdownMenuRadioItem(HolderSetMode.LIST.name) {
                    Text(modeLabel(HolderSetMode.LIST), style = StudioTypography.regular(13))
                }
            }
        }
    }
}

@Composable
private fun ModeTrigger(label: String) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val bg by animateColorAsState(
        targetValue = if (hovered) StudioColors.Zinc700.copy(alpha = 0.36f) else StudioColors.Zinc900.copy(alpha = 0.52f),
        animationSpec = StudioMotion.hoverSpec(),
        label = "holder-set-mode-bg"
    )

    DropdownMenuTrigger(
        modifier = Modifier
            .width(96.dp)
            .height(FieldRowHeight)
            .clip(modeShape)
            .background(bg, modeShape)
            .border(1.dp, StudioColors.Zinc800.copy(alpha = 0.55f), modeShape)
            .hoverable(interaction)
            .padding(horizontal = 10.dp)
    ) {
        Text(label, style = StudioTypography.medium(13), color = StudioColors.Zinc100, modifier = Modifier.weight(1f))
        SvgIcon(CHEVRON, 12.dp, tint = StudioColors.Zinc500)
    }
}

@Composable
private fun TagModeContent(
    registry: Identifier,
    value: JsonElement?,
    requiredMissing: Boolean,
    onValueChange: (JsonElement) -> Unit
) {
    val currentTag = remember(value) {
        val raw = runCatching { value?.asString }.getOrNull().orEmpty()
        if (raw.startsWith("#")) Identifier.tryParse(raw.removePrefix("#")) else null
    }
    var pickerOpen by remember { mutableStateOf(false) }

    Box {
        RequiredFieldFrame(requiredMissing = requiredMissing, modifier = Modifier.fillMaxWidth()) {
            HolderSetTrigger(
                label = currentTag?.let { "#$it" } ?: I18n.get("recipe:components.holder_set.tag.pick"),
                onClick = { pickerOpen = true }
            )
        }

        RegistryCommandPalette(
            visible = pickerOpen,
            registryId = registry,
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
private fun ListModeContent(
    registry: Identifier,
    value: JsonElement?,
    requiredMissing: Boolean,
    onValueChange: (JsonElement) -> Unit
) {
    val ids = remember(value) { holderSetIds(value) }
    var pickerOpen by remember { mutableStateOf(false) }

    Box {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (ids.isEmpty()) {
                RequiredFieldFrame(requiredMissing = requiredMissing, modifier = Modifier.weight(1f)) {
                    AddFieldButton(
                        label = I18n.get("recipe:components.holder_set.list.add"),
                        onClick = { pickerOpen = true },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .height(FieldRowHeight)
                        .horizontalScroll(rememberScrollState())
                ) {
                    ids.forEachIndexed { index, id ->
                        key(id.toString()) {
                            HolderSetChip(
                                id = id,
                                onRemove = {
                                    val next = JsonArray()
                                    ids.forEachIndexed { i, item -> if (i != index) next.add(item.toString()) }
                                    onValueChange(next)
                                }
                            )
                        }
                    }
                }
                AddFieldButton(
                    label = I18n.get("recipe:components.holder_set.list.add"),
                    onClick = { pickerOpen = true },
                    modifier = Modifier.width(132.dp)
                )
            }
        }

        RegistryCommandPalette(
            visible = pickerOpen,
            registryId = registry,
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
private fun HolderSetTrigger(label: String, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val border by animateColorAsState(
        targetValue = if (hovered) StudioColors.Zinc700.copy(alpha = 0.7f) else StudioColors.Zinc800.copy(alpha = 0.55f),
        animationSpec = StudioMotion.hoverSpec(),
        label = "holder-set-trigger-border"
    )

    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = Modifier
            .fillMaxWidth()
            .height(FieldRowHeight)
            .clip(trailingShape)
            .background(StudioColors.Zinc900.copy(alpha = 0.52f), trailingShape)
            .border(1.dp, border, trailingShape)
            .hoverable(interaction)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp)
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
private fun HolderSetChip(id: Identifier, onRemove: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.Companion
            .height(FieldRowHeight)
            .clip(chipShape)
            .background(StudioColors.Zinc800.copy(alpha = if (hovered) 0.72f else 0.45f), chipShape)
            .border(1.dp, StudioColors.Zinc700.copy(alpha = 0.42f), chipShape)
            .hoverable(interaction)
            .padding(start = 12.dp, end = 4.dp)
    ) {
        Text(
            text = id.toString(),
            style = StudioTypography.regular(12).copy(fontFamily = FontFamily.Monospace),
            color = StudioColors.Zinc200,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
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
            .background(if (hovered) StudioColors.Red500.copy(alpha = 0.18f) else Color.Transparent, chipShape)
            .hoverable(interaction)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(interactionSource = interaction, indication = null, onClick = onRemove)
    ) {
        SvgIcon(TRASH, 10.dp, tint = if (hovered) StudioColors.Red400 else StudioColors.Zinc500)
    }
}

private fun holderSetIds(value: JsonElement?): List<Identifier> {
    if (value !is JsonArray) return emptyList()
    return (0 until value.size()).mapNotNull { index ->
        runCatching { value.get(index).asString?.let(Identifier::tryParse) }.getOrNull()
    }
}

private fun detectMode(value: JsonElement?): HolderSetMode {
    if (value == null || !value.isJsonPrimitive) return HolderSetMode.LIST
    val primitive = value.asJsonPrimitive
    if (!primitive.isString) return HolderSetMode.LIST
    return if (primitive.asString.startsWith("#")) HolderSetMode.TAG else HolderSetMode.LIST
}

private fun modeLabel(mode: HolderSetMode): String = when (mode) {
    HolderSetMode.TAG -> I18n.get("recipe:components.holder_set.mode.tag")
    HolderSetMode.LIST -> I18n.get("recipe:components.holder_set.mode.list")
}
