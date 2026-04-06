package fr.hardel.asset_editor.workspace.action.recipe;

import fr.hardel.asset_editor.AssetEditor;
import fr.hardel.asset_editor.workspace.action.EditorAction;
import fr.hardel.asset_editor.workspace.action.EditorActionType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.CampfireCookingRecipe;
import net.minecraft.world.item.crafting.Recipe;

public record SetCookingTimeAction(int time) implements EditorAction {

    public static final EditorActionType<Recipe<?>, SetCookingTimeAction> TYPE = new EditorActionType<>(
        Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "recipe/set_cooking_time"),
        SetCookingTimeAction.class,
        StreamCodec.composite(ByteBufCodecs.VAR_INT, SetCookingTimeAction::time, SetCookingTimeAction::new),
        (entry, action, ctx) -> {
            RecipeIngredientHelper helper = new RecipeIngredientHelper(ctx.registries());
            Recipe<?> updated = helper.setCookingTime(entry.data(), sanitizeCookingTime(entry.data(), action.time()));
            return updated == null ? entry : entry.withData(updated);
        });

    @Override
    public EditorActionType<Recipe<?>, SetCookingTimeAction> type() {
        return TYPE;
    }

    private static int sanitizeCookingTime(Recipe<?> recipe, int time) {
        int max = recipe instanceof CampfireCookingRecipe ? Integer.MAX_VALUE : Short.MAX_VALUE;
        return Math.max(1, Math.min(time, max));
    }
}
