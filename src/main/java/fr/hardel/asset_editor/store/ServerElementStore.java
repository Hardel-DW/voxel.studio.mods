package fr.hardel.asset_editor.store;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
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

    private final Map<String, Map<Identifier, ElementEntry<?>>> reference = new HashMap<>();
    private final Map<String, Map<Identifier, ElementEntry<?>>> current = new HashMap<>();

    public <T> void snapshot(ResourceKey<Registry<T>> registryKey, Registry<T> registry,
                             ResourceManager referenceResources,
                             Function<ElementEntry<T>, CustomFields> customInitializer) {
        String name = registryName(registryKey);

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
    }

    @SuppressWarnings("unchecked")
    public <T> ElementEntry<T> get(Identifier registryId, Identifier elementId) {
        var registryMap = current.get(registryId.getPath());
        if (registryMap == null) return null;
        return (ElementEntry<T>) registryMap.get(elementId);
    }

    public <T> void put(Identifier registryId, Identifier elementId, ElementEntry<T> entry) {
        var registryMap = current.computeIfAbsent(registryId.getPath(), k -> new LinkedHashMap<>());
        registryMap.put(elementId, entry);
    }

    public <T> void flush(Path packRoot, ResourceKey<Registry<T>> registry, Codec<T> codec,
                           HolderLookup.Provider registries, FlushAdapter<T> adapter) {
        String name = registryName(registry);
        var referenceMap = reference.get(name);
        var currentMap = current.get(name);
        if (referenceMap == null || currentMap == null) return;

        var ops = registries.createSerializationContext(JsonOps.INSTANCE);
        var flushAdapter = adapter == null ? FlushAdapter.<T>identity() : adapter;
        flushElements(packRoot, name, referenceMap, currentMap, codec, ops, flushAdapter);
        flushTags(packRoot, name, referenceMap, currentMap, flushAdapter);
    }

    public void clearAll() {
        reference.clear();
        current.clear();
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

    @SuppressWarnings("unchecked")
    private <T> void flushElements(Path packRoot, String registry,
                                    Map<Identifier, ElementEntry<?>> referenceMap,
                                    Map<Identifier, ElementEntry<?>> currentMap,
                                    Codec<T> codec, DynamicOps<JsonElement> ops,
                                    FlushAdapter<T> adapter) {
        for (var entry : currentMap.entrySet()) {
            Identifier id = entry.getKey();
            var currentEntry = adapter.prepare((ElementEntry<T>) entry.getValue());
            var referenceEntry = referenceMap.containsKey(id)
                    ? adapter.prepare((ElementEntry<T>) referenceMap.get(id))
                    : null;

            if (referenceEntry != null && Objects.equals(referenceEntry.data(), currentEntry.data()))
                continue;

            codec.encodeStart(ops, currentEntry.data())
                    .ifSuccess(json -> writeFile(
                            packRoot.resolve("data").resolve(id.getNamespace()).resolve(registry).resolve(id.getPath() + ".json"),
                            GSON.toJson(json)))
                    .ifError(error -> LOGGER.warn("Failed to encode element {}: {}", id, error.message()));
        }
    }

    private void flushTags(Path packRoot, String registry,
                            Map<Identifier, ElementEntry<?>> referenceMap,
                            Map<Identifier, ElementEntry<?>> currentMap,
                            FlushAdapter<?> adapter) {
        Map<Identifier, Set<Identifier>> refTags = collectTagMemberships(referenceMap, adapter);
        Map<Identifier, Set<Identifier>> curTags = collectTagMemberships(currentMap, adapter);

        Set<Identifier> allTagIds = new HashSet<>();
        allTagIds.addAll(refTags.keySet());
        allTagIds.addAll(curTags.keySet());

        for (Identifier tagId : allTagIds) {
            Set<Identifier> referenceMembers = refTags.getOrDefault(tagId, Set.of());
            Set<Identifier> currentMembers = curTags.getOrDefault(tagId, Set.of());

            Path filePath = packRoot.resolve("data")
                    .resolve(tagId.getNamespace()).resolve("tags")
                    .resolve(registry).resolve(tagId.getPath() + ".json");

            if (referenceMembers.equals(currentMembers)) {
                try { Files.deleteIfExists(filePath); } catch (IOException e) {
                    LOGGER.warn("Failed to delete stale tag: {}", filePath, e);
                }
                continue;
            }

            Set<Identifier> added = new HashSet<>(currentMembers);
            added.removeAll(referenceMembers);
            Set<Identifier> removed = new HashSet<>(referenceMembers);
            removed.removeAll(currentMembers);

            ExtendedTagFile.CODEC.encodeStart(JsonOps.INSTANCE, new ExtendedTagFile(
                            added.stream().map(TagEntry::element).toList(),
                            removed.stream().map(TagEntry::element).toList(), false))
                    .ifSuccess(json -> writeFile(filePath, GSON.toJson(json)))
                    .ifError(error -> LOGGER.warn("Failed to encode tag {}: {}", tagId, error.message()));
        }
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
