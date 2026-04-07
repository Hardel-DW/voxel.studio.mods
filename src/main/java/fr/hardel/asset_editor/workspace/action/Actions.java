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

    // Enchantment
    public static final Action<?> SET_INT_FIELD = EditorActionRegistry.register("enchantment/set_int_field", SetIntFieldAction.CODEC, SetIntFieldAction.class);
    public static final Action<?> SET_MODE = EditorActionRegistry.register("enchantment/set_mode", SetModeAction.CODEC, SetModeAction.class);
    public static final Action<?> TOGGLE_DISABLED = EditorActionRegistry.register("enchantment/toggle_disabled", ToggleDisabledAction.CODEC, ToggleDisabledAction.class);
    public static final Action<?> TOGGLE_DISABLED_EFFECT = EditorActionRegistry.register("enchantment/toggle_disabled_effect", ToggleDisabledEffectAction.CODEC, ToggleDisabledEffectAction.class);
    public static final Action<?> TOGGLE_SLOT = EditorActionRegistry.register("enchantment/toggle_slot", ToggleSlotAction.CODEC, ToggleSlotAction.class);
    public static final Action<?> TOGGLE_TAG = EditorActionRegistry.register("enchantment/toggle_tag", ToggleTagAction.CODEC, ToggleTagAction.class);
    public static final Action<?> TOGGLE_EXCLUSIVE = EditorActionRegistry.register("enchantment/toggle_exclusive", ToggleExclusiveAction.CODEC, ToggleExclusiveAction.class);
    public static final Action<?> SET_SUPPORTED_ITEMS = EditorActionRegistry.register("enchantment/set_supported_items", SetSupportedItemsAction.CODEC, SetSupportedItemsAction.class);
    public static final Action<?> SET_PRIMARY_ITEMS = EditorActionRegistry.register("enchantment/set_primary_items", SetPrimaryItemsAction.CODEC, SetPrimaryItemsAction.class);
    public static final Action<?> SET_EXCLUSIVE_SET = EditorActionRegistry.register("enchantment/set_exclusive_set", SetExclusiveSetAction.CODEC, SetExclusiveSetAction.class);

    // Recipe
    public static final Action<?> ADD_INGREDIENT = EditorActionRegistry.register("recipe/add_ingredient", AddIngredientAction.CODEC, AddIngredientAction.class);
    public static final Action<?> ADD_SHAPELESS_INGREDIENT = EditorActionRegistry.register("recipe/add_shapeless_ingredient", AddShapelessIngredientAction.CODEC, AddShapelessIngredientAction.class);
    public static final Action<?> REMOVE_INGREDIENT = EditorActionRegistry.register("recipe/remove_ingredient", RemoveIngredientAction.CODEC, RemoveIngredientAction.class);
    public static final Action<?> REMOVE_ITEM_EVERYWHERE = EditorActionRegistry.register("recipe/remove_item_everywhere", RemoveItemEverywhereAction.CODEC, RemoveItemEverywhereAction.class);
    public static final Action<?> REPLACE_ITEM_EVERYWHERE = EditorActionRegistry.register("recipe/replace_item_everywhere", ReplaceItemEverywhereAction.CODEC, ReplaceItemEverywhereAction.class);
    public static final Action<?> CONVERT_RECIPE_TYPE = EditorActionRegistry.register("recipe/convert_recipe_type", ConvertRecipeTypeAction.CODEC, ConvertRecipeTypeAction.class);
    public static final Action<?> SET_RESULT_COUNT = EditorActionRegistry.register("recipe/set_result_count", SetResultCountAction.CODEC, SetResultCountAction.class);
    public static final Action<?> SET_RESULT_ITEM = EditorActionRegistry.register("recipe/set_result_item", SetResultItemAction.CODEC, SetResultItemAction.class);
    public static final Action<?> SET_GROUP = EditorActionRegistry.register("recipe/set_group", SetGroupAction.CODEC, SetGroupAction.class);
    public static final Action<?> SET_CATEGORY = EditorActionRegistry.register("recipe/set_category", SetCategoryAction.CODEC, SetCategoryAction.class);
    public static final Action<?> SET_COOKING_EXPERIENCE = EditorActionRegistry.register("recipe/set_cooking_experience", SetCookingExperienceAction.CODEC, SetCookingExperienceAction.class);
    public static final Action<?> SET_COOKING_TIME = EditorActionRegistry.register("recipe/set_cooking_time", SetCookingTimeAction.CODEC, SetCookingTimeAction.class);
    public static final Action<?> SET_SHOW_NOTIFICATION = EditorActionRegistry.register("recipe/set_show_notification", SetShowNotificationAction.CODEC, SetShowNotificationAction.class);

    public static void register() {
        // Static fields are initialized on class load — this method triggers that.
    }

    private Actions() {}
}
