package fr.hardel.asset_editor.workspace.action.recipe;

import fr.hardel.asset_editor.workspace.ElementEntry;
import fr.hardel.asset_editor.workspace.RegistryMutationContext;
import fr.hardel.asset_editor.workspace.action.EditorAction;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Recipe;

public record SetGroupAction(String group) implements EditorAction<Recipe<?>> {

    public static final StreamCodec<ByteBuf, SetGroupAction> CODEC = StreamCodec.composite(ByteBufCodecs.STRING_UTF8, SetGroupAction::group, SetGroupAction::new);

    @Override
    public ElementEntry<Recipe<?>> apply(ElementEntry<Recipe<?>> entry, RegistryMutationContext ctx) {
        RecipeIngredientHelper helper = new RecipeIngredientHelper(ctx.registries());
        Recipe<?> updated = helper.setGroup(entry.data(), group.trim());
        return updated == null ? entry : entry.withData(updated);
    }
}
