package fr.hardel.asset_editor.workspace.action.recipe;

import fr.hardel.asset_editor.workspace.flush.ElementEntry;
import fr.hardel.asset_editor.workspace.io.RegistryMutationContext;
import fr.hardel.asset_editor.workspace.action.EditorAction;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;

import java.util.List;
import java.util.Optional;

public record ReplaceItemEverywhereAction(Identifier from, Identifier to) implements EditorAction<Recipe<?>> {

    public static final StreamCodec<ByteBuf, ReplaceItemEverywhereAction> CODEC = StreamCodec.composite(
        Identifier.STREAM_CODEC, ReplaceItemEverywhereAction::from,
        Identifier.STREAM_CODEC, ReplaceItemEverywhereAction::to,
        ReplaceItemEverywhereAction::new);

    @Override
    public ElementEntry<Recipe<?>> apply(ElementEntry<Recipe<?>> entry, RegistryMutationContext ctx) {
        RecipeIngredientHelper helper = new RecipeIngredientHelper(ctx.registries());
        Recipe<?> recipe = entry.data();
        List<Optional<Ingredient>> ingredients = helper.extractIngredients(recipe);

        List<Optional<Ingredient>> replaced = ingredients.stream()
            .map(slot -> slot.flatMap(ingredient -> Optional.ofNullable(helper.replace(ingredient, from, to))))
            .toList();

        return entry.withData(helper.rebuild(recipe, replaced));
    }
}
