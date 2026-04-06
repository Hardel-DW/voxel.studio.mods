package fr.hardel.asset_editor.workspace.action.recipe;

import fr.hardel.asset_editor.AssetEditor;
import fr.hardel.asset_editor.workspace.action.EditorAction;
import fr.hardel.asset_editor.workspace.action.EditorActionType;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.Recipe;

public record SetResultItemAction(Identifier itemId) implements EditorAction {

    public static final EditorActionType<Recipe<?>, SetResultItemAction> TYPE = new EditorActionType<>(
        Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "recipe/set_result_item"),
        SetResultItemAction.class,
        StreamCodec.composite(Identifier.STREAM_CODEC, SetResultItemAction::itemId, SetResultItemAction::new),
        (entry, action, ctx) -> {
            RecipeIngredientHelper helper = new RecipeIngredientHelper(ctx.registries());
            Recipe<?> updated = helper.setResultItem(entry.data(), action.itemId());
            return updated == null ? entry : entry.withData(updated);
        });

    @Override
    public EditorActionType<Recipe<?>, SetResultItemAction> type() {
        return TYPE;
    }
}
