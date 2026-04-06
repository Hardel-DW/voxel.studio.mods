package fr.hardel.asset_editor.workspace.action.recipe;

import fr.hardel.asset_editor.AssetEditor;
import fr.hardel.asset_editor.workspace.action.EditorAction;
import fr.hardel.asset_editor.workspace.action.EditorActionType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.Recipe;

public record SetResultCountAction(int count) implements EditorAction {

    public static final EditorActionType<Recipe<?>, SetResultCountAction> TYPE = new EditorActionType<>(
        Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "recipe/set_result_count"),
        SetResultCountAction.class,
        StreamCodec.composite(ByteBufCodecs.VAR_INT, SetResultCountAction::count, SetResultCountAction::new),
        (entry, action, ctx) -> {
            RecipeIngredientHelper helper = new RecipeIngredientHelper(ctx.registries());
            int maxCount = Math.max(1, helper.extractResult(entry.data()).getMaxStackSize());
            int count = Math.max(1, Math.min(maxCount, action.count()));

            Recipe<?> updated = helper.setResultCount(entry.data(), count);
            return updated == null ? entry : entry.withData(updated);
        });

    @Override
    public EditorActionType<Recipe<?>, SetResultCountAction> type() {
        return TYPE;
    }
}
