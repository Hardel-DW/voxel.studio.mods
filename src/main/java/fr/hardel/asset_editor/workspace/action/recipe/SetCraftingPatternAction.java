package fr.hardel.asset_editor.workspace.action.recipe;

import fr.hardel.asset_editor.workspace.action.EditorAction;
import fr.hardel.asset_editor.workspace.action.recipe.adapter.RecipeAdapterRegistry;
import fr.hardel.asset_editor.workspace.flush.ElementEntry;
import fr.hardel.asset_editor.workspace.io.RegistryMutationContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.Recipe;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record SetCraftingPatternAction(Map<Integer, List<Identifier>> slots) implements EditorAction<Recipe<?>> {

    private static final StreamCodec<ByteBuf, Map<Integer, List<Identifier>>> SLOTS_CODEC =
        ByteBufCodecs.map(HashMap::new, ByteBufCodecs.VAR_INT, Identifier.STREAM_CODEC.apply(ByteBufCodecs.list()));

    public static final StreamCodec<ByteBuf, SetCraftingPatternAction> CODEC = StreamCodec.composite(
        SLOTS_CODEC, SetCraftingPatternAction::slots,
        SetCraftingPatternAction::new);

    @Override
    public ElementEntry<Recipe<?>> apply(ElementEntry<Recipe<?>> entry, RegistryMutationContext ctx) {
        RecipeIngredientHelper helper = new RecipeIngredientHelper(ctx.registries());
        Recipe<?> updated = RecipeAdapterRegistry.applyPattern(entry.data(), slots, helper);
        return updated == null ? entry : entry.withData(updated);
    }
}
