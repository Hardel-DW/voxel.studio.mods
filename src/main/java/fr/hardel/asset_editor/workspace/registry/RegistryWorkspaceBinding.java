package fr.hardel.asset_editor.workspace.registry;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import fr.hardel.asset_editor.network.workspace.WorkspaceElementSnapshot;
import fr.hardel.asset_editor.store.CustomFields;
import fr.hardel.asset_editor.store.ElementEntry;
import fr.hardel.asset_editor.store.adapter.FlushAdapter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

import java.util.function.Function;

public record RegistryWorkspaceBinding<T>(ResourceKey<Registry<T>> registryKey, Codec<T> codec, FlushAdapter<T> adapter, Function<ElementEntry<T>, CustomFields> customInitializer) {

    public RegistryWorkspaceBinding {
        adapter = adapter == null ? FlushAdapter.identity() : adapter;
        customInitializer = customInitializer == null ? entry -> CustomFields.EMPTY : customInitializer;
    }

    public WorkspaceElementSnapshot toSnapshot(ElementEntry<T> entry, HolderLookup.Provider registries) {
        return new WorkspaceElementSnapshot(registryKey.identifier(), entry.id(), encode(entry, registries), entry.tags(), entry.custom());
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

    public String encode(ElementEntry<T> entry, HolderLookup.Provider registries) {
        var ops = registries.createSerializationContext(JsonOps.INSTANCE);
        return codec.encodeStart(ops, entry.data())
            .result()
            .map(JsonElement::toString)
            .orElseThrow(() -> new IllegalArgumentException("Failed to encode workspace snapshot for " + entry.id()));
    }

    public String registryName() {
        return registryKey.identifier().toString();
    }

    public Identifier registryId() {
        return registryKey.identifier();
    }

    public void snapshotFromAccess(net.minecraft.core.RegistryAccess registryAccess, RegistrySnapshotConsumer consumer) {
        registryAccess.lookup(registryKey).ifPresent(registry ->
            consumer.accept(registryKey, registry, entry -> customInitializer.apply(entry))
        );
    }

    @FunctionalInterface
    public interface RegistrySnapshotConsumer {
        <T> void accept(ResourceKey<Registry<T>> registryKey, Registry<T> registry, Function<ElementEntry<T>, CustomFields> customInitializer);
    }
}
