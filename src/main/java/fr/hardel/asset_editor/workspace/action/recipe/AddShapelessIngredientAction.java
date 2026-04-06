package fr.hardel.asset_editor.workspace.action.recipe;

import fr.hardel.asset_editor.AssetEditor;
import fr.hardel.asset_editor.workspace.action.EditorAction;
import fr.hardel.asset_editor.workspace.action.EditorActionType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;

import java.util.ArrayList;
import java.util.List;

public record AddShapelessIngredientAction(List<Identifier> items) implements EditorAction {

    private static final StreamCodec<io.netty.buffer.ByteBuf, List<Identifier>> IDENTIFIER_LIST_CODEC = ByteBufCodecs.collection(ArrayList::new, Identifier.STREAM_CODEC);

    public static final EditorActionType<Recipe<?>, AddShapelessIngredientAction> TYPE = new EditorActionType<>(
        Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "recipe/add_shapeless_ingredient"),
        AddShapelessIngredientAction.class,
        StreamCodec.composite(IDENTIFIER_LIST_CODEC, AddShapelessIngredientAction::items, AddShapelessIngredientAction::new),
        (entry, action, ctx) -> {
            if (!(entry.data() instanceof ShapelessRecipe shapeless))
                return entry;

            RecipeIngredientHelper helper = new RecipeIngredientHelper(ctx.registries());
            List<Ingredient> newIngredients = new ArrayList<>(shapeless.ingredients);
            newIngredients.add(helper.toIngredient(action.items()));

            return entry.withData(new ShapelessRecipe(shapeless.group(), shapeless.category(), shapeless.result.copy(), newIngredients));
        });

    @Override
    public EditorActionType<Recipe<?>, AddShapelessIngredientAction> type() {
        return TYPE;
    }
}
