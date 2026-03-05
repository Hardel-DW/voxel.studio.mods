package fr.hardel.asset_editor.client.javafx.lib.store;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class RegistryElementStore {

    public record ElementEntry<T>(Identifier id, T data, Set<Identifier> tags) {
        public ElementEntry<T> withData(T newData) {
            return new ElementEntry<>(id, newData, tags);
        }

        public ElementEntry<T> withTag(Identifier tagId) {
            var copy = new HashSet<>(tags);
            copy.add(tagId);
            return new ElementEntry<>(id, data, Set.copyOf(copy));
        }

        public ElementEntry<T> withoutTag(Identifier tagId) {
            var copy = new HashSet<>(tags);
            copy.remove(tagId);
            return new ElementEntry<>(id, data, Set.copyOf(copy));
        }

        public ElementEntry<T> toggleTag(Identifier tagId) {
            return tags.contains(tagId) ? withoutTag(tagId) : withTag(tagId);
        }
    }

    private final Map<String, Map<Identifier, ElementEntry<?>>> vanilla = new HashMap<>();
    private final Map<String, Map<Identifier, ElementEntry<?>>> current = new HashMap<>();
    private final IntegerProperty version = new SimpleIntegerProperty(0);

    public IntegerProperty versionProperty() {
        return version;
    }

    public <T> void snapshot(String registryName, HolderLookup.RegistryLookup<T> lookup) {
        Map<Identifier, ElementEntry<?>> entries = new LinkedHashMap<>();

        Map<Identifier, Set<Identifier>> tagsByElement = new HashMap<>();
        lookup.listTags().forEach(named -> {
            Identifier tagId = named.key().location();
            for (Holder<T> holder : named.stream().toList()) {
                holder.unwrapKey().ifPresent(key ->
                        tagsByElement.computeIfAbsent(key.identifier(), k -> new HashSet<>()).add(tagId));
            }
        });

        lookup.listElements().forEach(holder -> {
            Identifier id = holder.key().identifier();
            Set<Identifier> tags = tagsByElement.getOrDefault(id, Set.of());
            entries.put(id, new ElementEntry<>(id, holder.value(), Set.copyOf(tags)));
        });

        vanilla.put(registryName, Map.copyOf(entries));
        current.put(registryName, new LinkedHashMap<>(entries));
    }

    @SuppressWarnings("unchecked")
    public <T> ElementEntry<T> get(String registry, Identifier id) {
        var registryMap = current.get(registry);
        if (registryMap == null) return null;
        return (ElementEntry<T>) registryMap.get(id);
    }

    public <T> void put(String registry, Identifier id, ElementEntry<T> entry) {
        current.computeIfAbsent(registry, k -> new LinkedHashMap<>()).put(id, entry);
    }

    @SuppressWarnings("unchecked")
    public <T> ElementEntry<T> getVanilla(String registry, Identifier id) {
        var registryMap = vanilla.get(registry);
        if (registryMap == null) return null;
        return (ElementEntry<T>) registryMap.get(id);
    }

    public Collection<ElementEntry<?>> allElements(String registry) {
        var registryMap = current.get(registry);
        return registryMap == null ? List.of() : registryMap.values();
    }

    public void incrementVersion() {
        version.set(version.get() + 1);
    }

    public <T> void flush(Path packRoot, String registry, Codec<T> codec, HolderLookup.Provider registries) {
        var vanillaMap = vanilla.get(registry);
        var currentMap = current.get(registry);
        if (vanillaMap == null || currentMap == null) return;

        var ops = registries.createSerializationContext(JsonOps.INSTANCE);
        var gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

        flushElements(packRoot, registry, vanillaMap, currentMap, codec, ops, gson);
        flushTags(packRoot, registry, vanillaMap, currentMap, gson);
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

            if (vanillaEntry != null && vanillaEntry.data() == currentEntry.data()) continue;

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

            if (vanillaMembers.equals(currentMembers)) continue;

            Set<Identifier> added = new HashSet<>(currentMembers);
            added.removeAll(vanillaMembers);

            Set<Identifier> removed = new HashSet<>(vanillaMembers);
            removed.removeAll(currentMembers);

            JsonObject tagJson = new JsonObject();

            if (removed.isEmpty()) {
                tagJson.addProperty("replace", false);
                JsonArray values = new JsonArray();
                for (Identifier id : added) values.add(id.toString());
                tagJson.add("values", values);
            } else {
                tagJson.addProperty("replace", true);
                JsonArray values = new JsonArray();
                for (Identifier id : currentMembers) values.add(id.toString());
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

    public void clearAll() {
        vanilla.clear();
        current.clear();
        version.set(0);
    }
}
