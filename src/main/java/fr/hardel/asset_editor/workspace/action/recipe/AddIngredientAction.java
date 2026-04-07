package fr.hardel.asset_editor.workspace.action.recipe;

import fr.hardel.asset_editor.workspace.ElementEntry;
import fr.hardel.asset_editor.workspace.RegistryMutationContext;
import fr.hardel.asset_editor.workspace.action.EditorAction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record AddIngredientAction(int slot, List<Identifier> items, boolean replace) implements EditorAction<Recipe<?>> {

    private static final StreamCodec<io.netty.buffer.ByteBuf, List<Identifier>> IDENTIFIER_LIST_CODEC = ByteBufCodecs.collection(ArrayList::new, Identifier.STREAM_CODEC);

    public static final StreamCodec<io.netty.buffer.ByteBuf, AddIngredientAction> CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, AddIngredientAction::slot,
        IDENTIFIER_LIST_CODEC, AddIngredientAction::items,
        ByteBufCodecs.BOOL, AddIngredientAction::replace,
        AddIngredientAction::new);

    @Override
    public ElementEntry<Recipe<?>> apply(ElementEntry<Recipe<?>> entry, RegistryMutationContext ctx) {
        RecipeIngredientHelper helper = new RecipeIngredientHelper(ctx.registries());
        Recipe<?> recipe = entry.data();
        if (recipe instanceof ShapelessRecipe)
            return entry;

        List<Optional<Ingredient>> ingredients = helper.extractIngredients(recipe);
        if (slot < 0 || slot >= ingredients.size())
            return entry;

        boolean shouldReplace = replace || ingredients.get(slot).isEmpty();
        Ingredient ingredient = shouldReplace ? helper.toIngredient(items) : helper.merge(ingredients.get(slot).get(), items);
        ingredients.set(slot, Optional.of(ingredient));

        return entry.withData(helper.rebuild(recipe, ingredients));
    }
}
