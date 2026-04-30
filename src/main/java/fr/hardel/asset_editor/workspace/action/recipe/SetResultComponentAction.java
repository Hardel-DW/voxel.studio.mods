package fr.hardel.asset_editor.workspace.action.recipe;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import fr.hardel.asset_editor.workspace.action.EditorAction;
import fr.hardel.asset_editor.workspace.action.recipe.adapter.RecipeAdapterRegistry;
import fr.hardel.asset_editor.workspace.flush.ElementEntry;
import fr.hardel.asset_editor.workspace.io.RegistryMutationContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StrictJsonParser;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import org.jspecify.annotations.Nullable;

public record SetResultComponentAction(Identifier componentId, String valueJson) implements EditorAction<Recipe<?>> {

    private static final int MAX_JSON_BYTES = 1 << 20;

    public static final StreamCodec<ByteBuf, SetResultComponentAction> CODEC = StreamCodec.composite(
        Identifier.STREAM_CODEC, SetResultComponentAction::componentId,
        ByteBufCodecs.stringUtf8(MAX_JSON_BYTES), SetResultComponentAction::valueJson,
        SetResultComponentAction::new);

    @Override
    public ElementEntry<Recipe<?>> apply(ElementEntry<Recipe<?>> entry, RegistryMutationContext ctx) {
        DataComponentType<?> type = BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(componentId);
        if (type == null || type.codec() == null) return entry;
        if (!RecipeAdapterRegistry.supportsResultComponents(entry.data())) return entry;

        ItemStack current = RecipeAdapterRegistry.extractResult(entry.data());
        if (current.isEmpty()) return entry;

        JsonElement json;
        try {
            json = StrictJsonParser.parse(valueJson);
        } catch (Exception e) {
            return entry;
        }

        DataComponentPatch newPatch = parseAndSet(current.getComponentsPatch(), type, json, ctx.registries());
        if (newPatch == null) return entry;

        Recipe<?> updated = RecipeAdapterRegistry.setResultComponents(entry.data(), newPatch);
        return updated == null ? entry : entry.withData(updated);
    }

    private static <T> @Nullable DataComponentPatch parseAndSet(DataComponentPatch old, DataComponentType<T> type, JsonElement json, HolderLookup.Provider registries) {
        var ops = registries.createSerializationContext(JsonOps.INSTANCE);
        return type.codecOrThrow().parse(ops, json).result()
            .map(value -> DataComponentPatches.set(old, type, value))
            .orElse(null);
    }
}
