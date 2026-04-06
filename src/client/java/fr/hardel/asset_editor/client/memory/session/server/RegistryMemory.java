package fr.hardel.asset_editor.client.memory.session.server;

import fr.hardel.asset_editor.client.memory.core.ReadableMemory;
import fr.hardel.asset_editor.client.memory.core.SimpleMemory;
import fr.hardel.asset_editor.client.memory.core.Subscription;
import fr.hardel.asset_editor.workspace.CustomFields;
import fr.hardel.asset_editor.workspace.ElementEntry;
import fr.hardel.asset_editor.workspace.definition.WorkspaceDefinition;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

import java.util.Collection;
import java.util.Collections;
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
            registries = registries == null || registries.isEmpty()
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(registries));
        }

        public static Snapshot empty() {
            return new Snapshot(Map.of());
        }
    }

    private final SimpleMemory<Snapshot> memory = new SimpleMemory<>(Snapshot.empty());
    private final LinkedHashMap<String, RegistrySlot<?>> registrySlots = new LinkedHashMap<>();

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
        RegistrySlot<T> slot = slot(registryKey);

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
            ElementEntry<T> enriched = entry.withCustom(customInitializer.apply(entry));
            entries.put(id, enriched);
        });

        publishRegistry(registryName(registryKey), slot, entries);
    }

    public <T> ElementEntry<T> get(ResourceKey<Registry<T>> registry, Identifier id) {
        return typedRegistrySnapshot(registry).get(id);
    }

    public <T> List<ElementEntry<T>> allTypedElements(ResourceKey<Registry<T>> registry) {
        return List.copyOf(typedRegistrySnapshot(registry).values());
    }

    public <T> void put(ResourceKey<Registry<T>> registry, Identifier id, ElementEntry<T> entry) {
        RegistrySlot<T> slot = slot(registry);
        LinkedHashMap<Identifier, ElementEntry<T>> nextEntries = new LinkedHashMap<>(slot.snapshot());
        nextEntries.put(id, entry);
        publishRegistry(registryName(registry), slot, nextEntries);
    }

    public <T> void replaceAll(ResourceKey<Registry<T>> registry, Collection<ElementEntry<T>> entries) {
        RegistrySlot<T> slot = slot(registry);
        LinkedHashMap<Identifier, ElementEntry<T>> nextEntries = new LinkedHashMap<>();
        for (ElementEntry<T> entry : entries) {
            nextEntries.put(entry.id(), entry);
        }

        publishRegistry(registryName(registry), slot, nextEntries);
    }

    public synchronized <T> ReadableMemory<Map<Identifier, ElementEntry<T>>> observeTypedRegistry(ResourceKey<Registry<T>> registry) {
        return slot(registry).memory();
    }

    public synchronized <T> Map<Identifier, ElementEntry<T>> typedRegistrySnapshot(ResourceKey<Registry<T>> registry) {
        return slot(registry).snapshot();
    }

    public synchronized Map<String, Integer> entryCountsSnapshot() {
        LinkedHashMap<String, Integer> counts = new LinkedHashMap<>();
        registrySlots.forEach((registry, slot) -> {
            int size = slot.snapshot().size();
            if (size > 0) {
                counts.put(registry, size);
            }
        });
        return Map.copyOf(counts);
    }

    public synchronized void clearAll() {
        registrySlots.values().forEach(RegistrySlot::clear);
        memory.setSnapshot(Snapshot.empty());
    }

    public WorkspaceDefinition.SnapshotConsumer asSnapshotConsumer() {
        return this::snapshotFromRegistry;
    }

    static String registryName(ResourceKey<?> key) {
        return key.identifier().toString();
    }

    private synchronized <T> void publishRegistry(String name, RegistrySlot<T> slot, Map<Identifier, ElementEntry<T>> nextEntries) {
        slot.publish(immutableEntries(nextEntries));
        registrySlots.put(name, slot);
        memory.setSnapshot(buildSnapshot());
    }

    private synchronized Snapshot buildSnapshot() {
        if (registrySlots.isEmpty())
            return Snapshot.empty();

        LinkedHashMap<String, Map<Identifier, ElementEntry<?>>> registries = new LinkedHashMap<>();
        registrySlots.forEach((name, slot) -> registries.put(name, slot.wildcardSnapshot()));
        return new Snapshot(registries);
    }

    private synchronized <T> RegistrySlot<T> slot(ResourceKey<Registry<T>> registry) {
        String name = registryName(registry);
        if (!registrySlots.containsKey(name)) {
            var slot = new RegistrySlot<T>();
            registrySlots.put(name, slot);
            return slot;
        }
        return TypeSafeSlotMap.narrow(registrySlots.get(name));
    }

    private static <T> Map<Identifier, ElementEntry<T>> immutableEntries(Map<Identifier, ElementEntry<T>> entries) {
        if (entries == null || entries.isEmpty())
            return Map.of();

        LinkedHashMap<Identifier, ElementEntry<T>> copy = new LinkedHashMap<>(entries);
        return Collections.unmodifiableMap(copy);
    }

    /**
     * Type-safe heterogeneous container cast (Effective Java, Item 33).
     * The cast is safe because slots are only ever created and accessed via methods
     * that receive {@code ResourceKey<Registry<T>>}, guaranteeing type consistency
     * between the key and the slot content.
     */
    private static final class TypeSafeSlotMap {
        @SuppressWarnings("unchecked")
        static <T> RegistrySlot<T> narrow(RegistrySlot<?> slot) {
            return (RegistrySlot<T>) slot;
        }
    }

    private static final class RegistrySlot<T> {

        private final SimpleMemory<Map<Identifier, ElementEntry<T>>> memory = new SimpleMemory<>(Map.of());

        private ReadableMemory<Map<Identifier, ElementEntry<T>>> memory() {
            return memory;
        }

        private Map<Identifier, ElementEntry<T>> snapshot() {
            return memory.snapshot();
        }

        private Map<Identifier, ElementEntry<?>> wildcardSnapshot() {
            LinkedHashMap<Identifier, ElementEntry<?>> result = new LinkedHashMap<>(memory.snapshot());
            return Collections.unmodifiableMap(result);
        }

        private void publish(Map<Identifier, ElementEntry<T>> nextEntries) {
            memory.setSnapshot(nextEntries);
        }

        private void clear() {
            memory.setSnapshot(Map.of());
        }
    }
}
