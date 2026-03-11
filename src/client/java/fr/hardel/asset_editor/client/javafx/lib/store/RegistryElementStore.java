package fr.hardel.asset_editor.client.javafx.lib.store;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.ResourceManager;
import fr.hardel.asset_editor.tag.ExtendedTagFile;
import net.minecraft.tags.TagEntry;
import net.minecraft.tags.TagLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;

public final class RegistryElementStore {

    public record CustomFields(Map<String, Object> values) {
        public static final CustomFields EMPTY = new CustomFields(Map.of());

        public CustomFields {
            if (!values.isEmpty()) {
                Map<String, Object> normalized = new LinkedHashMap<>();
                values.forEach((key, value) -> {
                    if (key != null && value != null) {
                        normalized.put(key, normalizeValue(value));
                    }
                });
                values = normalized.isEmpty() ? Map.of() : Map.copyOf(normalized);
            } else {
                values = Map.of();
            }
        }

        public CustomFields with(String key, Object value) {
            if (value == null) {
                return without(key);
            }

            Map<String, Object> copy = new LinkedHashMap<>(values);
            copy.put(key, normalizeValue(value));
            return new CustomFields(copy);
        }

        public CustomFields without(String key) {
            if (!values.containsKey(key)) {
                return this;
            }

            Map<String, Object> copy = new LinkedHashMap<>(values);
            copy.remove(key);
            return copy.isEmpty() ? EMPTY : new CustomFields(copy);
        }

        public String getString(String key, String fallback) {
            Object value = values.get(key);
            return value instanceof String string ? string : fallback;
        }

        @SuppressWarnings("unchecked")
        public Set<String> getStringSet(String key) {
            Object value = values.get(key);
            if (!(value instanceof Set<?> set)) {
                return Set.of();
            }
            if (set.stream().anyMatch(element -> !(element instanceof String))) {
                return Set.of();
            }
            return (Set<String>) set;
        }

        private static Object normalizeValue(Object value) {
            if (value instanceof Set<?> set) {
                return Set.copyOf(set);
            }
            if (value instanceof List<?> list) {
                return List.copyOf(list);
            }
            if (value instanceof Map<?, ?> map) {
                return Map.copyOf(map);
            }
            return value;
        }
    }

    public record ElementEntry<T>(Identifier id, T data, Set<Identifier> tags, CustomFields custom) {
        public ElementEntry<T> withData(T newData) {
            return new ElementEntry<>(id, newData, tags, custom);
        }

        public ElementEntry<T> withTags(Set<Identifier> newTags) {
            return new ElementEntry<>(id, data, Set.copyOf(newTags), custom);
        }

        public ElementEntry<T> withCustom(CustomFields newCustom) {
            return new ElementEntry<>(id, data, tags, newCustom);
        }

        public ElementEntry<T> toggleTag(Identifier tagId) {
            var copy = new HashSet<>(tags);
            if (!copy.remove(tagId))
                copy.add(tagId);
            return new ElementEntry<>(id, data, Set.copyOf(copy), custom);
        }
    }

    public interface FlushAdapter<T> {
        ElementEntry<T> prepare(ElementEntry<T> entry);

        static <T> FlushAdapter<T> identity() {
            return entry -> entry;
        }
    }

    private record SelectorKey(String registry, Identifier elementId) {
    }

    private record RegistryRef<T>(ResourceKey<Registry<T>> key, Registry<T> registry) {
    }

    private final Map<String, Map<Identifier, ElementEntry<?>>> reference = new HashMap<>();
    private final Map<String, Map<Identifier, ElementEntry<?>>> current = new HashMap<>();
    private final Map<String, RegistryRef<?>> registryRefs = new HashMap<>();
    private final Map<SelectorKey, List<StoreSelector<?>>> selectors = new HashMap<>();
    private final Map<String, List<Runnable>> registryListeners = new HashMap<>();

    public <T> void snapshot(ResourceKey<Registry<T>> registryKey, Registry<T> registry,
                             ResourceManager referenceResources) {
        snapshot(registryKey, registry, referenceResources, entry -> CustomFields.EMPTY);
    }

