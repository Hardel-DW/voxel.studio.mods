package fr.hardel.asset_editor.workspace.action.recipe;

import fr.hardel.asset_editor.workspace.flush.ElementEntry;
import fr.hardel.asset_editor.workspace.io.RegistryMutationContext;
import fr.hardel.asset_editor.workspace.action.EditorAction;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.Recipe;

public record ConvertRecipeTypeAction(Identifier newSerializer, boolean preserveIngredients) implements EditorAction<Recipe<?>> {

    public static final StreamCodec<ByteBuf, ConvertRecipeTypeAction> CODEC = StreamCodec.composite(
        Identifier.STREAM_CODEC, ConvertRecipeTypeAction::newSerializer,
        ByteBufCodecs.BOOL, ConvertRecipeTypeAction::preserveIngredients,
        ConvertRecipeTypeAction::new);

    @Override
    public ElementEntry<Recipe<?>> apply(ElementEntry<Recipe<?>> entry, RegistryMutationContext ctx) {
        RecipeIngredientHelper helper = new RecipeIngredientHelper(ctx.registries());
        Recipe<?> converted = helper.convertRecipeType(entry.data(), newSerializer, preserveIngredients);
        return converted == null ? entry : entry.withData(converted);
    }
}
