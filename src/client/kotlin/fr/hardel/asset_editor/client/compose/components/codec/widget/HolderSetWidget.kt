package fr.hardel.asset_editor.client.compose.components.codec.widget

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
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import fr.hardel.asset_editor.client.compose.StudioMotion
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.codec.CodecTokens
import fr.hardel.asset_editor.client.compose.components.codec.widget.common.AddFieldButton
import fr.hardel.asset_editor.client.compose.components.codec.widget.common.CodecSelect
import fr.hardel.asset_editor.client.compose.components.codec.widget.common.FieldRowHeight
import fr.hardel.asset_editor.client.compose.components.codec.widget.common.RegistryCommandPalette
import fr.hardel.asset_editor.client.compose.components.codec.widget.common.RegistryPickerMode
import fr.hardel.asset_editor.client.compose.components.codec.widget.common.RemoveIconButton
import fr.hardel.asset_editor.client.compose.components.codec.widget.common.SelectOption
import fr.hardel.asset_editor.data.codec.CodecWidget
import net.minecraft.client.resources.language.I18n
import net.minecraft.resources.Identifier

private enum class HolderSetMode { TAG, LIST }

private val modeShape = RoundedCornerShape(0.dp)
private val trailingShape = RoundedCornerShape(topEnd = CodecTokens.Radius, bottomEnd = CodecTokens.Radius)

@Composable
fun HolderSetHead(
    widget: CodecWidget.HolderSetWidget,
    value: JsonElement?,
    onValueChange: (JsonElement) -> Unit,
    modifier: Modifier = Modifier
) {
    val mode = detectMode(value)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(0.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        ModeSelect(
            mode = mode,
            onChange = { next ->
                if (next == mode) return@ModeSelect
                onValueChange(if (next == HolderSetMode.TAG) JsonPrimitive("#") else JsonArray())
            }
        )

        Box(modifier = Modifier.weight(1f)) {
            when (mode) {
                HolderSetMode.TAG -> TagModeTrigger(
                    registry = widget.registry(),
                    value = value,
                    onValueChange = onValueChange
                )
                HolderSetMode.LIST -> ListModeAddButton(
                    registry = widget.registry(),
                    value = value,
                    onValueChange = onValueChange
                )
            }
        }
    }
}

@Composable
fun HolderSetBody(
    widget: CodecWidget.HolderSetWidget,
    value: JsonElement?,
    onValueChange: (JsonElement) -> Unit,
    modifier: Modifier = Modifier
) {
    val ids = remember(value) { holderSetIds(value) }
    if (ids.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CodecTokens.Gap)
    ) {
        ids.forEachIndexed { index, id ->
            key(index) {
                IdentifierRow(
                    id = id,
                    registry = widget.registry(),
                    onChange = { newId ->
                        if (newId == id) return@IdentifierRow
                        val next = JsonArray()
                        ids.forEachIndexed { i, item ->
                            next.add(if (i == index) newId.toString() else item.toString())
                        }
                        onValueChange(next)
                    },
                    onRemove = {
                        val next = JsonArray()
                        ids.forEachIndexed { i, item -> if (i != index) next.add(item.toString()) }
                        onValueChange(next)
                    }
                )
            }
        }
    }
}

@Composable
private fun ModeSelect(mode: HolderSetMode, onChange: (HolderSetMode) -> Unit) {
    val options = remember {
        listOf(
            SelectOption(HolderSetMode.TAG.name, I18n.get("codec:holder_set.mode.tag")),
            SelectOption(HolderSetMode.LIST.name, I18n.get("codec:holder_set.mode.list"))
        )
    }
    CodecSelect(
        options = options,
        selected = mode.name,
        onSelect = { picked -> onChange(HolderSetMode.valueOf(picked)) },
        modifier = Modifier.width(96.dp),
        shape = modeShape
    )
}

