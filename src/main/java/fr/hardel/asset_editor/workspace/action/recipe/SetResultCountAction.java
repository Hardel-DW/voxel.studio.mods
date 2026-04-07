package fr.hardel.asset_editor.workspace.action.recipe;

import fr.hardel.asset_editor.workspace.flush.ElementEntry;
import fr.hardel.asset_editor.workspace.io.RegistryMutationContext;
import fr.hardel.asset_editor.workspace.action.EditorAction;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Recipe;

public record SetResultCountAction(int count) implements EditorAction<Recipe<?>> {

    public static final StreamCodec<ByteBuf, SetResultCountAction> CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, SetResultCountAction::count, SetResultCountAction::new);

    @Override
    public ElementEntry<Recipe<?>> apply(ElementEntry<Recipe<?>> entry, RegistryMutationContext ctx) {
        RecipeIngredientHelper helper = new RecipeIngredientHelper(ctx.registries());
        int maxCount = Math.max(1, helper.extractResult(entry.data()).getMaxStackSize());
        int sanitized = Math.max(1, Math.min(maxCount, count));

        Recipe<?> updated = helper.setResultCount(entry.data(), sanitized);
        return updated == null ? entry : entry.withData(updated);
    }
}
