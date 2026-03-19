package fr.hardel.asset_editor.store;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import fr.hardel.asset_editor.network.workspace.RegistryWorkspaceBinding;
import fr.hardel.asset_editor.tag.ExtendedTagFile;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagEntry;
import net.minecraft.tags.TagLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

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

    private final Map<String, Map<Identifier, ElementEntry<?>>> baselines = new HashMap<>();
    private final Map<String, Map<String, PackWorkspaceStore<?>>> workspaces = new HashMap<>();

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

        baselines.put(name, Map.copyOf(entries));
        workspaces.values().forEach(perRegistry -> perRegistry.remove(name));
    }

    public <T> ElementEntry<T> get(String packId, RegistryWorkspaceBinding<T> binding,
        Path packRoot, HolderLookup.Provider registries, Identifier elementId) {
        return workspace(packId, binding, packRoot, registries).get(elementId);
    }

    public <T> void put(String packId, RegistryWorkspaceBinding<T> binding,
        Path packRoot, HolderLookup.Provider registries,
        Identifier elementId, ElementEntry<T> entry) {
        workspace(packId, binding, packRoot, registries).put(elementId, entry);
    }

    public <T> List<ElementEntry<T>> snapshotWorkspace(String packId, RegistryWorkspaceBinding<T> binding,
        Path packRoot, HolderLookup.Provider registries) {
        return List.copyOf(workspace(packId, binding, packRoot, registries).entries());
    }

    public <T> void flushDirty(Path packRoot, String packId, RegistryWorkspaceBinding<T> binding,
        HolderLookup.Provider registries) {
        PackWorkspaceStore<T> workspace = workspace(packId, binding, packRoot, registries);
        Set<Identifier> dirty = workspace.dirty();
        if (dirty.isEmpty())
            return;

        var ops = registries.createSerializationContext(JsonOps.INSTANCE);
        var flushAdapter = binding.adapter() == null ? FlushAdapter.<T> identity() : binding.adapter();
        Map<Identifier, ElementEntry<T>> baselineMap = baselineEntries(binding);
        Map<Identifier, ElementEntry<T>> currentMap = workspace.entryMap();

        flushDirtyElements(packRoot, binding.registryKey().identifier().getPath(), baselineMap, currentMap, dirty,
            binding.codec(), ops, flushAdapter);
        flushDirtyTags(packRoot, binding.registryKey().identifier().getPath(), baselineMap, currentMap, dirty, flushAdapter);

        workspace.clearDirty();
    }

    public void clearAll() {
        baselines.clear();
        workspaces.clear();
    }

    @SuppressWarnings("unchecked")
    private <T> Map<Identifier, Set<Identifier>> loadTags(ResourceKey<Registry<T>> registryKey,
        ResourceManager resourceManager,
        Registry<T> registry) {
        var loader = new TagLoader<>(
            (TagLoader.ElementLookup<net.minecraft.core.Holder<T>>) TagLoader.ElementLookup.fromFrozenRegistry(registry),
            net.minecraft.core.registries.Registries.tagsDirPath(registryKey));

        Map<Identifier, Set<Identifier>> tagsByElement = new HashMap<>();
        loader.build(loader.load(resourceManager)).forEach((tagId, holders) -> {
            for (var holder : holders) {
                holder.unwrapKey().ifPresent(key -> tagsByElement.computeIfAbsent(key.identifier(), k -> new HashSet<>()).add(tagId));
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
    private <T> PackWorkspaceStore<T> workspace(String packId, RegistryWorkspaceBinding<T> binding,
        Path packRoot, HolderLookup.Provider registries) {
        Map<String, PackWorkspaceStore<?>> registryWorkspaces = workspaces.computeIfAbsent(packId, ignored -> new HashMap<>());
        return (PackWorkspaceStore<T>) registryWorkspaces.computeIfAbsent(binding.registryName(),
            ignored -> loadWorkspace(packRoot, binding, registries));
    }

    private <T> PackWorkspaceStore<T> loadWorkspace(Path packRoot, RegistryWorkspaceBinding<T> binding,
        HolderLookup.Provider registries) {
        Map<Identifier, ElementEntry<T>> entries = new LinkedHashMap<>();
        Map<Identifier, Set<Identifier>> tagsByElement = new LinkedHashMap<>();

        for (var entry : baselineEntries(binding).entrySet()) {
            entries.put(entry.getKey(), entry.getValue());
            tagsByElement.put(entry.getKey(), new LinkedHashSet<>(entry.getValue().tags()));
        }

        loadPackElementOverrides(packRoot, binding, registries, entries, tagsByElement);
        loadPackTagOverrides(packRoot, binding.registryKey().identifier().getPath(), tagsByElement, entries.keySet());

        Map<Identifier, ElementEntry<T>> initialized = new LinkedHashMap<>();
        for (var entry : entries.entrySet()) {
            Set<Identifier> tags = Set.copyOf(tagsByElement.getOrDefault(entry.getKey(), Set.of()));
            ElementEntry<T> raw = new ElementEntry<>(entry.getKey(), entry.getValue().data(), tags, CustomFields.EMPTY);
            initialized.put(entry.getKey(), binding.initializeEntry(raw));
        }

        return new PackWorkspaceStore<>(initialized);
    }

    @SuppressWarnings("unchecked")
    private <T> Map<Identifier, ElementEntry<T>> baselineEntries(RegistryWorkspaceBinding<T> binding) {
        return (Map<Identifier, ElementEntry<T>>) (Map<?, ?>) baselines.getOrDefault(binding.registryName(), Map.of());
    }

    private <T> void loadPackElementOverrides(Path packRoot, RegistryWorkspaceBinding<T> binding,
        HolderLookup.Provider registries,
        Map<Identifier, ElementEntry<T>> entries,
        Map<Identifier, Set<Identifier>> tagsByElement) {
        Path dataDir = packRoot.resolve("data");
        if (!Files.isDirectory(dataDir))
            return;

        String registryDir = binding.registryKey().identifier().getPath();
        var ops = registries.createSerializationContext(JsonOps.INSTANCE);

        try (Stream<Path> namespaces = Files.list(dataDir)) {
            namespaces.filter(Files::isDirectory).forEach(namespaceDir -> {
                String namespace = namespaceDir.getFileName().toString();
                Path registryRoot = namespaceDir.resolve(registryDir);
                if (!Files.isDirectory(registryRoot))
                    return;

                try (Stream<Path> files = Files.walk(registryRoot)) {
                    files.filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().endsWith(".json"))
                        .forEach(file -> {
                            Identifier elementId = elementId(namespace, registryRoot, file);
                            if (elementId == null)
                                return;

                            try (Reader reader = Files.newBufferedReader(file)) {
                                JsonElement json = JsonParser.parseReader(reader);
                                binding.codec().parse(ops, json).ifSuccess(value -> {
                                    Set<Identifier> tags = tagsByElement.getOrDefault(elementId, new LinkedHashSet<>());
                                    entries.put(elementId, new ElementEntry<>(elementId, value, Set.copyOf(tags), CustomFields.EMPTY));
                                    tagsByElement.computeIfAbsent(elementId, ignored -> new LinkedHashSet<>(tags));
                                }).ifError(error -> LOGGER.warn("Failed to decode pack entry {}: {}", elementId, error.message()));
                            } catch (IOException exception) {
                                LOGGER.warn("Failed to read pack entry {}: {}", file, exception.getMessage());
                            }
                        });
                } catch (IOException exception) {
                    LOGGER.warn("Failed to scan pack registry directory {}: {}", registryRoot, exception.getMessage());
                }
            });
        } catch (IOException exception) {
            LOGGER.warn("Failed to list pack namespaces in {}: {}", dataDir, exception.getMessage());
        }
    }

    private void loadPackTagOverrides(Path packRoot, String registryDir,
        Map<Identifier, Set<Identifier>> tagsByElement,
        Set<Identifier> knownElements) {
        Path dataDir = packRoot.resolve("data");
        if (!Files.isDirectory(dataDir))
            return;

        try (Stream<Path> namespaces = Files.list(dataDir)) {
            namespaces.filter(Files::isDirectory).forEach(namespaceDir -> {
                String namespace = namespaceDir.getFileName().toString();
                Path tagsRoot = namespaceDir.resolve("tags").resolve(registryDir);
                if (!Files.isDirectory(tagsRoot))
                    return;

                try (Stream<Path> files = Files.walk(tagsRoot)) {
                    files.filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().endsWith(".json"))
                        .forEach(file -> applyTagFile(namespace, tagsRoot, file, tagsByElement, knownElements));
                } catch (IOException exception) {
                    LOGGER.warn("Failed to scan tag directory {}: {}", tagsRoot, exception.getMessage());
                }
            });
        } catch (IOException exception) {
            LOGGER.warn("Failed to list pack namespaces in {}: {}", dataDir, exception.getMessage());
        }
    }

    private void applyTagFile(String namespace, Path tagsRoot, Path file,
        Map<Identifier, Set<Identifier>> tagsByElement,
        Set<Identifier> knownElements) {
        Identifier tagId = elementId(namespace, tagsRoot, file);
        if (tagId == null)
            return;

        try (Reader reader = Files.newBufferedReader(file)) {
            JsonElement parsed = JsonParser.parseReader(reader);
            if (!parsed.isJsonObject())
                return;

            JsonObject root = parsed.getAsJsonObject();
            if (root.has("replace") && root.get("replace").getAsBoolean()) {
                for (Set<Identifier> elementTags : tagsByElement.values())
                    elementTags.remove(tagId);
            }

            applyTagArray(root.get("values"), tagId, tagsByElement, knownElements, true);
            applyTagArray(root.get("exclude"), tagId, tagsByElement, knownElements, false);
        } catch (Exception exception) {
            LOGGER.warn("Failed to read pack tag {}: {}", file, exception.getMessage());
        }
    }

    private void applyTagArray(JsonElement arrayElement, Identifier tagId,
        Map<Identifier, Set<Identifier>> tagsByElement,
        Set<Identifier> knownElements,
        boolean add) {
        if (arrayElement == null || !arrayElement.isJsonArray())
            return;

        arrayElement.getAsJsonArray().forEach(element -> {
            String raw = null;
            if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
                raw = element.getAsString();
            } else if (element.isJsonObject() && element.getAsJsonObject().has("id")) {
                raw = element.getAsJsonObject().get("id").getAsString();
            }

            if (raw == null || raw.startsWith("#"))
                return;

            Identifier target = Identifier.tryParse(raw);
            if (target == null || !knownElements.contains(target))
                return;

            Set<Identifier> tags = tagsByElement.computeIfAbsent(target, ignored -> new LinkedHashSet<>());
            if (add)
                tags.add(tagId);
            else
                tags.remove(tagId);
        });
    }

    private static Identifier elementId(String namespace, Path root, Path file) {
        Path relative = root.relativize(file);
        String path = relative.toString().replace('\\', '/');
        if (!path.endsWith(".json"))
            return null;
        path = path.substring(0, path.length() - 5);
        return Identifier.fromNamespaceAndPath(namespace, path);
    }

    private <T> void flushDirtyElements(Path packRoot, String registry,
        Map<Identifier, ElementEntry<T>> vanillaMap,
        Map<Identifier, ElementEntry<T>> currentMap,
        Set<Identifier> dirty,
        Codec<T> codec, DynamicOps<JsonElement> ops,
        FlushAdapter<T> adapter) {
        for (Identifier id : dirty) {
            var currentRaw = currentMap.get(id);
            if (currentRaw == null)
                continue;

            var currentEntry = adapter.prepare(currentRaw);
            var vanillaEntry = vanillaMap.containsKey(id)
                ? adapter.prepare(vanillaMap.get(id))
                : null;

            Path filePath = packRoot.resolve("data").resolve(id.getNamespace())
                .resolve(registry).resolve(id.getPath() + ".json");

            if (vanillaEntry != null && Objects.equals(vanillaEntry.data(), currentEntry.data())) {
                try {
                    Files.deleteIfExists(filePath);
                } catch (IOException e) {
                    LOGGER.warn("Failed to delete reverted element: {}", filePath, e);
                }
                continue;
            }

            codec.encodeStart(ops, currentEntry.data())
                .ifSuccess(json -> writeFile(filePath, GSON.toJson(json)))
                .ifError(error -> LOGGER.warn("Failed to encode element {}: {}", id, error.message()));
        }
    }

    private <T> void flushDirtyTags(Path packRoot, String registry,
        Map<Identifier, ElementEntry<T>> vanillaMap,
        Map<Identifier, ElementEntry<T>> currentMap,
        Set<Identifier> dirty,
        FlushAdapter<T> adapter) {
        Set<Identifier> affectedTagIds = collectAffectedTags(vanillaMap, currentMap, dirty, adapter);
        if (affectedTagIds.isEmpty())
            return;

        Map<Identifier, Set<Identifier>> vanTags = collectTagMemberships(vanillaMap, adapter);
        Map<Identifier, Set<Identifier>> curTags = collectTagMemberships(currentMap, adapter);

        for (Identifier tagId : affectedTagIds) {
            Set<Identifier> vanillaMembers = vanTags.getOrDefault(tagId, Set.of());
            Set<Identifier> currentMembers = curTags.getOrDefault(tagId, Set.of());

            Path filePath = packRoot.resolve("data")
                .resolve(tagId.getNamespace()).resolve("tags")
                .resolve(registry).resolve(tagId.getPath() + ".json");

            if (vanillaMembers.equals(currentMembers)) {
                try {
                    Files.deleteIfExists(filePath);
                } catch (IOException e) {
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

    private <T> Set<Identifier> collectAffectedTags(Map<Identifier, ElementEntry<T>> vanillaMap,
        Map<Identifier, ElementEntry<T>> currentMap,
        Set<Identifier> dirty,
        FlushAdapter<T> adapter) {
        Set<Identifier> tags = new HashSet<>();
        for (Identifier id : dirty) {
            var van = vanillaMap.get(id);
            var cur = currentMap.get(id);
            if (van != null)
                tags.addAll(adapter.prepare(van).tags());
            if (cur != null)
                tags.addAll(adapter.prepare(cur).tags());
        }
        return tags;
    }

    private <T> Map<Identifier, Set<Identifier>> collectTagMemberships(Map<Identifier, ElementEntry<T>> elements,
        FlushAdapter<T> adapter) {
        Map<Identifier, Set<Identifier>> result = new HashMap<>();
        for (ElementEntry<T> entry : elements.values()) {
            var prepared = adapter.prepare(entry);
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
        return key.identifier().toString();
    }
}
