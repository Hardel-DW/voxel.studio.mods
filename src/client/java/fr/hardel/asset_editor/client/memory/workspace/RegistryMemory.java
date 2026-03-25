package fr.hardel.asset_editor.client.memory.workspace;

import fr.hardel.asset_editor.client.memory.core.ReadableMemory;
import fr.hardel.asset_editor.client.memory.core.SimpleMemory;
import fr.hardel.asset_editor.client.memory.core.Subscription;
import fr.hardel.asset_editor.store.CustomFields;
import fr.hardel.asset_editor.store.ElementEntry;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public final class RegistryMemory implements ReadableMemory<RegistryMemory.Snapshot> {

    public record Snapshot(Map<String, Map<Identifier, ElementEntry<?>>> registries) {

        public Snapshot {
            registries = copyRegistries(registries);
        }

        public static Snapshot empty() {
            return new Snapshot(Map.of());
        }
    }

    private final SimpleMemory<Snapshot> memory = new SimpleMemory<>(Snapshot.empty());

    @Override
    public Snapshot snapshot() {
        return memory.snapshot();
    }

    @Override
    public Subscription subscribe(Runnable listener) {
        return memory.subscribe(listener);
    }

    public <T> void snapshotFromRegistry(ResourceKey<Registry<T>> registryKey, Registry<T> registry,
        Function<ElementEntry<T>, CustomFields> customInitializer) {
        String name = registryName(registryKey);

        Map<Identifier, Set<Identifier>> tagsByElement = new HashMap<>();
        registry.listTags().forEach(named -> {
            Identifier tagId = named.key().location();
            for (Holder<T> holder : named.stream().toList()) {
                holder.unwrapKey().ifPresent(
                    key -> tagsByElement.computeIfAbsent(key.identifier(), ignored -> new HashSet<>()).add(tagId));
            }
        });

        LinkedHashMap<Identifier, ElementEntry<?>> entries = new LinkedHashMap<>();
        registry.listElements().forEach(holder -> {
            Identifier id = holder.key().identifier();
            Set<Identifier> tags = tagsByElement.getOrDefault(id, Set.of());
            ElementEntry<T> entry = new ElementEntry<>(id, holder.value(), Set.copyOf(tags), CustomFields.EMPTY);
            ElementEntry<T> enriched = entry.withCustom(customInitializer.apply(entry));
            entries.put(id, enriched);
        });

        memory.update(state -> {
            LinkedHashMap<String, Map<Identifier, ElementEntry<?>>> next = new LinkedHashMap<>(state.registries());
            next.put(name, entries);
            return new Snapshot(next);
        });
    }

    @SuppressWarnings("unchecked")
    public <T> ElementEntry<T> get(ResourceKey<Registry<T>> registry, Identifier id) {
        Map<Identifier, ElementEntry<?>> registryMap = snapshot().registries().get(registryName(registry));
        if (registryMap == null)
            return null;
        return (ElementEntry<T>) registryMap.get(id);
    }

    @SuppressWarnings("unchecked")
    public <T> List<ElementEntry<T>> allTypedElements(ResourceKey<Registry<T>> registry) {
        Map<Identifier, ElementEntry<?>> registryMap = snapshot().registries().get(registryName(registry));
        if (registryMap == null)
            return List.of();
        return registryMap.values().stream().map(entry -> (ElementEntry<T>) entry).toList();
    }

    public <T> void put(ResourceKey<Registry<T>> registry, Identifier id, ElementEntry<T> entry) {
        String name = registryName(registry);
        memory.update(state -> {
            LinkedHashMap<String, Map<Identifier, ElementEntry<?>>> next = new LinkedHashMap<>(state.registries());
            LinkedHashMap<Identifier, ElementEntry<?>> registryMap = new LinkedHashMap<>(next.getOrDefault(name, Map.of()));
            registryMap.put(id, entry);
            next.put(name, registryMap);
            return new Snapshot(next);
        });
    }

    public <T> void replaceAll(ResourceKey<Registry<T>> registry, Collection<ElementEntry<T>> entries) {
        String name = registryName(registry);
        LinkedHashMap<Identifier, ElementEntry<?>> nextEntries = new LinkedHashMap<>();
        for (ElementEntry<T> entry : entries) {
            nextEntries.put(entry.id(), entry);
        }

        memory.update(state -> {
            LinkedHashMap<String, Map<Identifier, ElementEntry<?>>> next = new LinkedHashMap<>(state.registries());
            next.put(name, nextEntries);
            return new Snapshot(next);
        });
    }

    public Map<String, Integer> entryCountsSnapshot() {
        LinkedHashMap<String, Integer> counts = new LinkedHashMap<>();
        snapshot().registries().forEach((registry, entries) -> counts.put(registry, entries.size()));
        return Map.copyOf(counts);
    }

    public void clearAll() {
        memory.setSnapshot(Snapshot.empty());
    }

    static String registryName(ResourceKey<?> key) {
        return key.identifier().toString();
    }

    private static Map<String, Map<Identifier, ElementEntry<?>>> copyRegistries(
        Map<String, Map<Identifier, ElementEntry<?>>> registries) {
        if (registries == null || registries.isEmpty())
            return Map.of();

        LinkedHashMap<String, Map<Identifier, ElementEntry<?>>> copy = new LinkedHashMap<>();
        registries.forEach((registry, entries) -> {
            LinkedHashMap<Identifier, ElementEntry<?>> registryCopy = new LinkedHashMap<>();
            if (entries != null)
                registryCopy.putAll(entries);
            copy.put(registry, java.util.Collections.unmodifiableMap(registryCopy));
        });
        return java.util.Collections.unmodifiableMap(copy);
    }
}