@Composable
private fun TagModeTrigger(
    registry: Identifier,
    value: JsonElement?,
    onValueChange: (JsonElement) -> Unit
) {
    val currentTag = remember(value) {
        val raw = runCatching { value?.asString }.getOrNull().orEmpty()
        if (raw.startsWith("#")) Identifier.tryParse(raw.removePrefix("#")) else null
    }
    var pickerOpen by remember { mutableStateOf(false) }

    Box {
        TriggerSurface(
            label = currentTag?.let { "#$it" } ?: I18n.get("codec:holder_set.tag.pick"),
            hasValue = currentTag != null,
            onClick = { pickerOpen = true }
        )
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
private fun ListModeAddButton(
    registry: Identifier,
    value: JsonElement?,
    onValueChange: (JsonElement) -> Unit
) {
    val ids = remember(value) { holderSetIds(value) }
    var pickerOpen by remember { mutableStateOf(false) }

    Box {
        AddFieldButton(
            label = I18n.get("codec:holder_set.list.add"),
            onClick = { pickerOpen = true },
            modifier = Modifier.fillMaxWidth(),
            shape = trailingShape
        )
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
private fun TriggerSurface(label: String, hasValue: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val border by animateColorAsState(
        targetValue = if (hovered) CodecTokens.BorderStrong else CodecTokens.Border,
        animationSpec = StudioMotion.hoverSpec(),
        label = "holder-set-trigger-border"
    )
    val bg by animateColorAsState(
        targetValue = if (hovered) CodecTokens.HoverBg else CodecTokens.InputBg,
        animationSpec = StudioMotion.hoverSpec(),
        label = "holder-set-trigger-bg"
    )

    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = Modifier
            .fillMaxWidth()
            .height(FieldRowHeight)
            .clip(trailingShape)
            .background(bg, trailingShape)
            .border(1.dp, border, trailingShape)
            .hoverable(interaction)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = CodecTokens.PaddingX)
    ) {
        Text(
            text = label,
            style = StudioTypography.regular(13).copy(fontFamily = FontFamily.Monospace),
            color = if (hasValue) CodecTokens.Text else CodecTokens.TextMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun IdentifierRow(
    id: Identifier,
    registry: Identifier,
    onChange: (Identifier) -> Unit,
    onRemove: () -> Unit
) {
    val rowShape = RoundedCornerShape(CodecTokens.Radius)
    var pickerOpen by remember { mutableStateOf(false) }
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val border by animateColorAsState(
        targetValue = if (hovered) CodecTokens.BorderStrong else CodecTokens.Border,
        animationSpec = StudioMotion.hoverSpec(),
        label = "id-row-border"
    )
    val bg by animateColorAsState(
        targetValue = if (hovered) CodecTokens.HoverBg else CodecTokens.InputBg,
        animationSpec = StudioMotion.hoverSpec(),
        label = "id-row-bg"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CodecTokens.Gap),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.weight(1f)) {
            Box(
                contentAlignment = Alignment.CenterStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(FieldRowHeight)
                    .clip(rowShape)
                    .background(bg, rowShape)
                    .border(1.dp, border, rowShape)
                    .hoverable(interaction)
                    .pointerHoverIcon(PointerIcon.Hand)
                    .clickable(interactionSource = interaction, indication = null, onClick = { pickerOpen = true })
                    .padding(horizontal = CodecTokens.PaddingX)
            ) {
                Text(
                    text = id.toString(),
                    style = StudioTypography.regular(13).copy(fontFamily = FontFamily.Monospace),
                    color = CodecTokens.Text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            RegistryCommandPalette(
                visible = pickerOpen,
                registryId = registry,
                mode = RegistryPickerMode.ELEMENTS,
                selected = id,
                onPick = { picked ->
                    pickerOpen = false
                    onChange(picked)
                },
                onDismiss = { pickerOpen = false }
            )
        }
        RemoveIconButton(onClick = onRemove)
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
