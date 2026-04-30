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
import fr.hardel.asset_editor.workspace.action.recipe.ConvertRecipeTypeAction;
import fr.hardel.asset_editor.workspace.action.recipe.RemoveItemEverywhereAction;
import fr.hardel.asset_editor.workspace.action.recipe.RemoveResultComponentAction;
import fr.hardel.asset_editor.workspace.action.recipe.ReplaceItemEverywhereAction;
import fr.hardel.asset_editor.workspace.action.recipe.SetCraftingPatternAction;
import fr.hardel.asset_editor.workspace.action.recipe.SetResultComponentAction;
import fr.hardel.asset_editor.workspace.action.recipe.SetCategoryAction;
import fr.hardel.asset_editor.workspace.action.recipe.SetCookingExperienceAction;
import fr.hardel.asset_editor.workspace.action.recipe.SetCookingTimeAction;
import fr.hardel.asset_editor.workspace.action.recipe.SetGroupAction;
import fr.hardel.asset_editor.workspace.action.recipe.SetResultCountAction;
import fr.hardel.asset_editor.workspace.action.recipe.SetResultItemAction;
import fr.hardel.asset_editor.workspace.action.recipe.SetShowNotificationAction;
import fr.hardel.asset_editor.workspace.action.loot_table.AddEntryAction;
import fr.hardel.asset_editor.workspace.action.loot_table.BalancePoolWeightsAction;
import fr.hardel.asset_editor.workspace.action.loot_table.RemoveEntryAction;
import fr.hardel.asset_editor.workspace.action.loot_table.ReplaceEntryItemAction;
import fr.hardel.asset_editor.workspace.action.loot_table.SetEntryCountMaxAction;
import fr.hardel.asset_editor.workspace.action.loot_table.SetEntryCountMinAction;
import fr.hardel.asset_editor.workspace.action.loot_table.SetEntryWeightAction;
import fr.hardel.asset_editor.workspace.flush.Workspaces;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.storage.loot.LootTable;

public final class Actions {

    // Enchantment
    public static final Action<Enchantment, SetIntFieldAction> SET_INT_FIELD = EditorActionRegistry.register(
        Workspaces.ENCHANTMENT,
        "enchantment/set_int_field",
        SetIntFieldAction.CODEC,
        SetIntFieldAction.class);

    public static final Action<Enchantment, SetModeAction> SET_MODE = EditorActionRegistry.register(
        Workspaces.ENCHANTMENT,
        "enchantment/set_mode",
        SetModeAction.CODEC,
        SetModeAction.class);

    public static final Action<Enchantment, ToggleDisabledAction> TOGGLE_DISABLED = EditorActionRegistry.register(
        Workspaces.ENCHANTMENT,
        "enchantment/toggle_disabled",
        ToggleDisabledAction.CODEC,
        ToggleDisabledAction.class);

    public static final Action<Enchantment, ToggleDisabledEffectAction> TOGGLE_DISABLED_EFFECT = EditorActionRegistry.register(
        Workspaces.ENCHANTMENT,
        "enchantment/toggle_disabled_effect",
        ToggleDisabledEffectAction.CODEC,
        ToggleDisabledEffectAction.class);

    public static final Action<Enchantment, ToggleSlotAction> TOGGLE_SLOT = EditorActionRegistry.register(
        Workspaces.ENCHANTMENT,
        "enchantment/toggle_slot",
        ToggleSlotAction.CODEC,
        ToggleSlotAction.class);

    public static final Action<Enchantment, ToggleTagAction> TOGGLE_TAG = EditorActionRegistry.register(
        Workspaces.ENCHANTMENT,
        "enchantment/toggle_tag",
        ToggleTagAction.CODEC,
        ToggleTagAction.class);

    public static final Action<Enchantment, ToggleExclusiveAction> TOGGLE_EXCLUSIVE = EditorActionRegistry.register(
        Workspaces.ENCHANTMENT,
        "enchantment/toggle_exclusive",
        ToggleExclusiveAction.CODEC,
        ToggleExclusiveAction.class);

