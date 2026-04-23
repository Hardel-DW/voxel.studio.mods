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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioMotion
import fr.hardel.asset_editor.client.compose.StudioTranslation
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.ui.CommandPalette
import fr.hardel.asset_editor.client.compose.components.ui.CommandPaletteEmpty
import fr.hardel.asset_editor.client.compose.components.ui.CommandPaletteItem
import fr.hardel.asset_editor.client.compose.components.ui.SvgIcon
import net.minecraft.client.Minecraft
import net.minecraft.client.resources.language.I18n
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey

enum class RegistryPickerMode { ELEMENTS, TAGS }

private val triggerShape = RoundedCornerShape(8.dp)
private val CHEVRON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/chevron-down.svg")

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
            .height(32.dp)
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
            style = StudioTypography.regular(13).copy(
                fontFamily = if (label != null) FontFamily.Monospace else FontFamily.Default
            ),
            color = if (label != null) StudioColors.Zinc100 else StudioColors.Zinc600,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        SvgIcon(CHEVRON, 12.dp, tint = StudioColors.Zinc500, modifier = Modifier.rotate(-90f))
    }
}

@Composable
fun RegistryPicker(
    registryId: Identifier,
    mode: RegistryPickerMode,
    selected: Identifier?,
    onPick: (Identifier) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val ids = remember(registryId, mode) { enumerateRegistry(registryId, mode) }
    val registryKey = remember(registryId) { ResourceKey.createRegistryKey<Any>(registryId) }

    val enriched = remember(ids, registryKey) {
        ids.map { id -> id to StudioTranslation.resolve(registryKey, id) }
    }
    val filtered = remember(enriched, query) {
        if (query.isBlank()) enriched
        else {
            val needle = query.lowercase()
            enriched.filter { (id, name) ->
                name.lowercase().contains(needle) || id.toString().lowercase().contains(needle)
            }
        }
    }

    CommandPalette(
        visible = true,
        onDismiss = onDismiss,
        value = query,
        onValueChange = { query = it },
        title = I18n.get(if (mode == RegistryPickerMode.TAGS) "recipe:components.picker.tags" else "recipe:components.picker.elements"),
        placeholder = I18n.get("recipe:components.picker.search")
    ) {
        if (filtered.isEmpty()) {
            CommandPaletteEmpty(I18n.get("recipe:components.picker.empty"))
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                items(items = filtered, key = { it.first.toString() }) { (id, name) ->
                    CommandPaletteItem(
                        label = name,
                        description = id.toString(),
                        onClick = { onPick(id) },
                        key = id,
                        leading = if (id == selected) {
                            { Box(Modifier.width(6.dp).height(6.dp).clip(RoundedCornerShape(3.dp)).background(StudioColors.Violet500)) }
                        } else null
                    )
                }
            }
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
