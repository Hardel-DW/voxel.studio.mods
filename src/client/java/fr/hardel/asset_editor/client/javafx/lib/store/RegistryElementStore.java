package fr.hardel.asset_editor.client.javafx.lib.store;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagEntry;
import net.minecraft.tags.TagFile;

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

    private final Map<String, Map<Identifier, ElementEntry<?>>> vanilla = new HashMap<>();
    private final Map<String, Map<Identifier, ElementEntry<?>>> current = new HashMap<>();
    private final Map<SelectorKey, List<StoreSelector<?>>> selectors = new HashMap<>();
    private final Map<String, List<Runnable>> registryListeners = new HashMap<>();

    public <T> void snapshot(ResourceKey<Registry<T>> registryKey, HolderLookup.RegistryLookup<T> lookup) {
        snapshot(registryKey, lookup, entry -> CustomFields.EMPTY);
    }

    public <T> void snapshot(ResourceKey<Registry<T>> registryKey, HolderLookup.RegistryLookup<T> lookup,
                             Function<ElementEntry<T>, CustomFields> customInitializer) {
        String name = registryName(registryKey);
        Map<Identifier, ElementEntry<?>> entries = new LinkedHashMap<>();

        Map<Identifier, Set<Identifier>> tagsByElement = new HashMap<>();
        lookup.listTags().forEach(named -> {
            Identifier tagId = named.key().location();
            for (Holder<T> holder : named.stream().toList()) {
                holder.unwrapKey().ifPresent(
                        key -> tagsByElement.computeIfAbsent(key.identifier(), k -> new HashSet<>()).add(tagId));
            }
        });

        lookup.listElements().forEach(holder -> {
            Identifier id = holder.key().identifier();
            Set<Identifier> tags = tagsByElement.getOrDefault(id, Set.of());
            ElementEntry<T> entry = new ElementEntry<>(id, holder.value(), Set.copyOf(tags), CustomFields.EMPTY);
            entries.put(id, entry.withCustom(customInitializer.apply(entry)));
        });

        vanilla.put(name, Map.copyOf(entries));
        current.put(name, new LinkedHashMap<>(entries));
        notifyRegistryListeners(name);
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
    public <T> ElementEntry<T> getVanilla(ResourceKey<Registry<T>> registry, Identifier id) {
        var registryMap = vanilla.get(registryName(registry));
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
        var vanillaMap = vanilla.get(name);
        var currentMap = current.get(name);
        if (vanillaMap == null || currentMap == null)
            return;

        var ops = registries.createSerializationContext(JsonOps.INSTANCE);
        var gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

        var flushAdapter = adapter == null ? FlushAdapter.<T>identity() : adapter;
        flushElements(packRoot, name, vanillaMap, currentMap, codec, ops, gson, flushAdapter);
        flushTags(packRoot, name, vanillaMap, currentMap, gson, flushAdapter);
    }

    public void clearAll() {
        vanilla.clear();
        current.clear();
        selectors.values().forEach(list -> list.forEach(StoreSelector::dispose));
        selectors.clear();
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
            Map<Identifier, ElementEntry<?>> vanillaMap,
            Map<Identifier, ElementEntry<?>> currentMap,
            Codec<T> codec, com.mojang.serialization.DynamicOps<JsonElement> ops,
            com.google.gson.Gson gson, FlushAdapter<T> adapter) {
        for (var entry : currentMap.entrySet()) {
            Identifier id = entry.getKey();
            var currentEntry = adapter.prepare((ElementEntry<T>) entry.getValue());
            var vanillaEntry = vanillaMap.containsKey(id)
                    ? adapter.prepare((ElementEntry<T>) vanillaMap.get(id))
                    : null;

            if (vanillaEntry != null && Objects.equals(vanillaEntry.data(), currentEntry.data()))
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
            Map<Identifier, ElementEntry<?>> vanillaMap,
            Map<Identifier, ElementEntry<?>> currentMap,
            com.google.gson.Gson gson, FlushAdapter<?> adapter) {
        Map<Identifier, Set<Identifier>> vanillaTags = collectTagMemberships(vanillaMap, adapter);
        Map<Identifier, Set<Identifier>> currentTags = collectTagMemberships(currentMap, adapter);

        Set<Identifier> allTagIds = new HashSet<>();
        allTagIds.addAll(vanillaTags.keySet());
        allTagIds.addAll(currentTags.keySet());

        for (Identifier tagId : allTagIds) {
            Set<Identifier> vanillaMembers = vanillaTags.getOrDefault(tagId, Set.of());
            Set<Identifier> currentMembers = currentTags.getOrDefault(tagId, Set.of());

            if (vanillaMembers.equals(currentMembers))
                continue;

            Set<Identifier> removed = new HashSet<>(vanillaMembers);
            removed.removeAll(currentMembers);

            List<TagEntry> values = new ArrayList<>();
            if (removed.isEmpty()) {
                Set<Identifier> added = new HashSet<>(currentMembers);
                added.removeAll(vanillaMembers);
                for (Identifier id : added)
                    values.add(TagEntry.element(id));
            } else {
                for (Identifier id : currentMembers)
                    values.add(TagEntry.element(id));
            }

            JsonElement tagJson = TagFile.CODEC.encodeStart(JsonOps.INSTANCE, new TagFile(values, !removed.isEmpty()))
                    .getOrThrow(msg -> new IllegalStateException("Tag encode failed: " + msg));

            Path filePath = packRoot.resolve("data")
                    .resolve(tagId.getNamespace())
                    .resolve("tags")
                    .resolve(registry)
                    .resolve(tagId.getPath() + ".json");

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