    public static final Action<Enchantment, SetSupportedItemsAction> SET_SUPPORTED_ITEMS = EditorActionRegistry.register(
        Workspaces.ENCHANTMENT,
        "enchantment/set_supported_items",
        SetSupportedItemsAction.CODEC,
        SetSupportedItemsAction.class);

    public static final Action<Enchantment, SetPrimaryItemsAction> SET_PRIMARY_ITEMS = EditorActionRegistry.register(
        Workspaces.ENCHANTMENT,
        "enchantment/set_primary_items",
        SetPrimaryItemsAction.CODEC,
        SetPrimaryItemsAction.class);

    public static final Action<Enchantment, SetExclusiveSetAction> SET_EXCLUSIVE_SET = EditorActionRegistry.register(
        Workspaces.ENCHANTMENT,
        "enchantment/set_exclusive_set",
        SetExclusiveSetAction.CODEC,
        SetExclusiveSetAction.class);

    // Loot Table
    public static final Action<LootTable, fr.hardel.asset_editor.workspace.action.loot_table.ToggleDisabledAction> LOOT_TOGGLE_DISABLED = EditorActionRegistry.register(
        Workspaces.LOOT_TABLE,
        "loot_table/toggle_disabled",
        fr.hardel.asset_editor.workspace.action.loot_table.ToggleDisabledAction.CODEC,
        fr.hardel.asset_editor.workspace.action.loot_table.ToggleDisabledAction.class);

    public static final Action<LootTable, AddEntryAction> LOOT_ADD_ENTRY = EditorActionRegistry.register(
        Workspaces.LOOT_TABLE,
        "loot_table/add_entry",
        AddEntryAction.CODEC,
        AddEntryAction.class);

    public static final Action<LootTable, RemoveEntryAction> LOOT_REMOVE_ENTRY = EditorActionRegistry.register(
        Workspaces.LOOT_TABLE,
        "loot_table/remove_entry",
        RemoveEntryAction.CODEC,
        RemoveEntryAction.class);

    public static final Action<LootTable, SetEntryWeightAction> LOOT_SET_ENTRY_WEIGHT = EditorActionRegistry.register(
        Workspaces.LOOT_TABLE,
        "loot_table/set_entry_weight",
        SetEntryWeightAction.CODEC,
        SetEntryWeightAction.class);

    public static final Action<LootTable, BalancePoolWeightsAction> LOOT_BALANCE_POOL_WEIGHTS = EditorActionRegistry.register(
        Workspaces.LOOT_TABLE,
        "loot_table/balance_pool_weights",
        BalancePoolWeightsAction.CODEC,
        BalancePoolWeightsAction.class);

    public static final Action<LootTable, ReplaceEntryItemAction> LOOT_REPLACE_ENTRY_ITEM = EditorActionRegistry.register(
        Workspaces.LOOT_TABLE,
        "loot_table/replace_entry_item",
        ReplaceEntryItemAction.CODEC,
        ReplaceEntryItemAction.class);

    public static final Action<LootTable, SetEntryCountMinAction> LOOT_SET_ENTRY_COUNT_MIN = EditorActionRegistry.register(
        Workspaces.LOOT_TABLE,
        "loot_table/set_entry_count_min",
        SetEntryCountMinAction.CODEC,
        SetEntryCountMinAction.class);

    public static final Action<LootTable, SetEntryCountMaxAction> LOOT_SET_ENTRY_COUNT_MAX = EditorActionRegistry.register(
        Workspaces.LOOT_TABLE,
        "loot_table/set_entry_count_max",
        SetEntryCountMaxAction.CODEC,
        SetEntryCountMaxAction.class);

    // Recipe
    public static final Action<Recipe<?>, SetCraftingPatternAction> SET_CRAFTING_PATTERN = EditorActionRegistry.register(
        Workspaces.RECIPE,
        "recipe/set_crafting_pattern",
        SetCraftingPatternAction.CODEC,
        SetCraftingPatternAction.class);

