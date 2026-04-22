package fr.hardel.asset_editor.workspace.action.recipe;

import fr.hardel.asset_editor.workspace.action.EditorAction;
import fr.hardel.asset_editor.workspace.action.recipe.adapter.RecipeAdapterRegistry;
import fr.hardel.asset_editor.workspace.flush.ElementEntry;
import fr.hardel.asset_editor.workspace.io.RegistryMutationContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;

public record RemoveResultComponentAction(Identifier componentId) implements EditorAction<Recipe<?>> {

    public static final StreamCodec<ByteBuf, RemoveResultComponentAction> CODEC = StreamCodec.composite(
        Identifier.STREAM_CODEC, RemoveResultComponentAction::componentId,
        RemoveResultComponentAction::new);

    @Override
    public ElementEntry<Recipe<?>> apply(ElementEntry<Recipe<?>> entry, RegistryMutationContext ctx) {
        DataComponentType<?> type = BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(componentId);
        if (type == null) return entry;

        if (!RecipeAdapterRegistry.supportsResultComponents(entry.data())) return entry;

        ItemStack current = RecipeAdapterRegistry.extractResult(entry.data());
        if (current.isEmpty()) return entry;

        DataComponentPatch newPatch = DataComponentPatches.forget(current.getComponentsPatch(), type);
        if (newPatch.equals(current.getComponentsPatch())) return entry;

        Recipe<?> updated = RecipeAdapterRegistry.setResultComponents(entry.data(), newPatch);
        return updated == null ? entry : entry.withData(updated);
    }
}
