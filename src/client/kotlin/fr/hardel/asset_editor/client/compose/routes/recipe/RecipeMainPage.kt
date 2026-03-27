package fr.hardel.asset_editor.client.compose.routes.recipe

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.components.page.recipe.RecipeInventory
import fr.hardel.asset_editor.client.compose.components.page.recipe.RecipeSection
import fr.hardel.asset_editor.client.compose.components.page.recipe.RecipeVisualModel
import fr.hardel.asset_editor.client.compose.components.page.recipe.placeholderRecipeVisual
import fr.hardel.asset_editor.client.compose.components.page.recipe.rememberRecipeEntry
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import fr.hardel.asset_editor.client.compose.lib.data.RecipeTreeData
import fr.hardel.asset_editor.client.compose.lib.data.StudioConcept
import fr.hardel.asset_editor.client.compose.lib.rememberCurrentElementDestination

@Composable
fun RecipeMainPage(context: StudioContext) {
    val editor = rememberCurrentElementDestination(context, StudioConcept.RECIPE)
    val runtimeEntry = rememberRecipeEntry(context, editor?.elementId)
    val fallback = remember { placeholderRecipeVisual("minecraft:crafting_shaped") }
    var model by remember(editor?.elementId) { mutableStateOf(runtimeEntry?.visual ?: fallback) }
    var selection by remember(editor?.elementId) {
        mutableStateOf(RecipeTreeData.getBlockByRecipeType((runtimeEntry?.visual ?: fallback).type).blockId.toString())
    }
    var selectedItemId by remember(editor?.elementId) { mutableStateOf<String?>(null) }
    var search by remember(editor?.elementId) { mutableStateOf("") }

    LaunchedEffect(runtimeEntry?.id) {
        val next = runtimeEntry?.visual ?: fallback
        model = next
        selection = RecipeTreeData.getBlockByRecipeType(next.type).blockId.toString()
        selectedItemId = null
        search = ""
    }

    // TSX: div.p-8.h-full.overflow-y-auto > div.grid.grid-cols-2.gap-8.items-start
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(32.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(32.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height((maxHeight - 200.dp).coerceAtLeast(520.dp))
        ) {
            RecipeSection(
                model = model,
                selection = selection,
                selectedItemId = selectedItemId,
                onSelectionChange = { newSelection ->
                    val nextType = if (newSelection == "minecraft:barrier") {
                        RecipeTreeData.getAllRecipeTypes().firstOrNull() ?: model.type
                    } else {
                        RecipeTreeData.getBlockConfig(newSelection)?.recipeTypes?.firstOrNull()?.toString() ?: model.type
                    }
                    selection = newSelection
                    model = placeholderRecipeVisual(nextType).copy(resultCount = model.resultCount)
                },
                onRecipeTypeChange = { newType ->
                    model = placeholderRecipeVisual(newType).copy(resultCount = model.resultCount)
                },
                onResultCountChange = { value ->
                    model = model.copy(resultCount = value)
                },
                onSlotClick = { slot ->
                    model = updateSlot(model, slot, selectedItemId)
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )

            RecipeInventory(
                context = context,
                search = search,
                onSearchChange = { search = it },
                selectedItemId = selectedItemId,
                onSelectItem = { itemId -> selectedItemId = itemId },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }
    }
}

private fun updateSlot(
    model: RecipeVisualModel,
    slot: String,
    selectedItemId: String?
): RecipeVisualModel {
    val nextSlots = LinkedHashMap(model.slots)
    if (selectedItemId == null) {
        nextSlots.remove(slot)
    } else {
        nextSlots[slot] = listOf(selectedItemId)
    }
    return model.copy(slots = nextSlots)
}
