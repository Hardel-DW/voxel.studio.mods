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
import java.util.Optional;

public record AddIngredientAction(int slot, List<Identifier> items, boolean replace) implements EditorAction {

    private static final StreamCodec<io.netty.buffer.ByteBuf, List<Identifier>> IDENTIFIER_LIST_CODEC = ByteBufCodecs.collection(ArrayList::new, Identifier.STREAM_CODEC);

    public static final EditorActionType<Recipe<?>, AddIngredientAction> TYPE = new EditorActionType<>(
        Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "recipe/add_ingredient"),
        AddIngredientAction.class,
        StreamCodec.composite(
            ByteBufCodecs.VAR_INT, AddIngredientAction::slot,
            IDENTIFIER_LIST_CODEC, AddIngredientAction::items,
            ByteBufCodecs.BOOL, AddIngredientAction::replace,
            AddIngredientAction::new),
        (entry, action, ctx) -> {
            RecipeIngredientHelper helper = new RecipeIngredientHelper(ctx.registries());
            Recipe<?> recipe = entry.data();
            if (recipe instanceof ShapelessRecipe)
                return entry;

            List<Optional<Ingredient>> ingredients = helper.extractIngredients(recipe);
            if (action.slot() < 0 || action.slot() >= ingredients.size())
                return entry;

            boolean shouldReplace = action.replace() || ingredients.get(action.slot()).isEmpty();
            Ingredient ingredient = shouldReplace ? helper.toIngredient(action.items()) : helper.merge(ingredients.get(action.slot()).get(), action.items());
            ingredients.set(action.slot(), Optional.of(ingredient));

            return entry.withData(helper.rebuild(recipe, ingredients));
        });

    @Override
    public EditorActionType<Recipe<?>, AddIngredientAction> type() {
        return TYPE;
    }
}
