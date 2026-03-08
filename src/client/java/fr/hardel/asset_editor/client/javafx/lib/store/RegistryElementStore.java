package fr.hardel.asset_editor.client.javafx.lib.store;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;

public final class RegistryElementStore {

    public record ElementEntry<T>(Identifier id, T data, Set<Identifier> tags) {
        public ElementEntry<T> withData(T newData) {
            return new ElementEntry<>(id, newData, tags);
        }

        public ElementEntry<T> toggleTag(Identifier tagId) {
            var copy = new HashSet<>(tags);
            if (!copy.remove(tagId))
                copy.add(tagId);
            return new ElementEntry<>(id, data, Set.copyOf(copy));
        }
    }

    private record SelectorKey(String registry, Identifier elementId) {
    }

    private final Map<String, Map<Identifier, ElementEntry<?>>> vanilla = new HashMap<>();
    private final Map<String, Map<Identifier, ElementEntry<?>>> current = new HashMap<>();
    private final Map<SelectorKey, List<StoreSelector<?>>> selectors = new HashMap<>();
    private final Map<String, List<Runnable>> registryListeners = new HashMap<>();

    public <T> void snapshot(ResourceKey<Registry<T>> registryKey, HolderLookup.RegistryLookup<T> lookup) {
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
            entries.put(id, new ElementEntry<>(id, holder.value(), Set.copyOf(tags)));
        });

        vanilla.put(name, Map.copyOf(entries));
        current.put(name, new LinkedHashMap<>(entries));
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
        current.computeIfAbsent(name, k -> new LinkedHashMap<>()).put(id, entry);
        notifySelectors(name, id, entry);
        notifyRegistryListeners(name);
    }

    public <T> void subscribeRegistry(ResourceKey<Registry<T>> registry, Runnable listener) {
        registryListeners.computeIfAbsent(registryName(registry), k -> new ArrayList<>()).add(listener);
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
            HolderLookup.Provider registries) {
        String name = registryName(registry);
        var vanillaMap = vanilla.get(name);
        var currentMap = current.get(name);
        if (vanillaMap == null || currentMap == null)
            return;

        var ops = registries.createSerializationContext(JsonOps.INSTANCE);
        var gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

        flushElements(packRoot, name, vanillaMap, currentMap, codec, ops, gson);
        flushTags(packRoot, name, vanillaMap, currentMap, gson);
    }

    public void clearAll() {
        vanilla.clear();
        current.clear();
        selectors.values().forEach(list -> list.forEach(StoreSelector::dispose));
        selectors.clear();
        registryListeners.clear();
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
            com.google.gson.Gson gson) {
        for (var entry : currentMap.entrySet()) {
            Identifier id = entry.getKey();
            var currentEntry = (ElementEntry<T>) entry.getValue();
            var vanillaEntry = (ElementEntry<T>) vanillaMap.get(id);

            if (vanillaEntry != null && vanillaEntry.data() == currentEntry.data())
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
            com.google.gson.Gson gson) {
        Map<Identifier, Set<Identifier>> vanillaTags = collectTagMemberships(vanillaMap);
        Map<Identifier, Set<Identifier>> currentTags = collectTagMemberships(currentMap);

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

            JsonObject tagJson = new JsonObject();

            if (removed.isEmpty()) {
                tagJson.addProperty("replace", false);
                JsonArray values = new JsonArray();
                Set<Identifier> added = new HashSet<>(currentMembers);
                added.removeAll(vanillaMembers);
                for (Identifier id : added)
                    values.add(id.toString());
                tagJson.add("values", values);
            } else {
                tagJson.addProperty("replace", true);
                JsonArray values = new JsonArray();
                for (Identifier id : currentMembers)
                    values.add(id.toString());
                tagJson.add("values", values);
            }

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

    private Map<Identifier, Set<Identifier>> collectTagMemberships(Map<Identifier, ElementEntry<?>> elements) {
        Map<Identifier, Set<Identifier>> result = new HashMap<>();
        for (var entry : elements.values()) {
            for (Identifier tagId : entry.tags()) {
                result.computeIfAbsent(tagId, k -> new HashSet<>()).add(entry.id());
            }
        }
        return result;
    }

    static String registryName(ResourceKey<?> key) {
        return key.identifier().getPath();
    }
}
