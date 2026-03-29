package fr.hardel.asset_editor.client.compose.routes.recipe

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.components.page.recipe.RecipeInventory
import fr.hardel.asset_editor.client.compose.components.page.recipe.RecipeSection
import fr.hardel.asset_editor.client.compose.components.page.recipe.model.RecipeVisualModel
import fr.hardel.asset_editor.client.compose.components.page.recipe.model.placeholderRecipeVisual
import fr.hardel.asset_editor.client.compose.components.page.recipe.rememberRecipeEntries
import fr.hardel.asset_editor.client.compose.components.page.recipe.rememberRecipeEntry
import fr.hardel.asset_editor.client.compose.lib.RegistryPageDialogs
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import fr.hardel.asset_editor.client.compose.lib.data.RecipeTreeData
import fr.hardel.asset_editor.client.compose.lib.data.StudioConcept
import fr.hardel.asset_editor.client.compose.lib.dispatchRegistryAction
import fr.hardel.asset_editor.client.compose.lib.rememberCurrentElementDestination
import fr.hardel.asset_editor.client.compose.lib.rememberCurrentRegistryEntry
import fr.hardel.asset_editor.client.compose.lib.rememberRegistryDialogState
import fr.hardel.asset_editor.workspace.action.recipe.RecipeEditorActions
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.world.item.crafting.ShapelessRecipe

private enum class PaintMode { NONE, PAINTING, ERASING }

@Composable
fun RecipeMainPage(context: StudioContext) {
    val editor = rememberCurrentElementDestination(context, StudioConcept.RECIPE)
    val entries = rememberRecipeEntries(context)
    val runtimeEntry = rememberRecipeEntry(context, editor?.elementId)
    val workspaceEntry = rememberCurrentRegistryEntry(context, Registries.RECIPE)
    val dialogs = rememberRegistryDialogState()
    val fallback = remember { placeholderRecipeVisual("minecraft:crafting_shaped") }

    val model = runtimeEntry?.visual ?: fallback
    val targetId = runtimeEntry?.id ?: editor?.elementId?.let(Identifier::tryParse)
    val selection = RecipeTreeData.getBlockByRecipeType(model.type).blockId.toString()
    var selectedItemId by remember(editor?.elementId) { mutableStateOf<String?>(null) }
    var search by remember(editor?.elementId) { mutableStateOf("") }
    var paintMode by remember { mutableStateOf(PaintMode.NONE) }

    val recipeCounts = remember(entries) {
        val counts = mutableMapOf<String, Int>()
        for (blockId in RecipeTreeData.getAllBlockIds(includeSpecial = true)) {
            counts[blockId] = entries.count { RecipeTreeData.canBlockHandleRecipeType(blockId, it.type) }
        }
        counts
    }

    fun dispatchAddItem(slot: String, itemId: String) {
        val editableEntry = workspaceEntry ?: return
        val editableTargetId = targetId ?: return
        val itemIdentifier = Identifier.tryParse(itemId) ?: return
        val slotIndex = slot.toIntOrNull() ?: return

        if (editableEntry.data() is ShapelessRecipe) {
            context.dispatchRegistryAction(
                registry = Registries.RECIPE,
                target = editableTargetId,
                action = RecipeEditorActions.AddShapelessIngredient(listOf(itemIdentifier)),
                dialogs = dialogs
            )
        } else {
            context.dispatchRegistryAction(
                registry = Registries.RECIPE,
                target = editableTargetId,
                action = RecipeEditorActions.AddIngredient(slotIndex, listOf(itemIdentifier), true),
                dialogs = dialogs
            )
        }
    }

    fun dispatchClearSlot(slot: String) {
        val editableTargetId = targetId ?: return
        if (workspaceEntry == null) return
        val slotIndex = slot.toIntOrNull() ?: return

        context.dispatchRegistryAction(
            registry = Registries.RECIPE,
            target = editableTargetId,
            action = RecipeEditorActions.RemoveIngredient(slotIndex, emptyList()),
            dialogs = dialogs
        )
    }

    @OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
            .onPointerEvent(PointerEventType.Release) { paintMode = PaintMode.NONE }
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(32.dp),
            verticalAlignment = Alignment.Top,
            modifier = Modifier
                .fillMaxSize()
        ) {
            RecipeSection(
                model = model,
                selection = selection,
                recipeCounts = recipeCounts,
                onSelectionChange = { newSelection ->
                    val nextType = if (newSelection == "minecraft:barrier") {
                        RecipeTreeData.getAllRecipeTypes().firstOrNull() ?: model.type
                    } else {
                        RecipeTreeData.getBlockConfig(newSelection)?.recipeTypes?.firstOrNull()?.toString() ?: model.type
                    }

                    if (nextType != model.type && targetId != null && workspaceEntry != null) {
                        context.dispatchRegistryAction(
                            registry = Registries.RECIPE,
                            target = targetId,
                            action = RecipeEditorActions.ConvertRecipeType(
                                Identifier.parse(nextType), true
                            ),
                            dialogs = dialogs
                        )
                    }
                },
                onResultCountChange = { value ->
                    if (!model.resultCountEditable || targetId == null || workspaceEntry == null) return@RecipeSection
                    context.dispatchRegistryAction(
                        registry = Registries.RECIPE,
                        target = targetId,
                        action = RecipeEditorActions.SetResultCount(value),
                        dialogs = dialogs
                    )
                },
                onSlotPointerDown = { slot, button ->
                    when (button) {
                        PointerButton.Primary -> {
                            selectedItemId?.let { dispatchAddItem(slot, it) }
                            paintMode = PaintMode.PAINTING
                        }
                        PointerButton.Secondary -> {
                            dispatchClearSlot(slot)
                            paintMode = PaintMode.ERASING
                        }
                        else -> {}
                    }
                },
                onSlotPointerEnter = { slot ->
                    when (paintMode) {
                        PaintMode.PAINTING -> selectedItemId?.let { dispatchAddItem(slot, it) }
                        PaintMode.ERASING -> dispatchClearSlot(slot)
                        PaintMode.NONE -> {}
                    }
                },
                resultCountEnabled = model.resultCountEditable,
                modifier = Modifier.weight(1f)
            )

            RecipeInventory(
                context = context,
                search = search,
                onSearchChange = { search = it },
                selectedItemId = selectedItemId,
                onSelectItem = { itemId -> selectedItemId = if (selectedItemId == itemId) null else itemId },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }

        RegistryPageDialogs(context, dialogs)
    }
}
