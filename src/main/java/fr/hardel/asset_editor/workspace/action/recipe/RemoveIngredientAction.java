package fr.hardel.asset_editor.workspace.action.recipe;

import fr.hardel.asset_editor.workspace.flush.ElementEntry;
import fr.hardel.asset_editor.workspace.io.RegistryMutationContext;
import fr.hardel.asset_editor.workspace.action.EditorAction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record RemoveIngredientAction(int slot, List<Identifier> items) implements EditorAction<Recipe<?>> {

    private static final StreamCodec<io.netty.buffer.ByteBuf, List<Identifier>> IDENTIFIER_LIST_CODEC = ByteBufCodecs.collection(ArrayList::new, Identifier.STREAM_CODEC);

    public static final StreamCodec<io.netty.buffer.ByteBuf, RemoveIngredientAction> CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, RemoveIngredientAction::slot,
        IDENTIFIER_LIST_CODEC, RemoveIngredientAction::items,
        RemoveIngredientAction::new);

    @Override
    public ElementEntry<Recipe<?>> apply(ElementEntry<Recipe<?>> entry, RegistryMutationContext ctx) {
        RecipeIngredientHelper helper = new RecipeIngredientHelper(ctx.registries());
        Recipe<?> recipe = entry.data();
        List<Optional<Ingredient>> ingredients = helper.extractIngredients(recipe);
        if (slot < 0 || slot >= ingredients.size())
            return entry;

        Optional<Ingredient> updated = items.isEmpty()
            ? Optional.empty()
            : ingredients.get(slot).map(existing -> helper.remove(existing, items));

        if (!items.isEmpty() && ingredients.get(slot).isEmpty())
            return entry;

        ingredients.set(slot, updated);
        return entry.withData(helper.rebuild(recipe, ingredients));
    }
}
