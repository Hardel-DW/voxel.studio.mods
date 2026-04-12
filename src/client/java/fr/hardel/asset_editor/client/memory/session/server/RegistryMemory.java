package fr.hardel.asset_editor.client.memory.session.server;

import fr.hardel.asset_editor.client.memory.core.ClientWorkspaceRegistries;
import fr.hardel.asset_editor.client.memory.core.ClientWorkspaceRegistry;
import fr.hardel.asset_editor.client.memory.core.ReadableMemory;
import fr.hardel.asset_editor.client.memory.core.SimpleMemory;
import fr.hardel.asset_editor.client.memory.core.Subscription;
import fr.hardel.asset_editor.workspace.flush.CustomFields;
import fr.hardel.asset_editor.workspace.flush.ElementEntry;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class RegistryMemory implements ReadableMemory<RegistryMemory.Snapshot> {

    public record Snapshot(Map<String, Map<Identifier, ElementEntry<?>>> registries) {

        public Snapshot {
            registries = registries == null || registries.isEmpty()
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(registries));
        }

        public static Snapshot empty() {
            return new Snapshot(Map.of());
        }
    }

    private final SimpleMemory<Snapshot> memory = new SimpleMemory<>(Snapshot.empty());
    private final LinkedHashMap<String, ClientWorkspaceRegistry<?>> registries = new LinkedHashMap<>();

    public RegistryMemory() {
        this(ClientWorkspaceRegistries.all());
    }

    public RegistryMemory(Collection<ClientWorkspaceRegistry<?>> registries) {
        registries.forEach(this::register);
    }

    @Override
    public Snapshot snapshot() {
        return memory.snapshot();
    }

    @Override
    public Subscription subscribe(Runnable listener) {
        return memory.subscribe(listener);
    }

    public <T> void snapshotFromRegistry(ClientWorkspaceRegistry<T> registryHandle, Registry<T> registry) {
        Map<Identifier, Set<Identifier>> tagsByElement = new HashMap<>();
        registry.listTags().forEach(named -> {
            Identifier tagId = named.key().location();
            for (Holder<T> holder : named.stream().toList()) {
                holder.unwrapKey().ifPresent(
                    key -> tagsByElement.computeIfAbsent(key.identifier(), ignored -> new HashSet<>()).add(tagId));
            }
        });

        LinkedHashMap<Identifier, ElementEntry<T>> entries = new LinkedHashMap<>();
        registry.listElements().forEach(holder -> {
            Identifier id = holder.key().identifier();
            Set<Identifier> tags = tagsByElement.getOrDefault(id, Set.of());
            ElementEntry<T> entry = new ElementEntry<>(id, holder.value(), Set.copyOf(tags), CustomFields.EMPTY);
            entries.put(id, registryHandle.definition().initializeEntry(entry));
        });

        publishRegistry(registryHandle, entries);
    }

    public <T> ElementEntry<T> get(ClientWorkspaceRegistry<T> registry, Identifier id) {
        return typedRegistrySnapshot(registry).get(id);
    }

    public <T> List<ElementEntry<T>> allTypedElements(ClientWorkspaceRegistry<T> registry) {
        return List.copyOf(typedRegistrySnapshot(registry).values());
    }

    public <T> void put(ClientWorkspaceRegistry<T> registry, Identifier id, ElementEntry<T> entry) {
        LinkedHashMap<Identifier, ElementEntry<T>> nextEntries = new LinkedHashMap<>(registry.snapshot());
        nextEntries.put(id, entry);
        publishRegistry(registry, nextEntries);
    }

    public <T> void replaceAll(ClientWorkspaceRegistry<T> registry, Collection<ElementEntry<T>> entries) {
        LinkedHashMap<Identifier, ElementEntry<T>> nextEntries = new LinkedHashMap<>();
        for (ElementEntry<T> entry : entries) {
            nextEntries.put(entry.id(), entry);
        }

        publishRegistry(registry, nextEntries);
    }

    public <T> ReadableMemory<Map<Identifier, ElementEntry<T>>> observeTypedRegistry(ClientWorkspaceRegistry<T> registry) {
        return registry.memory();
    }

    public <T> Map<Identifier, ElementEntry<T>> typedRegistrySnapshot(ClientWorkspaceRegistry<T> registry) {
        return registry.snapshot();
    }

    public synchronized Map<String, Integer> entryCountsSnapshot() {
        LinkedHashMap<String, Integer> counts = new LinkedHashMap<>();
        registries.forEach((registryId, registry) -> {
            int size = registry.snapshot().size();
            if (size > 0) {
                counts.put(registryId, size);
            }
        });
        return Map.copyOf(counts);
    }

    public synchronized void clearAll() {
        registries.values().forEach(ClientWorkspaceRegistry::clear);
        memory.setSnapshot(Snapshot.empty());
    }

    private synchronized <T> void publishRegistry(ClientWorkspaceRegistry<T> registry, Map<Identifier, ElementEntry<T>> nextEntries) {
        registry.publish(immutableEntries(nextEntries));
        memory.setSnapshot(buildSnapshot());
    }

    private synchronized Snapshot buildSnapshot() {
        if (registries.isEmpty())
            return Snapshot.empty();

        LinkedHashMap<String, Map<Identifier, ElementEntry<?>>> snapshots = new LinkedHashMap<>();
        registries.forEach((name, registry) -> snapshots.put(name, registry.wildcardSnapshot()));
        return new Snapshot(snapshots);
    }

    private static <T> Map<Identifier, ElementEntry<T>> immutableEntries(Map<Identifier, ElementEntry<T>> entries) {
        if (entries == null || entries.isEmpty())
            return Map.of();

        LinkedHashMap<Identifier, ElementEntry<T>> copy = new LinkedHashMap<>(entries);
        return Collections.unmodifiableMap(copy);
    }

    private void register(ClientWorkspaceRegistry<?> registry) {
        String registryId = registryName(registry);
        if (registries.putIfAbsent(registryId, registry) != null)
            throw new IllegalStateException("Duplicate client workspace registry: " + registryId);
    }

    public static String registryName(ClientWorkspaceRegistry<?> registry) {
        return registry.registryId().toString();
    }
}
