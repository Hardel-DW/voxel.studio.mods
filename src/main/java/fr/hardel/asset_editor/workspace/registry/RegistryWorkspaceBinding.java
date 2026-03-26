package fr.hardel.asset_editor.workspace.registry;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import fr.hardel.asset_editor.network.workspace.WorkspaceElementSnapshot;
import fr.hardel.asset_editor.store.CustomFields;
import fr.hardel.asset_editor.store.ElementEntry;
import fr.hardel.asset_editor.store.FlushAdapter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

import java.util.function.Function;

public record RegistryWorkspaceBinding<T>(ResourceKey<Registry<T>> registryKey, Codec<T> codec, FlushAdapter<T> adapter, RegistryMutationHandler<T> mutationHandler, Function<ElementEntry<T>, CustomFields> customInitializer) {

    public RegistryWorkspaceBinding {
        adapter = adapter == null ? FlushAdapter.identity() : adapter;
        mutationHandler = mutationHandler == null ? RegistryMutationHandler.unsupported() : mutationHandler;
        customInitializer = customInitializer == null ? entry -> CustomFields.EMPTY : customInitializer;
    }

    public WorkspaceElementSnapshot toSnapshot(ElementEntry<T> entry, HolderLookup.Provider registries) {
        var ops = registries.createSerializationContext(JsonOps.INSTANCE);
        String dataJson = codec.encodeStart(ops, entry.data())
            .result()
            .map(JsonElement::toString)
            .orElse("{}");

        return new WorkspaceElementSnapshot(registryKey.identifier(), entry.id(), dataJson, entry.tags(), entry.custom());
    }

    public ElementEntry<T> fromSnapshot(WorkspaceElementSnapshot snapshot, HolderLookup.Provider registries) {
        var ops = registries.createSerializationContext(JsonOps.INSTANCE);
        JsonElement json = JsonParser.parseString(snapshot.dataJson());
        T data = codec.parse(ops, json).getOrThrow(message -> new IllegalArgumentException("Failed to decode workspace snapshot for " + snapshot.targetId() + ": " + message));
        return new ElementEntry<>(snapshot.targetId(), data, snapshot.tags(), snapshot.custom());
    }

    public ElementEntry<T> initializeEntry(ElementEntry<T> entry) {
        return entry.withCustom(customInitializer.apply(entry));
    }

    public String registryName() {
        return registryKey.identifier().toString();
    }

    public Identifier registryId() {
        return registryKey.identifier();
    }
}
