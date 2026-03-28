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
import androidx.compose.runtime.LaunchedEffect
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
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import fr.hardel.asset_editor.client.compose.lib.data.RecipeTreeData
import fr.hardel.asset_editor.client.compose.lib.data.StudioConcept
import fr.hardel.asset_editor.client.compose.lib.rememberCurrentElementDestination

private enum class PaintMode { NONE, PAINTING, ERASING }

@Composable
fun RecipeMainPage(context: StudioContext) {
    val editor = rememberCurrentElementDestination(context, StudioConcept.RECIPE)
    val entries = rememberRecipeEntries(context)
    val runtimeEntry = rememberRecipeEntry(context, editor?.elementId)
    val fallback = remember { placeholderRecipeVisual("minecraft:crafting_shaped") }
    var model by remember(editor?.elementId) { mutableStateOf(runtimeEntry?.visual ?: fallback) }
    var selection by remember(editor?.elementId) {
        mutableStateOf(RecipeTreeData.getBlockByRecipeType((runtimeEntry?.visual ?: fallback).type).blockId.toString())
    }
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

    LaunchedEffect(runtimeEntry?.id) {
        val next = runtimeEntry?.visual ?: fallback
        model = next
        selection = RecipeTreeData.getBlockByRecipeType(next.type).blockId.toString()
        selectedItemId = null
        search = ""
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
                    selection = newSelection
                    model = placeholderRecipeVisual(nextType).copy(resultCount = model.resultCount)
                },
                onResultCountChange = { value ->
                    model = model.copy(resultCount = value)
                },
                onSlotPointerDown = { slot, button ->
                    when (button) {
                        PointerButton.Primary -> {
                            model = applyItemToSlot(model, slot, selectedItemId)
                            paintMode = PaintMode.PAINTING
                        }
                        PointerButton.Secondary -> {
                            model = clearSlot(model, slot)
                            paintMode = PaintMode.ERASING
                        }
                        else -> {}
                    }
                },
                onSlotPointerEnter = { slot ->
                    when (paintMode) {
                        PaintMode.PAINTING -> model = applyItemToSlot(model, slot, selectedItemId)
                        PaintMode.ERASING -> model = clearSlot(model, slot)
                        PaintMode.NONE -> {}
                    }
                },
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
    }
}

private fun applyItemToSlot(model: RecipeVisualModel, slot: String, selectedItemId: String?): RecipeVisualModel {
    if (selectedItemId == null) return model
    val nextSlots = LinkedHashMap(model.slots)
    nextSlots[slot] = listOf(selectedItemId)
    return model.copy(slots = nextSlots)
}

private fun clearSlot(model: RecipeVisualModel, slot: String): RecipeVisualModel {
    val nextSlots = LinkedHashMap(model.slots)
    nextSlots.remove(slot)
    return model.copy(slots = nextSlots)
}
