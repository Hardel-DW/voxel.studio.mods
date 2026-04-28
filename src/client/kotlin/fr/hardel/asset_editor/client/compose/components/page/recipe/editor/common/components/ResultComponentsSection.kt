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
import androidx.compose.runtime.mutableStateMapOf
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
import fr.hardel.asset_editor.client.compose.components.mcdoc.McdocDefaults
import fr.hardel.asset_editor.client.compose.components.ui.CollapsibleSection
import fr.hardel.asset_editor.client.compose.components.ui.SvgIcon
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import fr.hardel.asset_editor.client.compose.lib.rememberCurrentRegistryEntry
import fr.hardel.asset_editor.client.memory.core.ClientWorkspaceRegistries
import fr.hardel.asset_editor.client.mcdoc.McdocService
import fr.hardel.asset_editor.client.mcdoc.ast.McdocType
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
private const val DATA_COMPONENT_REGISTRY = "minecraft:data_component"

private data class PatchEntry(
    val componentId: Identifier,
    val type: DataComponentType<*>,
    val valueJson: JsonElement?
)

private sealed interface RowSource {
    val componentId: Identifier
    data class Existing(val entry: PatchEntry) : RowSource {
        override val componentId: Identifier get() = entry.componentId
    }
    data class Pending(override val componentId: Identifier) : RowSource
}

@Composable
fun ResultComponentsSection(
    context: StudioContext,
    onAction: (EditorAction<*>) -> Unit,
    modifier: Modifier = Modifier
) {
    val workspaceEntry = rememberCurrentRegistryEntry(context, ClientWorkspaceRegistries.RECIPE)
    val recipe = workspaceEntry?.data ?: return
    if (!RecipeAdapterRegistry.supportsResultComponents(recipe)) return

    val componentIds = remember { availableComponentIds() }
    val transientIds = remember { unsupportedComponentIds(componentIds) }

    val entries = remember(recipe) { extractPatchEntries(recipe) }
    val patchIds = remember(entries) { entries.map { it.componentId }.toSet() }

    val pendingIds = remember { mutableStateListOf<Identifier>() }
    LaunchedEffect(patchIds) {
        pendingIds.removeAll(patchIds)
    }

    val expandedMap = remember { mutableStateMapOf<Identifier, Boolean>() }
    var addModalVisible by remember { mutableStateOf(false) }

    val rows = remember(entries, pendingIds.toList(), patchIds) {
        val out = mutableListOf<RowSource>()
        for (entry in entries) out.add(RowSource.Existing(entry))
        for (id in pendingIds) {
            if (id !in patchIds) out.add(RowSource.Pending(id))
        }
        out
    }

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
            rows.forEach { row ->
                key(row.componentId.toString()) {
                    UnifiedRow(
                        row = row,
                        expanded = expandedMap[row.componentId] ?: (row is RowSource.Pending),
                        onExpandChange = { expandedMap[row.componentId] = it },
                        onAction = onAction,
                        onCancelPending = {
                            pendingIds.remove(row.componentId)
                            expandedMap.remove(row.componentId)
                        }
                    )
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
        componentIds = componentIds,
        excludedIds = excluded,
        onPick = { id ->
            if (id !in pendingIds) {
                pendingIds.add(id)
                expandedMap[id] = true
            }
        }
    )
}

@Composable
private fun UnifiedRow(
    row: RowSource,
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    onAction: (EditorAction<*>) -> Unit,
    onCancelPending: () -> Unit
) {
    val id = row.componentId
    val componentType = remember(id) { typeForComponent(id) }
    val type = when (row) {
        is RowSource.Existing -> row.entry.type
        is RowSource.Pending -> BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(id)
    } ?: return

    val isPending = row is RowSource.Pending
    val initialValue = when (row) {
        is RowSource.Existing -> row.entry.valueJson
        is RowSource.Pending -> componentType?.let { McdocDefaults.defaultFor(it) }
    }

    ResultComponentRow(
        componentId = id,
        componentType = componentType,
        initialValue = initialValue,
        isPending = isPending,
        expanded = expanded,
        onExpandChange = onExpandChange,
        validate = { json -> validate(type, json) },
        onSave = { json -> onAction(SetResultComponentAction(id, json.toString())) },
        onDelete = {
            if (isPending) onCancelPending()
            else onAction(RemoveResultComponentAction(id))
        }
    )
}

@Composable
private fun AddButton(onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val borderColor by animateColorAsState(
        targetValue = if (hovered) StudioColors.Emerald400.copy(alpha = 0.42f) else StudioColors.Zinc800.copy(alpha = 0.42f),
        animationSpec = StudioMotion.hoverSpec(),
        label = "add-component-border"
    )
    val background by animateColorAsState(
        targetValue = if (hovered) StudioColors.Emerald400.copy(alpha = 0.08f) else StudioColors.Zinc900.copy(alpha = 0.22f),
        animationSpec = StudioMotion.hoverSpec(),
        label = "add-component-bg"
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(buttonShape)
            .background(background, buttonShape)
            .border(1.dp, borderColor, buttonShape)
            .hoverable(interaction)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        SvgIcon(
            location = PLUS,
            size = 14.dp,
            tint = if (hovered) StudioColors.Emerald400 else StudioColors.Zinc300
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = I18n.get("recipe:components.add"),
            style = StudioTypography.medium(13),
            color = if (hovered) StudioColors.Zinc50 else StudioColors.Zinc300
        )
    }
}

private fun availableComponentIds(): List<Identifier> {
    val keys = McdocService.current().dispatch().entries(DATA_COMPONENT_REGISTRY).keys
    return keys.asSequence()
        .filter { !it.startsWith("%") }
        .mapNotNull { dispatchKeyToIdentifier(it) }
        .toList()
}

private fun dispatchKeyToIdentifier(key: String): Identifier? {
    if (key.contains(':')) return Identifier.tryParse(key)
    return Identifier.tryParse("minecraft:$key")
}

private fun typeForComponent(id: Identifier): McdocType? =
    McdocService.current().dispatch()
        .resolve(DATA_COMPONENT_REGISTRY, id.path)
        .map { it.target() }
        .orElse(null)

private fun unsupportedComponentIds(componentIds: List<Identifier>): Set<Identifier> {
    val out = mutableSetOf<Identifier>()
    for (id in componentIds) {
        val type = BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(id)
        if (type != null && type.codec() == null) out.add(id)
    }
    return out
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
