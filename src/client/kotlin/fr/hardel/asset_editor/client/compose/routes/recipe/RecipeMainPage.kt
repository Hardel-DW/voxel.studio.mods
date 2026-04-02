package fr.hardel.asset_editor.client.compose.routes.recipe

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.components.page.recipe.RecipeInventory
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.PaintMode
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.RecipeEditorDispatch
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.utils.RecipeEditorState
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.utils.placeholderRecipeVisual
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.utils.rememberRecipeEntries
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.utils.rememberRecipeEntry
import fr.hardel.asset_editor.client.compose.lib.RegistryPageDialogs
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import fr.hardel.asset_editor.client.compose.lib.data.RecipeTreeData
import fr.hardel.asset_editor.client.compose.lib.dispatchRegistryAction
import fr.hardel.asset_editor.client.compose.lib.rememberCurrentElementDestination
import fr.hardel.asset_editor.client.compose.lib.rememberCurrentRegistryEntry
import fr.hardel.asset_editor.client.compose.lib.rememberRegistryDialogState
import fr.hardel.asset_editor.workspace.action.recipe.RecipeEditorActions
import fr.hardel.asset_editor.workspace.action.recipe.adapter.RecipeAdapterRegistry
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier

@Composable
fun RecipeMainPage(context: StudioContext) {
    val conceptId = context.studioConceptId(Registries.RECIPE) ?: return
    val editor = rememberCurrentElementDestination(context, conceptId)
    val entries = rememberRecipeEntries(context)
    val runtimeEntry = rememberRecipeEntry(context, editor?.elementId)
    val workspaceEntry = rememberCurrentRegistryEntry(context, Registries.RECIPE)
    val dialogs = rememberRegistryDialogState()
    val fallback = remember { placeholderRecipeVisual("minecraft:crafting_shaped") }

    val model = runtimeEntry?.visual ?: fallback
    val recipe = workspaceEntry?.data
    val resultItemEditable = recipe?.let(RecipeAdapterRegistry::supportsResultItem) == true
    val targetId = runtimeEntry?.id ?: editor?.elementId?.let(Identifier::tryParse)
    var selectedItemId by remember(editor?.elementId) { mutableStateOf<String?>(null) }
    var search by remember(editor?.elementId) { mutableStateOf("") }
    var paintMode by remember { mutableStateOf(PaintMode.NONE) }

    LaunchedEffect(editor?.elementId, workspaceEntry, context.sessionMemory().worldSessionKey()) {
        if (editor?.elementId == null || workspaceEntry != null) return@LaunchedEffect
        val elementId = Identifier.tryParse(editor.elementId) ?: return@LaunchedEffect
        context.gateway.requestElementSeed(Registries.RECIPE, elementId)
    }

    val recipeCounts = remember(entries) {
        val counts = mutableMapOf<String, Int>()
        for (entryId in RecipeTreeData.getAllEntryIds(includeSpecial = true)) {
            counts[entryId] = entries.count { RecipeTreeData.canEntryHandleRecipeType(entryId, it.type) }
        }
        counts
    }

    val editorState = RecipeEditorState(
        model = model,
        recipe = recipe,
        selection = model.type,
        recipeCounts = recipeCounts,
        selectedItemId = selectedItemId,
        paintMode = paintMode,
        resultCountEnabled = model.resultCountEditable && model.resultCountMax > 1,
        onSelectionChange = { newSelection ->
            val nextType = if (newSelection == "minecraft:barrier") {
                RecipeTreeData.getAllRecipeTypes().firstOrNull() ?: model.type
            } else if (!RecipeTreeData.isEntryId(newSelection)) {
                newSelection
            } else {
                RecipeTreeData.getEntryConfig(newSelection)?.recipeTypes?.firstOrNull()?.toString() ?: model.type
            }

            if (nextType != model.type && targetId != null && workspaceEntry != null) {
                context.dispatchRegistryAction(
                    registry = Registries.RECIPE,
                    target = targetId,
                    action = RecipeEditorActions.ConvertRecipeType(Identifier.parse(nextType), true),
                    dialogs = dialogs
                )
            }
        },
        onResultCountChange = { value ->
            if (!model.resultCountEditable || targetId == null || workspaceEntry == null) return@RecipeEditorState
            context.dispatchRegistryAction(
                registry = Registries.RECIPE,
                target = targetId,
                action = RecipeEditorActions.SetResultCount(value),
                dialogs = dialogs
            )
        },
        onResultItemChange = {
            val itemId = selectedItemId?.let(Identifier::tryParse) ?: return@RecipeEditorState
            if (targetId == null || workspaceEntry == null || !resultItemEditable) return@RecipeEditorState
            context.dispatchRegistryAction(
                registry = Registries.RECIPE,
                target = targetId,
                action = RecipeEditorActions.SetResultItem(itemId),
                dialogs = dialogs
            )
        },
        onAction = { action ->
            if (targetId == null || workspaceEntry == null) return@RecipeEditorState
            if (action is RecipeEditorActions.AddIngredient ||
                action is RecipeEditorActions.AddShapelessIngredient) {
                paintMode = PaintMode.PAINTING
            } else if (action is RecipeEditorActions.RemoveIngredient) {
                paintMode = PaintMode.ERASING
            }
            context.dispatchRegistryAction(
                registry = Registries.RECIPE,
                target = targetId,
                action = action,
                dialogs = dialogs
            )
        }
    )

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
            modifier = Modifier.fillMaxSize()
        ) {
            RecipeEditorDispatch(
                state = editorState,
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
