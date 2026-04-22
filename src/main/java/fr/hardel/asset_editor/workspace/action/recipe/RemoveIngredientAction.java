package fr.hardel.asset_editor.workspace.action.recipe;

import fr.hardel.asset_editor.workspace.flush.ElementEntry;
import fr.hardel.asset_editor.workspace.io.RegistryMutationContext;
import fr.hardel.asset_editor.workspace.action.EditorAction;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;

import java.util.*;
import java.util.stream.Collectors;

public record RemoveIngredientAction(int slot, List<Identifier> items) implements EditorAction<Recipe<?>> {

    private static final StreamCodec<ByteBuf, List<Identifier>> IDENTIFIER_LIST_CODEC = ByteBufCodecs.collection(ArrayList::new, Identifier.STREAM_CODEC);

    public static final StreamCodec<ByteBuf, RemoveIngredientAction> CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, RemoveIngredientAction::slot,
        IDENTIFIER_LIST_CODEC, RemoveIngredientAction::items,
        RemoveIngredientAction::new);

    @Override
    public ElementEntry<Recipe<?>> apply(ElementEntry<Recipe<?>> entry, RegistryMutationContext ctx) {
        RecipeIngredientHelper helper = new RecipeIngredientHelper(ctx.registries());
        Recipe<?> recipe = entry.data();

        if (recipe instanceof ShapelessRecipe shapeless) {
            return applyShapeless(entry, shapeless);
        }

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

    private ElementEntry<Recipe<?>> applyShapeless(ElementEntry<Recipe<?>> entry, ShapelessRecipe shapeless) {
        if (items.isEmpty())
            return entry;

        Set<Identifier> toRemove = new HashSet<>(items);
        List<Ingredient> newIngredients = new ArrayList<>(shapeless.ingredients);
        Iterator<Ingredient> it = newIngredients.iterator();
        while (it.hasNext()) {
            Set<Identifier> ingredientItems = it.next().values.stream()
                .flatMap(h -> h.unwrapKey().stream())
                .map(ResourceKey::identifier)
                .collect(Collectors.toSet());
            if (ingredientItems.equals(toRemove)) {
                it.remove();
                break;
            }
        }

        if (newIngredients.size() == shapeless.ingredients.size())
            return entry;

        if (newIngredients.isEmpty())
            return entry;

        return entry.withData(new ShapelessRecipe(shapeless.group(), shapeless.category(), shapeless.result.copy(), newIngredients));
    }
}
