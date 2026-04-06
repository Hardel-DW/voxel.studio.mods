package fr.hardel.asset_editor.workspace.action;

import fr.hardel.asset_editor.workspace.action.enchantment.SetExclusiveSetAction;
import fr.hardel.asset_editor.workspace.action.enchantment.SetIntFieldAction;
import fr.hardel.asset_editor.workspace.action.enchantment.SetModeAction;
import fr.hardel.asset_editor.workspace.action.enchantment.SetPrimaryItemsAction;
import fr.hardel.asset_editor.workspace.action.enchantment.SetSupportedItemsAction;
import fr.hardel.asset_editor.workspace.action.enchantment.ToggleDisabledAction;
import fr.hardel.asset_editor.workspace.action.enchantment.ToggleDisabledEffectAction;
import fr.hardel.asset_editor.workspace.action.enchantment.ToggleExclusiveAction;
import fr.hardel.asset_editor.workspace.action.enchantment.ToggleSlotAction;
import fr.hardel.asset_editor.workspace.action.enchantment.ToggleTagAction;
import fr.hardel.asset_editor.workspace.action.recipe.AddIngredientAction;
import fr.hardel.asset_editor.workspace.action.recipe.AddShapelessIngredientAction;
import fr.hardel.asset_editor.workspace.action.recipe.ConvertRecipeTypeAction;
import fr.hardel.asset_editor.workspace.action.recipe.RemoveIngredientAction;
import fr.hardel.asset_editor.workspace.action.recipe.RemoveItemEverywhereAction;
import fr.hardel.asset_editor.workspace.action.recipe.ReplaceItemEverywhereAction;
import fr.hardel.asset_editor.workspace.action.recipe.SetCategoryAction;
import fr.hardel.asset_editor.workspace.action.recipe.SetCookingExperienceAction;
import fr.hardel.asset_editor.workspace.action.recipe.SetCookingTimeAction;
import fr.hardel.asset_editor.workspace.action.recipe.SetGroupAction;
import fr.hardel.asset_editor.workspace.action.recipe.SetResultCountAction;
import fr.hardel.asset_editor.workspace.action.recipe.SetResultItemAction;
import fr.hardel.asset_editor.workspace.action.recipe.SetShowNotificationAction;

public final class Actions {

    public static void register() {
        EditorActionRegistry.register(SetIntFieldAction.TYPE);
        EditorActionRegistry.register(SetModeAction.TYPE);
        EditorActionRegistry.register(ToggleDisabledAction.TYPE);
        EditorActionRegistry.register(ToggleDisabledEffectAction.TYPE);
        EditorActionRegistry.register(ToggleSlotAction.TYPE);
        EditorActionRegistry.register(ToggleTagAction.TYPE);
        EditorActionRegistry.register(ToggleExclusiveAction.TYPE);
        EditorActionRegistry.register(SetSupportedItemsAction.TYPE);
        EditorActionRegistry.register(SetPrimaryItemsAction.TYPE);
        EditorActionRegistry.register(SetExclusiveSetAction.TYPE);
        EditorActionRegistry.register(AddIngredientAction.TYPE);
        EditorActionRegistry.register(AddShapelessIngredientAction.TYPE);
        EditorActionRegistry.register(RemoveIngredientAction.TYPE);
        EditorActionRegistry.register(RemoveItemEverywhereAction.TYPE);
        EditorActionRegistry.register(ReplaceItemEverywhereAction.TYPE);
        EditorActionRegistry.register(ConvertRecipeTypeAction.TYPE);
        EditorActionRegistry.register(SetResultCountAction.TYPE);
        EditorActionRegistry.register(SetResultItemAction.TYPE);
        EditorActionRegistry.register(SetGroupAction.TYPE);
        EditorActionRegistry.register(SetCategoryAction.TYPE);
        EditorActionRegistry.register(SetCookingExperienceAction.TYPE);
        EditorActionRegistry.register(SetCookingTimeAction.TYPE);
        EditorActionRegistry.register(SetShowNotificationAction.TYPE);
    }

    private Actions() {}
}
