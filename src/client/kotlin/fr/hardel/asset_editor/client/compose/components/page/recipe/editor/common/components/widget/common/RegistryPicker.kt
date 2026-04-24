package fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.components.widget.common

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioMotion
import fr.hardel.asset_editor.client.compose.StudioTranslation
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.ui.Popover
import fr.hardel.asset_editor.client.compose.components.ui.SvgIcon
import net.minecraft.client.Minecraft
import net.minecraft.client.resources.language.I18n
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey

enum class RegistryPickerMode { ELEMENTS, TAGS }

private val triggerShape = RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp)
private val searchShape = RoundedCornerShape(6.dp)
private val itemShape = RoundedCornerShape(6.dp)
private val CHEVRON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/chevron-down.svg")
private val SEARCH = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/search.svg")

@Composable
fun RegistryTrigger(
    label: String?,
    placeholder: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val border by animateColorAsState(
        targetValue = if (hovered) StudioColors.Zinc700 else StudioColors.Zinc800,
        animationSpec = StudioMotion.hoverSpec(),
        label = "registry-trigger-border"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .height(26.dp)
            .clip(triggerShape)
            .background(StudioColors.Zinc950.copy(alpha = 0.5f), triggerShape)
            .border(1.dp, border, triggerShape)
            .hoverable(interaction)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp)
    ) {
        Text(
            text = label ?: placeholder,
            style = StudioTypography.regular(12).copy(
                fontFamily = if (label != null) FontFamily.Monospace else FontFamily.Default
            ),
            color = if (label != null) StudioColors.Zinc100 else StudioColors.Zinc600,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        SvgIcon(CHEVRON, 12.dp, tint = StudioColors.Zinc500)
    }
}

/**
 * Inline dropdown picker anchored to its trigger. Renders nothing when [expanded] is false.
 * Place inside a Box that also contains the trigger so the popup anchors correctly.
 */
@Composable
fun RegistryDropdown(
    expanded: Boolean,
    registryId: Identifier,
    mode: RegistryPickerMode,
    selected: Identifier?,
    onPick: (Identifier) -> Unit,
    onDismiss: () -> Unit
) {
    if (!expanded) return

    var query by remember { mutableStateOf(TextFieldValue("")) }
    val focusRequester = remember { FocusRequester() }
    val ids = remember(registryId, mode) { enumerateRegistry(registryId, mode) }
    val registryKey = remember(registryId) { ResourceKey.createRegistryKey<Any>(registryId) }
    val enriched = remember(ids, registryKey) {
        ids.map { id -> id to StudioTranslation.resolve(registryKey, id) }
    }
    val filtered = remember(enriched, query.text) {
        if (query.text.isBlank()) enriched
        else {
            val needle = query.text.lowercase()
            enriched.filter { (id, name) ->
                name.lowercase().contains(needle) || id.toString().lowercase().contains(needle)
            }
        }
    }

    Popover(
        expanded = true,
        onDismiss = onDismiss,
        alignment = Alignment.TopStart
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.widthIn(min = 280.dp, max = 380.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .clip(searchShape)
                    .background(StudioColors.Zinc950.copy(alpha = 0.6f), searchShape)
                    .border(1.dp, StudioColors.Zinc800, searchShape)
                    .padding(horizontal = 10.dp)
            ) {
                SvgIcon(SEARCH, 12.dp, tint = StudioColors.Zinc500)
                Spacer(Modifier.width(8.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (query.text.isEmpty()) {
                        Text(
                            text = I18n.get("recipe:components.picker.search"),
                            style = StudioTypography.regular(13),
                            color = StudioColors.Zinc600
                        )
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        textStyle = StudioTypography.regular(13).copy(color = StudioColors.Zinc100),
                        cursorBrush = SolidColor(StudioColors.Zinc100),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
                    )
                }
            }

            Box(modifier = Modifier.heightIn(max = 280.dp)) {
                if (filtered.isEmpty()) {
                    Text(
                        text = I18n.get("recipe:components.picker.empty"),
                        style = StudioTypography.regular(12),
                        color = StudioColors.Zinc500,
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp)
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        items(items = filtered, key = { it.first.toString() }) { (id, name) ->
                            RegistryItem(
                                label = name,
                                identifier = id.toString(),
                                selected = id == selected,
                                onClick = { onPick(id) }
                            )
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        runCatching { focusRequester.requestFocus() }
    }
}

@Composable
private fun RegistryItem(
    label: String,
    identifier: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val bg by animateColorAsState(
        targetValue = when {
            hovered -> StudioColors.Zinc800.copy(alpha = 0.75f)
            selected -> StudioColors.Violet500.copy(alpha = 0.15f)
            else -> Color.Transparent
        },
        animationSpec = StudioMotion.hoverSpec(),
        label = "registry-item-bg"
    )
    val labelColor = if (selected) StudioColors.Violet500 else StudioColors.Zinc100

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(itemShape)
            .background(bg, itemShape)
            .hoverable(interaction)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = StudioTypography.medium(13),
                color = labelColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = identifier,
                style = StudioTypography.regular(10).copy(fontFamily = FontFamily.Monospace),
                color = StudioColors.Zinc500,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (selected) {
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(StudioColors.Violet500)
            )
        }
    }
}

private fun enumerateRegistry(registryId: Identifier, mode: RegistryPickerMode): List<Identifier> {
    val registries = Minecraft.getInstance().connection?.registryAccess() ?: return emptyList()
    val key = ResourceKey.createRegistryKey<Any>(registryId)
    val lookup = registries.lookup(key).orElse(null) ?: return emptyList()
    return when (mode) {
        RegistryPickerMode.ELEMENTS -> lookup.listElementIds().map { it.identifier() }.sorted().toList()
        RegistryPickerMode.TAGS -> lookup.listTagIds().map { it.location() }.sorted().toList()
    }
}
