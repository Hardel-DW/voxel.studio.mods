package fr.hardel.asset_editor.workspace.action.recipe;

import fr.hardel.asset_editor.AssetEditor;
import fr.hardel.asset_editor.workspace.action.EditorAction;
import fr.hardel.asset_editor.workspace.action.EditorActionType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.Recipe;

public record ConvertRecipeTypeAction(Identifier newSerializer, boolean preserveIngredients) implements EditorAction {

    public static final EditorActionType<Recipe<?>, ConvertRecipeTypeAction> TYPE = new EditorActionType<>(
        Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "recipe/convert_recipe_type"),
        ConvertRecipeTypeAction.class,
        StreamCodec.composite(
            Identifier.STREAM_CODEC, ConvertRecipeTypeAction::newSerializer,
            ByteBufCodecs.BOOL, ConvertRecipeTypeAction::preserveIngredients,
            ConvertRecipeTypeAction::new),
        (entry, action, ctx) -> {
            RecipeIngredientHelper helper = new RecipeIngredientHelper(ctx.registries());
            Recipe<?> converted = helper.convertRecipeType(entry.data(), action.newSerializer(), action.preserveIngredients());
            return converted == null ? entry : entry.withData(converted);
        });

    @Override
    public EditorActionType<Recipe<?>, ConvertRecipeTypeAction> type() {
        return TYPE;
    }
}
