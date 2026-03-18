package fr.hardel.asset_editor.store;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import fr.hardel.asset_editor.tag.ExtendedTagFile;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagEntry;
import net.minecraft.tags.TagLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;

public final class ServerElementStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerElementStore.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private static ServerElementStore instance;

    public static ServerElementStore get() {
        return instance;
    }

    public static void init() {
        instance = new ServerElementStore();
    }

    public static void shutdown() {
        instance = null;
    }

    private final Map<String, Map<Identifier, ElementEntry<?>>> vanilla = new HashMap<>();
    private final Map<String, Map<Identifier, ElementEntry<?>>> current = new HashMap<>();
    private final Map<String, Map<String, Set<Identifier>>> dirtyElements = new HashMap<>();

    public <T> void snapshot(ResourceKey<Registry<T>> registryKey, Registry<T> registry,
                             Function<ElementEntry<T>, CustomFields> customInitializer) {
        String name = registryName(registryKey);

        Map<Identifier, Set<Identifier>> tagsByElement = new HashMap<>();
        registry.listTags().forEach(named -> {
            Identifier tagId = named.key().location();
            for (Holder<T> holder : named.stream().toList()) {
                holder.unwrapKey().ifPresent(
                        key -> tagsByElement.computeIfAbsent(key.identifier(), k -> new HashSet<>()).add(tagId));
            }
        });

        Map<Identifier, ElementEntry<?>> entries = new LinkedHashMap<>();
        registry.listElements().forEach(holder -> {
            Identifier id = holder.key().identifier();
            Set<Identifier> tags = tagsByElement.getOrDefault(id, Set.of());
            ElementEntry<T> entry = new ElementEntry<>(id, holder.value(), Set.copyOf(tags), CustomFields.EMPTY);
            entries.put(id, entry.withCustom(customInitializer.apply(entry)));
        });

        current.put(name, new LinkedHashMap<>(entries));
        dirtyElements.remove(name);
    }

    public <T> void snapshotVanilla(ResourceKey<Registry<T>> registryKey,
                                     ResourceManager vanillaResources,
                                     Registry<T> registry,
                                     Codec<T> codec,
                                     HolderLookup.Provider registries,
                                     Function<ElementEntry<T>, CustomFields> customInitializer) {
        String name = registryName(registryKey);
        var ops = registries.createSerializationContext(JsonOps.INSTANCE);

        Map<Identifier, T> vanillaData = loadElementsFromResources(registryKey, vanillaResources, codec, ops);
        Map<Identifier, Set<Identifier>> vanillaTags = loadTags(registryKey, vanillaResources, registry);

        Map<Identifier, ElementEntry<?>> entries = new LinkedHashMap<>();
        for (var entry : vanillaData.entrySet()) {
            Identifier id = entry.getKey();
            Set<Identifier> tags = vanillaTags.getOrDefault(id, Set.of());
            ElementEntry<T> vanillaEntry = new ElementEntry<>(id, entry.getValue(), Set.copyOf(tags), CustomFields.EMPTY);
            entries.put(id, vanillaEntry.withCustom(customInitializer.apply(vanillaEntry)));
        }

        vanilla.put(name, Map.copyOf(entries));
    }

    @SuppressWarnings("unchecked")
    public <T> ElementEntry<T> get(Identifier registryId, Identifier elementId) {
        var registryMap = current.get(registryId.getPath());
        if (registryMap == null) return null;
        return (ElementEntry<T>) registryMap.get(elementId);
    }

    public <T> void put(Identifier registryId, Identifier elementId, ElementEntry<T> entry, String packId) {
        String name = registryId.getPath();
        var registryMap = current.computeIfAbsent(name, k -> new LinkedHashMap<>());
        registryMap.put(elementId, entry);
        dirtyElements.computeIfAbsent(name, k -> new HashMap<>())
                .computeIfAbsent(packId, k -> new HashSet<>())
                .add(elementId);
    }

    public <T> void flushDirty(Path packRoot, String packId, ResourceKey<Registry<T>> registry, Codec<T> codec,
                                HolderLookup.Provider registries, FlushAdapter<T> adapter) {
        String name = registryName(registry);
        var packMap = dirtyElements.get(name);
        if (packMap == null) return;

        Set<Identifier> dirty = packMap.get(packId);
        if (dirty == null || dirty.isEmpty()) return;

        var vanillaMap = vanilla.getOrDefault(name, Map.of());
        var currentMap = current.get(name);
        if (currentMap == null) return;

        var ops = registries.createSerializationContext(JsonOps.INSTANCE);
        var flushAdapter = adapter == null ? FlushAdapter.<T>identity() : adapter;

        flushDirtyElements(packRoot, name, vanillaMap, currentMap, dirty, codec, ops, flushAdapter);
        flushDirtyTags(packRoot, name, vanillaMap, currentMap, dirty, flushAdapter);

        dirty.clear();
    }

    public void clearAll() {
        vanilla.clear();
        current.clear();
        dirtyElements.clear();
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

    private <T> Map<Identifier, T> loadElementsFromResources(ResourceKey<Registry<T>> registryKey,
                                                               ResourceManager resourceManager,
                                                               Codec<T> codec,
                                                               DynamicOps<JsonElement> ops) {
        String dir = registryKey.identifier().getPath();
        Map<Identifier, T> result = new LinkedHashMap<>();

        resourceManager.listResources(dir, id -> id.getPath().endsWith(".json"))
                .forEach((resourceId, resource) -> {
                    String fullPath = resourceId.getPath();
                    String elementPath = fullPath.substring(dir.length() + 1, fullPath.length() - 5);
                    Identifier elementId = Identifier.fromNamespaceAndPath(resourceId.getNamespace(), elementPath);

                    try (var reader = resource.openAsReader()) {
                        JsonElement json = JsonParser.parseReader(reader);
                        codec.parse(ops, json)
                                .ifSuccess(value -> result.put(elementId, value))
                                .ifError(error -> LOGGER.warn("Failed to decode vanilla {}: {}", elementId, error.message()));
                    } catch (IOException e) {
                        LOGGER.warn("Failed to read vanilla resource {}: {}", elementId, e);
                    }
                });

        return result;
    }

    @SuppressWarnings("unchecked")
    private <T> void flushDirtyElements(Path packRoot, String registry,
                                         Map<Identifier, ElementEntry<?>> vanillaMap,
                                         Map<Identifier, ElementEntry<?>> currentMap,
                                         Set<Identifier> dirty,
                                         Codec<T> codec, DynamicOps<JsonElement> ops,
                                         FlushAdapter<T> adapter) {
        for (Identifier id : dirty) {
            var currentRaw = currentMap.get(id);
            if (currentRaw == null) continue;

            var currentEntry = adapter.prepare((ElementEntry<T>) currentRaw);
            var vanillaEntry = vanillaMap.containsKey(id)
                    ? adapter.prepare((ElementEntry<T>) vanillaMap.get(id))
                    : null;

            Path filePath = packRoot.resolve("data").resolve(id.getNamespace())
                    .resolve(registry).resolve(id.getPath() + ".json");

            if (vanillaEntry != null && Objects.equals(vanillaEntry.data(), currentEntry.data())) {
                try { Files.deleteIfExists(filePath); } catch (IOException e) {
                    LOGGER.warn("Failed to delete reverted element: {}", filePath, e);
                }
                continue;
            }

            codec.encodeStart(ops, currentEntry.data())
                    .ifSuccess(json -> writeFile(filePath, GSON.toJson(json)))
                    .ifError(error -> LOGGER.warn("Failed to encode element {}: {}", id, error.message()));
        }
    }

    private void flushDirtyTags(Path packRoot, String registry,
                                 Map<Identifier, ElementEntry<?>> vanillaMap,
                                 Map<Identifier, ElementEntry<?>> currentMap,
                                 Set<Identifier> dirty,
                                 FlushAdapter<?> adapter) {
        Set<Identifier> affectedTagIds = collectAffectedTags(vanillaMap, currentMap, dirty, adapter);
        if (affectedTagIds.isEmpty()) return;

        Map<Identifier, Set<Identifier>> vanTags = collectTagMemberships(vanillaMap, adapter);
        Map<Identifier, Set<Identifier>> curTags = collectTagMemberships(currentMap, adapter);

        for (Identifier tagId : affectedTagIds) {
            Set<Identifier> vanillaMembers = vanTags.getOrDefault(tagId, Set.of());
            Set<Identifier> currentMembers = curTags.getOrDefault(tagId, Set.of());

            Path filePath = packRoot.resolve("data")
                    .resolve(tagId.getNamespace()).resolve("tags")
                    .resolve(registry).resolve(tagId.getPath() + ".json");

            if (vanillaMembers.equals(currentMembers)) {
                try { Files.deleteIfExists(filePath); } catch (IOException e) {
                    LOGGER.warn("Failed to delete reverted tag: {}", filePath, e);
                }
                continue;
            }

            Set<Identifier> added = new HashSet<>(currentMembers);
            added.removeAll(vanillaMembers);
            Set<Identifier> removed = new HashSet<>(vanillaMembers);
            removed.removeAll(currentMembers);

            ExtendedTagFile.CODEC.encodeStart(JsonOps.INSTANCE, new ExtendedTagFile(
                            added.stream().map(TagEntry::element).toList(),
                            removed.stream().map(TagEntry::element).toList(), false))
                    .ifSuccess(json -> writeFile(filePath, GSON.toJson(json)))
                    .ifError(error -> LOGGER.warn("Failed to encode tag {}: {}", tagId, error.message()));
        }
    }

    @SuppressWarnings("unchecked")
    private Set<Identifier> collectAffectedTags(Map<Identifier, ElementEntry<?>> vanillaMap,
                                                 Map<Identifier, ElementEntry<?>> currentMap,
                                                 Set<Identifier> dirty,
                                                 FlushAdapter<?> adapter) {
        Set<Identifier> tags = new HashSet<>();
        for (Identifier id : dirty) {
            var van = vanillaMap.get(id);
            var cur = currentMap.get(id);
            if (van != null)
                tags.addAll(((FlushAdapter<Object>) adapter).prepare((ElementEntry<Object>) van).tags());
            if (cur != null)
                tags.addAll(((FlushAdapter<Object>) adapter).prepare((ElementEntry<Object>) cur).tags());
        }
        return tags;
    }

    @SuppressWarnings("unchecked")
    private Map<Identifier, Set<Identifier>> collectTagMemberships(Map<Identifier, ElementEntry<?>> elements,
                                                                    FlushAdapter<?> adapter) {
        Map<Identifier, Set<Identifier>> result = new HashMap<>();
        for (var entry : elements.values()) {
            var prepared = ((FlushAdapter<Object>) adapter).prepare((ElementEntry<Object>) entry);
            for (Identifier tagId : prepared.tags())
                result.computeIfAbsent(tagId, k -> new HashSet<>()).add(prepared.id());
        }
        return result;
    }

    private static void writeFile(Path path, String content) {
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, content);
        } catch (IOException e) {
            LOGGER.warn("Failed to write: {}", path, e);
        }
    }

    private static String registryName(ResourceKey<?> key) {
        return key.identifier().getPath();
    }
}
