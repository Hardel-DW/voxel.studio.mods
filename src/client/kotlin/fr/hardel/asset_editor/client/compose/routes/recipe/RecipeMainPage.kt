package fr.hardel.asset_editor.client.compose.routes.recipe

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.components.page.recipe.RecipeTreeData
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.PaintMode
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.RecipeEditorDispatch
import fr.hardel.asset_editor.client.compose.components.page.recipe.utils.RecipeEditorState
import fr.hardel.asset_editor.client.compose.components.page.recipe.utils.RecipePageState
import fr.hardel.asset_editor.client.compose.components.page.recipe.utils.placeholderRecipeVisual
import fr.hardel.asset_editor.client.compose.components.page.recipe.utils.rememberRecipeEntries
import fr.hardel.asset_editor.client.compose.components.page.recipe.utils.rememberRecipeEntry
import fr.hardel.asset_editor.client.compose.lib.*
import fr.hardel.asset_editor.client.memory.core.ClientWorkspaceRegistries
import fr.hardel.asset_editor.workspace.action.recipe.AddIngredientAction
import fr.hardel.asset_editor.workspace.action.recipe.AddShapelessIngredientAction
import fr.hardel.asset_editor.workspace.action.recipe.ConvertRecipeTypeAction
import fr.hardel.asset_editor.workspace.action.recipe.RemoveIngredientAction
import fr.hardel.asset_editor.workspace.action.recipe.SetResultCountAction
import fr.hardel.asset_editor.workspace.action.recipe.SetResultItemAction
import fr.hardel.asset_editor.workspace.action.recipe.adapter.RecipeAdapterRegistry
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier

@Composable
fun RecipeMainPage(context: StudioContext) {
    val conceptId = context.studioConceptId(Registries.RECIPE) ?: return
    val editor = rememberCurrentElementDestination(context, conceptId)
    val entries = rememberRecipeEntries(context)
    val runtimeEntry = rememberRecipeEntry(context, editor?.elementId)
    val workspaceEntry = rememberCurrentRegistryEntry(context, ClientWorkspaceRegistries.RECIPE)
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
        context.gateway.requestElementSeed(ClientWorkspaceRegistries.RECIPE, elementId)
    }

    val recipeCounts = remember(entries) {
        val counts = mutableMapOf<String, Int>()
        for (entryId in RecipeTreeData.getAllEntryIds(includeSpecial = true)) {
            counts[entryId] = entries.count { RecipeTreeData.canEntryHandleRecipeType(entryId, it.type) }
        }
        counts
    }

    val onSelectionChange: (String) -> Unit = { newSelection ->
        val nextType = if (newSelection == "minecraft:barrier") {
            RecipeTreeData.getAllRecipeTypes().firstOrNull() ?: model.type
        } else if (!RecipeTreeData.isEntryId(newSelection)) {
            newSelection
        } else {
            RecipeTreeData.getEntryConfig(newSelection)?.recipeTypes?.firstOrNull()?.toString() ?: model.type
        }

        if (nextType != model.type && targetId != null && workspaceEntry != null) {
            context.dispatchRegistryAction(
                workspace = ClientWorkspaceRegistries.RECIPE,
                target = targetId,
                action = ConvertRecipeTypeAction(Identifier.parse(nextType), true),
                dialogs = dialogs
            )
        }
    }

    val editorState = RecipeEditorState(
        model = model,
        recipe = recipe,
        selectedItemId = selectedItemId,
        paintMode = paintMode,
        resultCountEnabled = model.resultCountEditable && model.resultCountMax > 1,
        onResultCountChange = { value ->
            if (!model.resultCountEditable || targetId == null || workspaceEntry == null) return@RecipeEditorState
            context.dispatchRegistryAction(
                workspace = ClientWorkspaceRegistries.RECIPE,
                target = targetId,
                action = SetResultCountAction(value),
                dialogs = dialogs
            )
        },
        onResultItemChange = {
            val itemId = selectedItemId?.let(Identifier::tryParse) ?: return@RecipeEditorState
            if (targetId == null || workspaceEntry == null || !resultItemEditable) return@RecipeEditorState
            context.dispatchRegistryAction(
                workspace = ClientWorkspaceRegistries.RECIPE,
                target = targetId,
                action = SetResultItemAction(itemId),
                dialogs = dialogs
            )
        },
        onAction = { action ->
            if (targetId == null || workspaceEntry == null) return@RecipeEditorState
            if (action is AddIngredientAction ||
                action is AddShapelessIngredientAction) {
                paintMode = PaintMode.PAINTING
            } else if (action is RemoveIngredientAction) {
                paintMode = PaintMode.ERASING
            }
            context.dispatchRegistryAction(
                workspace = ClientWorkspaceRegistries.RECIPE,
                target = targetId,
                action = action,
                dialogs = dialogs
            )
        }
    )

    val pageState = RecipePageState(
        editor = editorState,
        context = context,
        recipeCounts = recipeCounts,
        onSelectionChange = onSelectionChange,
        search = search,
        onSearchChange = { search = it },
        onSelectItem = { itemId -> selectedItemId = if (selectedItemId == itemId) null else itemId },
        onPaintReset = { paintMode = PaintMode.NONE }
    )

    Box(modifier = Modifier.fillMaxSize().padding(32.dp)) {
        RecipeEditorDispatch(state = pageState, modifier = Modifier.fillMaxSize())
        RegistryPageDialogs(context, dialogs)
    }
}