    public <T> void snapshot(ResourceKey<Registry<T>> registryKey, Registry<T> registry,
                             ResourceManager referenceResources,
                             Function<ElementEntry<T>, CustomFields> customInitializer) {
        String name = registryName(registryKey);
        registryRefs.put(name, new RegistryRef<>(registryKey, registry));

        Map<Identifier, Set<Identifier>> refTagsByElement = loadTags(registryKey, referenceResources, registry);

        Map<Identifier, Set<Identifier>> curTagsByElement = new HashMap<>();
        registry.listTags().forEach(named -> {
            Identifier tagId = named.key().location();
            for (Holder<T> holder : named.stream().toList()) {
                holder.unwrapKey().ifPresent(
                        key -> curTagsByElement.computeIfAbsent(key.identifier(), k -> new HashSet<>()).add(tagId));
            }
        });

        Map<Identifier, ElementEntry<?>> refEntries = new LinkedHashMap<>();
        Map<Identifier, ElementEntry<?>> curEntries = new LinkedHashMap<>();

        registry.listElements().forEach(holder -> {
            Identifier id = holder.key().identifier();
            Set<Identifier> refTags = refTagsByElement.getOrDefault(id, Set.of());
            Set<Identifier> curTags = curTagsByElement.getOrDefault(id, Set.of());

            ElementEntry<T> refEntry = new ElementEntry<>(id, holder.value(), Set.copyOf(refTags), CustomFields.EMPTY);
            refEntries.put(id, refEntry.withCustom(customInitializer.apply(refEntry)));

            ElementEntry<T> curEntry = new ElementEntry<>(id, holder.value(), Set.copyOf(curTags), CustomFields.EMPTY);
            curEntries.put(id, curEntry.withCustom(customInitializer.apply(curEntry)));
        });

        reference.put(name, Map.copyOf(refEntries));
        current.put(name, new LinkedHashMap<>(curEntries));
        notifyRegistryListeners(name);
    }

