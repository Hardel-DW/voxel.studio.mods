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

public record RemoveItemEverywhereAction(List<Identifier> items) implements EditorAction {

    private static final StreamCodec<io.netty.buffer.ByteBuf, List<Identifier>> IDENTIFIER_LIST_CODEC = ByteBufCodecs.collection(ArrayList::new, Identifier.STREAM_CODEC);

    public static final EditorActionType<Recipe<?>, RemoveItemEverywhereAction> TYPE = new EditorActionType<>(
        Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "recipe/remove_item_everywhere"),
        RemoveItemEverywhereAction.class,
        StreamCodec.composite(IDENTIFIER_LIST_CODEC, RemoveItemEverywhereAction::items, RemoveItemEverywhereAction::new),
        (entry, action, ctx) -> {
            RecipeIngredientHelper helper = new RecipeIngredientHelper(ctx.registries());
            Recipe<?> recipe = entry.data();
            List<Optional<Ingredient>> ingredients = helper.extractIngredients(recipe);

            List<Optional<Ingredient>> filtered = ingredients.stream()
                .map(slot -> slot.flatMap(ingredient -> Optional.ofNullable(helper.remove(ingredient, action.items()))))
                .toList();

            return entry.withData(helper.rebuild(recipe, filtered));
        });

    @Override
    public EditorActionType<Recipe<?>, RemoveItemEverywhereAction> type() {
        return TYPE;
    }
}
