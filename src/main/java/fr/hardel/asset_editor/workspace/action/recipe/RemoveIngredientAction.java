package fr.hardel.asset_editor.workspace.action.recipe;

import fr.hardel.asset_editor.AssetEditor;
import fr.hardel.asset_editor.workspace.action.EditorAction;
import fr.hardel.asset_editor.workspace.action.EditorActionType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record RemoveIngredientAction(int slot, List<Identifier> items) implements EditorAction {

    private static final StreamCodec<io.netty.buffer.ByteBuf, List<Identifier>> IDENTIFIER_LIST_CODEC = ByteBufCodecs.collection(ArrayList::new, Identifier.STREAM_CODEC);

    public static final EditorActionType<Recipe<?>, RemoveIngredientAction> TYPE = new EditorActionType<>(
        Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "recipe/remove_ingredient"),
        RemoveIngredientAction.class,
        StreamCodec.composite(
            ByteBufCodecs.VAR_INT, RemoveIngredientAction::slot,
            IDENTIFIER_LIST_CODEC, RemoveIngredientAction::items,
            RemoveIngredientAction::new),
        (entry, action, ctx) -> {
            RecipeIngredientHelper helper = new RecipeIngredientHelper(ctx.registries());
            Recipe<?> recipe = entry.data();
            List<Optional<Ingredient>> ingredients = helper.extractIngredients(recipe);
            if (action.slot() < 0 || action.slot() >= ingredients.size())
                return entry;

            Optional<Ingredient> updated = action.items().isEmpty()
                ? Optional.empty()
                : ingredients.get(action.slot()).map(existing -> helper.remove(existing, action.items()));

            if (!action.items().isEmpty() && ingredients.get(action.slot()).isEmpty())
                return entry;

            ingredients.set(action.slot(), updated);
            return entry.withData(helper.rebuild(recipe, ingredients));
        });

    @Override
    public EditorActionType<Recipe<?>, RemoveIngredientAction> type() {
        return TYPE;
    }
}
