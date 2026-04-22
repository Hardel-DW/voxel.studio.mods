package fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import com.google.gson.JsonElement
import com.mojang.serialization.JsonOps
import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioMotion
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.ui.CollapsibleSection
import fr.hardel.asset_editor.client.compose.components.ui.SvgIcon
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import fr.hardel.asset_editor.client.compose.lib.rememberCurrentRegistryEntry
import fr.hardel.asset_editor.client.compose.lib.rememberServerData
import fr.hardel.asset_editor.client.memory.core.ClientWorkspaceRegistries
import fr.hardel.asset_editor.client.memory.core.StudioDataSlots
import fr.hardel.asset_editor.data.component.StudioComponentTypeDef
import fr.hardel.asset_editor.workspace.action.EditorAction
import fr.hardel.asset_editor.workspace.action.recipe.RemoveResultComponentAction
import fr.hardel.asset_editor.workspace.action.recipe.SetResultComponentAction
import fr.hardel.asset_editor.workspace.action.recipe.adapter.RecipeAdapterRegistry
import net.minecraft.client.Minecraft
import net.minecraft.client.resources.language.I18n
import net.minecraft.core.component.DataComponentType
import net.minecraft.core.component.TypedDataComponent
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.world.item.crafting.Recipe

private val buttonShape = RoundedCornerShape(10.dp)
private val PLUS = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/plus.svg")

private data class PatchEntry(
    val componentId: Identifier,
    val type: DataComponentType<*>,
    val valueJson: JsonElement?
)

@Composable
fun ResultComponentsSection(
    context: StudioContext,
    onAction: (EditorAction<*>) -> Unit,
    modifier: Modifier = Modifier
) {
    val workspaceEntry = rememberCurrentRegistryEntry(context, ClientWorkspaceRegistries.RECIPE)
    val recipe = workspaceEntry?.data ?: return
    if (!RecipeAdapterRegistry.supportsResultComponents(recipe)) return

    val definitions = rememberServerData(StudioDataSlots.COMPONENT_TYPES)
    val defsById = remember(definitions) { definitions.associateBy { it.id() } }
    val transientIds = remember(definitions) { buildTransientIds(definitions) }

    val entries = remember(recipe) { extractPatchEntries(recipe) }
    val patchIds = remember(entries) { entries.map { it.componentId }.toSet() }

    val pendingIds = remember { mutableStateListOf<Identifier>() }
    LaunchedEffect(patchIds) {
        pendingIds.removeAll(patchIds)
    }

    var addModalVisible by remember { mutableStateOf(false) }

    CollapsibleSection(
        title = I18n.get("recipe:components.title"),
        subtitle = I18n.get("recipe:components.description"),
        modifier = modifier,
        initiallyExpanded = entries.isNotEmpty()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            entries.forEach { entry ->
                key(entry.componentId.toString()) {
                    ExistingRow(entry = entry, defsById = defsById, onAction = onAction)
                }
            }

            pendingIds.forEach { pendingId ->
                if (pendingId !in patchIds) {
                    key("pending-$pendingId") {
                        PendingRow(
                            pendingId = pendingId,
                            def = defsById[pendingId],
                            type = BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(pendingId),
                            onAction = onAction,
                            onCancel = { pendingIds.remove(pendingId) }
                        )
                    }
                }
            }

            AddButton(onClick = { addModalVisible = true })
        }
    }

    val excluded = remember(patchIds, pendingIds.toList(), transientIds) {
        patchIds + pendingIds.toList() + transientIds
    }

    AddComponentModal(
        visible = addModalVisible,
        onDismiss = { addModalVisible = false },
        definitions = definitions,
        excludedIds = excluded,
        onPick = { def ->
            if (def.id() !in pendingIds) pendingIds.add(def.id())
        }
    )
}

@Composable
private fun ExistingRow(
    entry: PatchEntry,
    defsById: Map<Identifier, StudioComponentTypeDef>,
    onAction: (EditorAction<*>) -> Unit
) {
    val componentId = entry.componentId
    val type = entry.type
    val widget = defsById[componentId]?.widget()
    ResultComponentRow(
        componentId = componentId,
        widget = widget,
        initialValue = entry.valueJson,
        isPending = false,
        validate = { json -> validate(type, json) },
        onSave = { json -> onAction(SetResultComponentAction(componentId, json.toString())) },
        onDelete = { onAction(RemoveResultComponentAction(componentId)) }
    )
}

@Composable
private fun PendingRow(
    pendingId: Identifier,
    def: StudioComponentTypeDef?,
    type: DataComponentType<*>?,
    onAction: (EditorAction<*>) -> Unit,
    onCancel: () -> Unit
) {
    if (def == null || type == null) return
    val widget = def.widget()
    val initial = remember(widget) { defaultJsonFor(widget) }
    ResultComponentRow(
        componentId = pendingId,
        widget = widget,
        initialValue = initial,
        isPending = true,
        validate = { json -> validate(type, json) },
        onSave = { json -> onAction(SetResultComponentAction(pendingId, json.toString())) },
        onDelete = onCancel
    )
}

@Composable
private fun AddButton(onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val borderColor by animateColorAsState(
        targetValue = if (hovered) StudioColors.Zinc700 else StudioColors.Zinc800,
        animationSpec = StudioMotion.hoverSpec(),
        label = "add-component-border"
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(buttonShape)
            .background(StudioColors.Zinc900.copy(alpha = 0.25f), buttonShape)
            .border(1.dp, borderColor, buttonShape)
            .hoverable(interaction)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        SvgIcon(
            location = PLUS,
            size = 14.dp,
            tint = if (hovered) StudioColors.Zinc200 else StudioColors.Zinc400
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = I18n.get("recipe:components.add"),
            style = StudioTypography.medium(13),
            color = if (hovered) StudioColors.Zinc100 else StudioColors.Zinc300
        )
    }
}

private fun extractPatchEntries(recipe: Recipe<*>): List<PatchEntry> {
    val result = RecipeAdapterRegistry.extractResult(recipe)
    if (result.isEmpty) return emptyList()
    val out = mutableListOf<PatchEntry>()
    for (entry in result.componentsPatch.entrySet()) {
        val type = entry.key
        val id = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(type) ?: continue
        val value = entry.value.orElse(null)
        out.add(PatchEntry(id, type, value?.let { encodeToJson(type, it) }))
    }
    return out
}

private fun encodeToJson(type: DataComponentType<*>, value: Any): JsonElement? {
    if (type.codec() == null) return null
    val typed = TypedDataComponent.createUnchecked(type, value)
    return typed.encodeValue(JsonOps.INSTANCE).result().orElse(null)
}

private fun validate(type: DataComponentType<*>, json: JsonElement): Boolean {
    val codec = type.codec() ?: return false
    val registries = Minecraft.getInstance().connection?.registryAccess() ?: return false
    val ops = registries.createSerializationContext(JsonOps.INSTANCE)
    return codec.parse(ops, json).isSuccess
}

private fun buildTransientIds(defs: List<StudioComponentTypeDef>): Set<Identifier> {
    val out = mutableSetOf<Identifier>()
    for (def in defs) {
        val type = BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(def.id())
        if (type != null && type.codec() == null) out.add(def.id())
    }
    return out
}