    public void reloadReference(ResourceManager referenceResources) {
        for (var ref : registryRefs.values()) {
            reloadReferenceFor(ref, referenceResources);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> ElementEntry<T> get(ResourceKey<Registry<T>> registry, Identifier id) {
        var registryMap = current.get(registryName(registry));
        if (registryMap == null)
            return null;
        return (ElementEntry<T>) registryMap.get(id);
    }

    public <T> void put(ResourceKey<Registry<T>> registry, Identifier id, ElementEntry<T> entry) {
        String name = registryName(registry);
        var registryMap = current.computeIfAbsent(name, k -> new LinkedHashMap<>());
        var previous = registryMap.put(id, entry);
        if (Objects.equals(previous, entry))
            return;
        notifySelectors(name, id, entry);
        notifyRegistryListeners(name);
    }

    public <T> void subscribeRegistry(ResourceKey<Registry<T>> registry, Runnable listener) {
        var list = registryListeners.computeIfAbsent(registryName(registry), k -> new ArrayList<>());
        if (!list.contains(listener)) list.add(listener);
    }

    public <T> void unsubscribeRegistry(ResourceKey<Registry<T>> registry, Runnable listener) {
        var list = registryListeners.get(registryName(registry));
        if (list != null) {
            list.remove(listener);
            if (list.isEmpty()) registryListeners.remove(registryName(registry));
        }
    }

    @SuppressWarnings("unchecked")
    public <T> ElementEntry<T> getReference(ResourceKey<Registry<T>> registry, Identifier id) {
        var registryMap = reference.get(registryName(registry));
        if (registryMap == null)
            return null;
        return (ElementEntry<T>) registryMap.get(id);
    }

    public <T> Collection<ElementEntry<?>> allElements(ResourceKey<Registry<T>> registry) {
        var registryMap = current.get(registryName(registry));
        return registryMap == null ? List.of() : registryMap.values();
    }

    @SuppressWarnings("unchecked")
    public <T> List<ElementEntry<T>> allTypedElements(ResourceKey<Registry<T>> registry) {
        var registryMap = current.get(registryName(registry));
        if (registryMap == null) return List.of();
        return registryMap.values().stream().map(e -> (ElementEntry<T>) e).toList();
    }

    public <T, R> StoreSelector<R> select(ResourceKey<Registry<T>> registry, Identifier id,
            Function<ElementEntry<T>, R> extractor) {
        String name = registryName(registry);
        ElementEntry<T> entry = get(registry, id);
        var selector = new StoreSelector<>(extractor, entry);
        selectors.computeIfAbsent(new SelectorKey(name, id), k -> new ArrayList<>()).add(selector);
        return selector;
    }

    public void disposeSelectors(List<StoreSelector<?>> toDispose) {
        for (var selector : toDispose) {
            selector.dispose();
        }
        for (var list : selectors.values()) {
            list.removeAll(toDispose);
        }
        selectors.entrySet().removeIf(e -> e.getValue().isEmpty());
    }

    public <T> void flush(Path packRoot, ResourceKey<Registry<T>> registry, Codec<T> codec,
            HolderLookup.Provider registries, FlushAdapter<T> adapter) {
        String name = registryName(registry);
        var referenceMap = reference.get(name);
        var currentMap = current.get(name);
        if (referenceMap == null || currentMap == null)
            return;

        var ops = registries.createSerializationContext(JsonOps.INSTANCE);
        var gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

        var flushAdapter = adapter == null ? FlushAdapter.<T>identity() : adapter;
        flushElements(packRoot, name, referenceMap, currentMap, codec, ops, gson, flushAdapter);
        flushTags(packRoot, name, referenceMap, currentMap, gson, flushAdapter);
    }

    public void clearAll() {
        reference.clear();
        current.clear();
        registryRefs.clear();
        selectors.values().forEach(list -> list.forEach(StoreSelector::dispose));
        selectors.clear();
    }

    private <T> void reloadReferenceFor(RegistryRef<T> ref, ResourceManager referenceResources) {
        String name = registryName(ref.key());
        var refMap = reference.get(name);
        if (refMap == null) return;

        Map<Identifier, Set<Identifier>> refTagsByElement = loadTags(ref.key(), referenceResources, ref.registry());

        Map<Identifier, ElementEntry<?>> updated = new LinkedHashMap<>();
        for (var entry : refMap.entrySet()) {
            Set<Identifier> tags = refTagsByElement.getOrDefault(entry.getKey(), Set.of());
            updated.put(entry.getKey(), entry.getValue().withTags(tags));
        }
        reference.put(name, Map.copyOf(updated));
    }

    @SuppressWarnings("unchecked")
    private <T> Map<Identifier, Set<Identifier>> loadTags(ResourceKey<Registry<T>> registryKey,
                                                           ResourceManager resourceManager,
                                                           Registry<T> registry) {
        var loader = new TagLoader<>(
                (TagLoader.ElementLookup<Holder<T>>) TagLoader.ElementLookup.fromFrozenRegistry(registry),
                Registries.tagsDirPath(registryKey));

        Map<Identifier, Set<Identifier>> tagsByElement = new HashMap<>();
        loader.build(loader.load(resourceManager)).forEach((tagId, holders) -> {
            for (var holder : holders) {
                holder.unwrapKey().ifPresent(key ->
                        tagsByElement.computeIfAbsent(key.identifier(), k -> new HashSet<>()).add(tagId));
            }
        });
        return tagsByElement;
    }

    private void notifySelectors(String registry, Identifier id, ElementEntry<?> entry) {
        var list = selectors.get(new SelectorKey(registry, id));
        if (list == null) return;
        for (var selector : list) selector.recompute(entry);
    }

    private void notifyRegistryListeners(String registry) {
        var list = registryListeners.get(registry);
        if (list != null) list.forEach(Runnable::run);
    }

    @SuppressWarnings("unchecked")
    private <T> void flushElements(Path packRoot, String registry,
            Map<Identifier, ElementEntry<?>> referenceMap,
            Map<Identifier, ElementEntry<?>> currentMap,
            Codec<T> codec, DynamicOps<JsonElement> ops,
            Gson gson, FlushAdapter<T> adapter) {
        for (var entry : currentMap.entrySet()) {
            Identifier id = entry.getKey();
            var currentEntry = adapter.prepare((ElementEntry<T>) entry.getValue());
            var referenceEntry = referenceMap.containsKey(id)
                    ? adapter.prepare((ElementEntry<T>) referenceMap.get(id))
                    : null;

            if (referenceEntry != null && Objects.equals(referenceEntry.data(), currentEntry.data()))
                continue;

            JsonElement json = codec.encodeStart(ops, currentEntry.data())
                    .getOrThrow(msg -> new IllegalStateException("Encode failed: " + msg));

            Path filePath = packRoot.resolve("data")
                    .resolve(id.getNamespace())
                    .resolve(registry)
                    .resolve(id.getPath() + ".json");

            try {
                Files.createDirectories(filePath.getParent());
                Files.writeString(filePath, gson.toJson(json));
            } catch (IOException e) {
                System.err.println("Failed to write element: " + filePath + " - " + e.getMessage());
            }
        }
    }

    private void flushTags(Path packRoot, String registry,
            Map<Identifier, ElementEntry<?>> referenceMap,
            Map<Identifier, ElementEntry<?>> currentMap,
            Gson gson, FlushAdapter<?> adapter) {
        Map<Identifier, Set<Identifier>> refTags = collectTagMemberships(referenceMap, adapter);
        Map<Identifier, Set<Identifier>> curTags = collectTagMemberships(currentMap, adapter);

        Set<Identifier> allTagIds = new HashSet<>();
        allTagIds.addAll(refTags.keySet());
        allTagIds.addAll(curTags.keySet());

        for (Identifier tagId : allTagIds) {
            Set<Identifier> referenceMembers = refTags.getOrDefault(tagId, Set.of());
            Set<Identifier> currentMembers = curTags.getOrDefault(tagId, Set.of());

            Path filePath = packRoot.resolve("data")
                    .resolve(tagId.getNamespace())
                    .resolve("tags")
                    .resolve(registry)
                    .resolve(tagId.getPath() + ".json");

            if (referenceMembers.equals(currentMembers)) {
                try {
                    Files.deleteIfExists(filePath);
                } catch (IOException e) {
                    System.err.println("Failed to delete stale tag: " + filePath + " - " + e.getMessage());
                }
                continue;
            }

            Set<Identifier> added = new HashSet<>(currentMembers);
            added.removeAll(referenceMembers);

            Set<Identifier> removed = new HashSet<>(referenceMembers);
            removed.removeAll(currentMembers);

            List<TagEntry> values = added.stream().map(TagEntry::element).toList();
            List<TagEntry> exclude = removed.stream().map(TagEntry::element).toList();

            JsonElement tagJson = ExtendedTagFile.CODEC.encodeStart(JsonOps.INSTANCE, new ExtendedTagFile(values, exclude, false))
                    .getOrThrow(msg -> new IllegalStateException("Tag encode failed: " + msg));

            try {
                Files.createDirectories(filePath.getParent());
                Files.writeString(filePath, gson.toJson(tagJson));
            } catch (IOException e) {
                System.err.println("Failed to write tag: " + filePath + " - " + e.getMessage());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Map<Identifier, Set<Identifier>> collectTagMemberships(Map<Identifier, ElementEntry<?>> elements,
                                                                   FlushAdapter<?> adapter) {
        Map<Identifier, Set<Identifier>> result = new HashMap<>();
        for (var entry : elements.values()) {
            var prepared = ((FlushAdapter<Object>) adapter).prepare((ElementEntry<Object>) entry);
            for (Identifier tagId : prepared.tags()) {
                result.computeIfAbsent(tagId, k -> new HashSet<>()).add(prepared.id());
            }
        }
        return result;
    }

    static String registryName(ResourceKey<?> key) {
        return key.identifier().getPath();
    }
}
