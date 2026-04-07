package fr.hardel.asset_editor.client.compose.routes.recipe

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.components.page.recipe.RecipeInventory
import fr.hardel.asset_editor.client.compose.components.page.recipe.RecipeTreeData
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.PaintMode
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.RecipeEditorDispatch
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.utils.RecipeEditorState
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.utils.placeholderRecipeVisual
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.utils.rememberRecipeEntries
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.utils.rememberRecipeEntry
import fr.hardel.asset_editor.client.compose.lib.*
import fr.hardel.asset_editor.client.memory.session.server.ClientWorkspaceRegistries
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
                    workspace = ClientWorkspaceRegistries.RECIPE,
                    target = targetId,
                    action = ConvertRecipeTypeAction(Identifier.parse(nextType), true),
                    dialogs = dialogs
                )
            }
        },
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