    public static final Action<Recipe<?>, RemoveItemEverywhereAction> REMOVE_ITEM_EVERYWHERE = EditorActionRegistry.register(
        Workspaces.RECIPE,
        "recipe/remove_item_everywhere",
        RemoveItemEverywhereAction.CODEC,
        RemoveItemEverywhereAction.class);

    public static final Action<Recipe<?>, ReplaceItemEverywhereAction> REPLACE_ITEM_EVERYWHERE = EditorActionRegistry.register(
        Workspaces.RECIPE,
        "recipe/replace_item_everywhere",
        ReplaceItemEverywhereAction.CODEC,
        ReplaceItemEverywhereAction.class);

    public static final Action<Recipe<?>, ConvertRecipeTypeAction> CONVERT_RECIPE_TYPE = EditorActionRegistry.register(
        Workspaces.RECIPE,
        "recipe/convert_recipe_type",
        ConvertRecipeTypeAction.CODEC,
        ConvertRecipeTypeAction.class);

    public static final Action<Recipe<?>, SetResultCountAction> SET_RESULT_COUNT = EditorActionRegistry.register(
        Workspaces.RECIPE,
        "recipe/set_result_count",
        SetResultCountAction.CODEC,
        SetResultCountAction.class);

    public static final Action<Recipe<?>, SetResultItemAction> SET_RESULT_ITEM = EditorActionRegistry.register(
        Workspaces.RECIPE,
        "recipe/set_result_item",
        SetResultItemAction.CODEC,
        SetResultItemAction.class);

    public static final Action<Recipe<?>, SetGroupAction> SET_GROUP = EditorActionRegistry.register(
        Workspaces.RECIPE,
        "recipe/set_group",
        SetGroupAction.CODEC,
        SetGroupAction.class);

    public static final Action<Recipe<?>, SetCategoryAction> SET_CATEGORY = EditorActionRegistry.register(
        Workspaces.RECIPE,
        "recipe/set_category",
        SetCategoryAction.CODEC,
        SetCategoryAction.class);

    public static final Action<Recipe<?>, SetCookingExperienceAction> SET_COOKING_EXPERIENCE = EditorActionRegistry.register(
        Workspaces.RECIPE,
        "recipe/set_cooking_experience",
        SetCookingExperienceAction.CODEC,
        SetCookingExperienceAction.class);

    public static final Action<Recipe<?>, SetCookingTimeAction> SET_COOKING_TIME = EditorActionRegistry.register(
        Workspaces.RECIPE,
        "recipe/set_cooking_time",
        SetCookingTimeAction.CODEC,
        SetCookingTimeAction.class);

    public static final Action<Recipe<?>, SetShowNotificationAction> SET_SHOW_NOTIFICATION = EditorActionRegistry.register(
        Workspaces.RECIPE,
        "recipe/set_show_notification",
        SetShowNotificationAction.CODEC,
        SetShowNotificationAction.class);

    public static final Action<Recipe<?>, SetResultComponentAction> SET_RESULT_COMPONENT = EditorActionRegistry.register(
        Workspaces.RECIPE,
        "recipe/set_result_component",
        SetResultComponentAction.CODEC,
        SetResultComponentAction.class);

    public static final Action<Recipe<?>, RemoveResultComponentAction> REMOVE_RESULT_COMPONENT = EditorActionRegistry.register(
        Workspaces.RECIPE,
        "recipe/remove_result_component",
        RemoveResultComponentAction.CODEC,
        RemoveResultComponentAction.class);

    public static final Action<Object, SetEntryDataAction> SET_ENTRY_DATA = EditorActionRegistry.registerGlobal(
        "set_entry_data",
        SetEntryDataAction.CODEC,
        SetEntryDataAction.class);

    public static void register() {
        // Static fields are initialized on class load — this method triggers that.
    }

    private Actions() {}
}
