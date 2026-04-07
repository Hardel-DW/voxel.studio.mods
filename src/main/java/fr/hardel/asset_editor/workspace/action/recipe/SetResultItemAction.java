package fr.hardel.asset_editor.workspace.action.recipe;

import fr.hardel.asset_editor.workspace.flush.ElementEntry;
import fr.hardel.asset_editor.workspace.io.RegistryMutationContext;
import fr.hardel.asset_editor.workspace.action.EditorAction;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.Recipe;

public record SetResultItemAction(Identifier itemId) implements EditorAction<Recipe<?>> {

    public static final StreamCodec<ByteBuf, SetResultItemAction> CODEC = StreamCodec.composite(Identifier.STREAM_CODEC, SetResultItemAction::itemId, SetResultItemAction::new);

    @Override
    public ElementEntry<Recipe<?>> apply(ElementEntry<Recipe<?>> entry, RegistryMutationContext ctx) {
        RecipeIngredientHelper helper = new RecipeIngredientHelper(ctx.registries());
        Recipe<?> updated = helper.setResultItem(entry.data(), itemId);
        return updated == null ? entry : entry.withData(updated);
    }
}
