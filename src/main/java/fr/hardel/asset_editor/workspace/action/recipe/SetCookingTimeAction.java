package fr.hardel.asset_editor.workspace.action.recipe;

import fr.hardel.asset_editor.workspace.flush.ElementEntry;
import fr.hardel.asset_editor.workspace.io.RegistryMutationContext;
import fr.hardel.asset_editor.workspace.action.EditorAction;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.CampfireCookingRecipe;
import net.minecraft.world.item.crafting.Recipe;

public record SetCookingTimeAction(int time) implements EditorAction<Recipe<?>> {

    public static final StreamCodec<ByteBuf, SetCookingTimeAction> CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, SetCookingTimeAction::time, SetCookingTimeAction::new);

    @Override
    public ElementEntry<Recipe<?>> apply(ElementEntry<Recipe<?>> entry, RegistryMutationContext ctx) {
        RecipeIngredientHelper helper = new RecipeIngredientHelper(ctx.registries());
        Recipe<?> updated = helper.setCookingTime(entry.data(), sanitizeCookingTime(entry.data(), time));
        return updated == null ? entry : entry.withData(updated);
    }

    private static int sanitizeCookingTime(Recipe<?> recipe, int time) {
        int max = recipe instanceof CampfireCookingRecipe ? Integer.MAX_VALUE : Short.MAX_VALUE;
        return Math.max(1, Math.min(time, max));
    }
}
