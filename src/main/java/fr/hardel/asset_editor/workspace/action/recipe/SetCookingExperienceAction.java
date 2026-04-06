package fr.hardel.asset_editor.workspace.action.recipe;

import fr.hardel.asset_editor.AssetEditor;
import fr.hardel.asset_editor.workspace.action.EditorAction;
import fr.hardel.asset_editor.workspace.action.EditorActionType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.Recipe;

public record SetCookingExperienceAction(float experience) implements EditorAction {

    public static final EditorActionType<Recipe<?>, SetCookingExperienceAction> TYPE = new EditorActionType<>(
        Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "recipe/set_cooking_experience"),
        SetCookingExperienceAction.class,
        StreamCodec.composite(ByteBufCodecs.FLOAT, SetCookingExperienceAction::experience, SetCookingExperienceAction::new),
        (entry, action, ctx) -> {
            if (!Float.isFinite(action.experience()))
                return entry;

            RecipeIngredientHelper helper = new RecipeIngredientHelper(ctx.registries());
            Recipe<?> updated = helper.setCookingExperience(entry.data(), Math.max(0.0F, action.experience()));
            return updated == null ? entry : entry.withData(updated);
        });

    @Override
    public EditorActionType<Recipe<?>, SetCookingExperienceAction> type() {
        return TYPE;
    }
}
