package fr.hardel.asset_editor.workspace.action.recipe;

import fr.hardel.asset_editor.AssetEditor;
import fr.hardel.asset_editor.workspace.action.EditorAction;
import fr.hardel.asset_editor.workspace.action.EditorActionType;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;

import java.util.List;
import java.util.Optional;

public record ReplaceItemEverywhereAction(Identifier from, Identifier to) implements EditorAction {

    public static final EditorActionType<Recipe<?>, ReplaceItemEverywhereAction> TYPE = new EditorActionType<>(
        Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "recipe/replace_item_everywhere"),
        ReplaceItemEverywhereAction.class,
        StreamCodec.composite(
            Identifier.STREAM_CODEC, ReplaceItemEverywhereAction::from,
            Identifier.STREAM_CODEC, ReplaceItemEverywhereAction::to,
            ReplaceItemEverywhereAction::new),
        (entry, action, ctx) -> {
            RecipeIngredientHelper helper = new RecipeIngredientHelper(ctx.registries());
            Recipe<?> recipe = entry.data();
            List<Optional<Ingredient>> ingredients = helper.extractIngredients(recipe);

            List<Optional<Ingredient>> replaced = ingredients.stream()
                .map(slot -> slot.flatMap(ingredient -> Optional.ofNullable(helper.replace(ingredient, action.from(), action.to()))))
                .toList();

            return entry.withData(helper.rebuild(recipe, replaced));
        });

    @Override
    public EditorActionType<Recipe<?>, ReplaceItemEverywhereAction> type() {
        return TYPE;
    }
}
