package fr.hardel.asset_editor.workspace.action.recipe;

import fr.hardel.asset_editor.workspace.flush.ElementEntry;
import fr.hardel.asset_editor.workspace.io.RegistryMutationContext;
import fr.hardel.asset_editor.workspace.action.EditorAction;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Recipe;

public record SetCookingExperienceAction(float experience) implements EditorAction<Recipe<?>> {

    public static final StreamCodec<ByteBuf, SetCookingExperienceAction> CODEC = StreamCodec.composite(ByteBufCodecs.FLOAT, SetCookingExperienceAction::experience, SetCookingExperienceAction::new);

    @Override
    public ElementEntry<Recipe<?>> apply(ElementEntry<Recipe<?>> entry, RegistryMutationContext ctx) {
        if (!Float.isFinite(experience))
            return entry;

        RecipeIngredientHelper helper = new RecipeIngredientHelper(ctx.registries());
        Recipe<?> updated = helper.setCookingExperience(entry.data(), Math.max(0.0F, experience));
        return updated == null ? entry : entry.withData(updated);
    }
}
