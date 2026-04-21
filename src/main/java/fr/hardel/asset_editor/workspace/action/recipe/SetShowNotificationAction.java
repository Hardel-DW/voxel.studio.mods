package fr.hardel.asset_editor.workspace.action.recipe;

import fr.hardel.asset_editor.workspace.action.recipe.adapter.RecipeAdapter;
import fr.hardel.asset_editor.workspace.flush.ElementEntry;
import fr.hardel.asset_editor.workspace.io.RegistryMutationContext;
import fr.hardel.asset_editor.workspace.action.EditorAction;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Recipe;

public record SetShowNotificationAction(boolean value) implements EditorAction<Recipe<?>> {

    public static final StreamCodec<ByteBuf, SetShowNotificationAction> CODEC = StreamCodec.composite(ByteBufCodecs.BOOL, SetShowNotificationAction::value, SetShowNotificationAction::new);

    @Override
    public ElementEntry<Recipe<?>> apply(ElementEntry<Recipe<?>> entry, RegistryMutationContext ctx) {
        RecipeIngredientHelper helper = new RecipeIngredientHelper(ctx.registries());
        Recipe<?> updated = helper.setProperty(entry.data(), RecipeAdapter.SHOW_NOTIFICATION, value);
        return updated == null ? entry : entry.withData(updated);
    }
}
