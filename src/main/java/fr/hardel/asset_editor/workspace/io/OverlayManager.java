package fr.hardel.asset_editor.workspace.io;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import fr.hardel.asset_editor.workspace.CustomFields;
import fr.hardel.asset_editor.workspace.ElementEntry;
import fr.hardel.asset_editor.workspace.RegistryWorkspace;
import fr.hardel.asset_editor.workspace.definition.WorkspaceDefinition;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class OverlayManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(OverlayManager.class);

    private final MinecraftServer server;

    public OverlayManager(MinecraftServer server) {
        this.server = server;
    }

    public <T> RegistryWorkspace<T> loadWorkspace(String packId, Path packRoot, WorkspaceDefinition<T> binding, HolderLookup.Provider registries, Map<Identifier, ElementEntry<T>> baseEntries) {
        LayerState<T> referenceState = baseLayer(baseEntries);
        for (Path layerRoot : lowerLayerRoots(packId))
            applyLayer(layerRoot, binding, registries, referenceState);

        LayerState<T> currentState = referenceState.copy();
        applyLayer(packRoot, binding, registries, currentState);

        return new RegistryWorkspace<>(
            initializeEntries(binding, referenceState),
            initializeEntries(binding, currentState));
    }

    private List<Path> lowerLayerRoots(String targetPackId) {
        List<Path> layers = new ArrayList<>();
        for (Pack pack : server.getPackRepository().getSelectedPacks()) {
            if (pack.getPackSource() != PackSource.WORLD)
                continue;

            if (pack.getId().equals(targetPackId))
                break;

            resolvePackRoot(pack.getId()).ifPresent(layers::add);
        }
        return layers;
    }

    private Optional<Path> resolvePackRoot(String packId) {
        String relativeName = packId.startsWith("file/") ? packId.substring(5) : packId;
        Path datapackDir = server.getWorldPath(LevelResource.DATAPACK_DIR).toAbsolutePath().normalize();
        Path resolved = datapackDir.resolve(relativeName).normalize();

        if (!resolved.startsWith(datapackDir) || !Files.isDirectory(resolved))
            return Optional.empty();

        return Optional.of(resolved);
    }

    private <T> void applyLayer(Path packRoot, WorkspaceDefinition<T> binding, HolderLookup.Provider registries, LayerState<T> state) {
        applyElementOverrides(packRoot, binding, registries, state);
        applyTagOverrides(packRoot, binding.registryKey().identifier().getPath(), state);
    }

    private <T> LayerState<T> baseLayer(Map<Identifier, ElementEntry<T>> baseEntries) {
        return new LayerState<>(new LinkedHashMap<>(baseEntries), copyTags(baseEntries));
    }

    private <T> Map<Identifier, ElementEntry<T>> initializeEntries(WorkspaceDefinition<T> binding, LayerState<T> state) {
        Map<Identifier, ElementEntry<T>> initialized = new LinkedHashMap<>();

        for (var entry : state.entries.entrySet()) {
            Set<Identifier> tags = Set.copyOf(state.tagsByElement.getOrDefault(entry.getKey(), Set.of()));
            ElementEntry<T> raw = new ElementEntry<>(entry.getKey(), entry.getValue().data(), tags, CustomFields.EMPTY);
            initialized.put(entry.getKey(), binding.initializeEntry(raw));
        }

        return initialized;
    }

    private <T> Map<Identifier, Set<Identifier>> copyTags(Map<Identifier, ElementEntry<T>> entries) {
        Map<Identifier, Set<Identifier>> tags = new LinkedHashMap<>();

        for (var entry : entries.entrySet()) {
            tags.put(entry.getKey(), new LinkedHashSet<>(entry.getValue().tags()));
        }

        return tags;
    }

    private <T> void applyElementOverrides(Path packRoot, WorkspaceDefinition<T> binding,
        HolderLookup.Provider registries,
        LayerState<T> state) {
        Path dataDir = packRoot.resolve("data");

        if (!Files.isDirectory(dataDir)) {
            return;
        }

        String registryDir = binding.registryKey().identifier().getPath();
        var ops = registries.createSerializationContext(JsonOps.INSTANCE);

        for (Path namespaceDir : namespaceDirectories(dataDir)) {
            applyElementOverridesInNamespace(namespaceDir, registryDir, binding, ops, state);
        }
    }

    private <T> void applyElementOverridesInNamespace(Path namespaceDir, String registryDir,
        WorkspaceDefinition<T> binding,
        DynamicOps<JsonElement> ops,
        LayerState<T> state) {
        Path registryRoot = namespaceDir.resolve(registryDir);

        if (!Files.isDirectory(registryRoot)) {
            return;
        }

        String namespace = namespaceDir.getFileName().toString();
        for (Path file : jsonFiles(registryRoot, "pack registry directory")) {
            Identifier elementId = elementId(namespace, registryRoot, file);
            if (elementId == null)
                continue;

            applyElementOverrideFile(file, elementId, binding, ops, state);
        }
    }

    private <T> void applyElementOverrideFile(Path file, Identifier elementId,
        WorkspaceDefinition<T> binding,
        DynamicOps<JsonElement> ops,
        LayerState<T> state) {
        JsonElement json = readJson(file, "pack entry");
        if (json == null)
            return;

        binding.codec().parse(ops, json)
            .ifSuccess(value -> mergeElementEntry(state, elementId, value))
            .ifError(error -> LOGGER.warn("Failed to decode pack entry {}: {}", elementId, error.message()));
    }

    private <T> void mergeElementEntry(LayerState<T> state, Identifier elementId, T value) {
        Set<Identifier> tags = state.tagsByElement.getOrDefault(elementId, new LinkedHashSet<>());
        state.entries.put(elementId, new ElementEntry<>(elementId, value, Set.copyOf(tags), CustomFields.EMPTY));
        state.tagsByElement.computeIfAbsent(elementId, ignored -> new LinkedHashSet<>(tags));
    }

    private void applyTagOverrides(Path packRoot, String registryDir, LayerState<?> state) {
        Path dataDir = packRoot.resolve("data");
        if (!Files.isDirectory(dataDir))
            return;

        for (Path namespaceDir : namespaceDirectories(dataDir))
            applyTagOverridesInNamespace(namespaceDir, registryDir, state);
    }

    private void applyTagOverridesInNamespace(Path namespaceDir, String registryDir, LayerState<?> state) {
        Path tagsRoot = namespaceDir.resolve("tags").resolve(registryDir);
        if (!Files.isDirectory(tagsRoot))
            return;

        String namespace = namespaceDir.getFileName().toString();
        for (Path file : jsonFiles(tagsRoot, "tag directory"))
            applyTagFile(namespace, tagsRoot, file, state);
    }

    private void applyTagFile(String namespace, Path tagsRoot, Path file, LayerState<?> state) {
        Identifier tagId = elementId(namespace, tagsRoot, file);
        if (tagId == null)
            return;

        JsonElement parsed = readJson(file, "pack tag");
        if (parsed == null || !parsed.isJsonObject())
            return;

        JsonObject root = parsed.getAsJsonObject();
        if (replaceTag(root))
            removeTag(tagId, state.tagsByElement);

        applyTagArray(root.get("values"), tagId, state, true);
        applyTagArray(root.get("exclude"), tagId, state, false);
    }

    private boolean replaceTag(JsonObject root) {
        return root.has("replace") && root.get("replace").getAsBoolean();
    }

    private void removeTag(Identifier tagId, Map<Identifier, Set<Identifier>> tagsByElement) {
        for (Set<Identifier> elementTags : tagsByElement.values())
            elementTags.remove(tagId);
    }

    private void applyTagArray(JsonElement arrayElement, Identifier tagId, LayerState<?> state, boolean add) {
        if (arrayElement == null || !arrayElement.isJsonArray())
            return;

        for (JsonElement element : arrayElement.getAsJsonArray()) {
            Identifier target = targetId(element);
            if (target == null || !state.entries.containsKey(target))
                continue;
            updateMembership(state.tagsByElement, target, tagId, add);
        }
    }

    private Identifier targetId(JsonElement element) {
        String raw = rawTargetId(element);
        if (raw == null || raw.startsWith("#"))
            return null;
        return Identifier.tryParse(raw);
    }

    private String rawTargetId(JsonElement element) {
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString())
            return element.getAsString();
        if (element.isJsonObject() && element.getAsJsonObject().has("id"))
            return element.getAsJsonObject().get("id").getAsString();
        return null;
    }

    private void updateMembership(Map<Identifier, Set<Identifier>> tagsByElement, Identifier target, Identifier tagId, boolean add) {
        Set<Identifier> tags = tagsByElement.computeIfAbsent(target, ignored -> new LinkedHashSet<>());

        if (add)
            tags.add(tagId);
        else
            tags.remove(tagId);
    }

    private List<Path> namespaceDirectories(Path dataDir) {
        return childDirectories(dataDir);
    }

    private List<Path> childDirectories(Path root) {
        try (var children = Files.list(root)) {
            return children.filter(Files::isDirectory).toList();
        } catch (IOException exception) {
            LOGGER.warn("Failed to list {} in {}: {}", "pack namespaces", root, exception.getMessage());
            return List.of();
        }
    }

    private List<Path> jsonFiles(Path root, String label) {
        try (var files = Files.walk(root)) {
            return files.filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().endsWith(".json"))
                .toList();
        } catch (IOException exception) {
            LOGGER.warn("Failed to scan {} {}: {}", label, root, exception.getMessage());
            return List.of();
        }
    }

    private JsonElement readJson(Path file, String label) {
        try (Reader reader = Files.newBufferedReader(file)) {
            return JsonParser.parseReader(reader);
        } catch (IOException exception) {
            LOGGER.warn("Failed to read {} {}: {}", label, file, exception.getMessage());
            return null;
        }
    }

    private static Identifier elementId(String namespace, Path root, Path file) {
        Path relative = root.relativize(file);
        String path = relative.toString().replace('\\', '/');
        if (!path.endsWith(".json"))
            return null;

        return Identifier.fromNamespaceAndPath(namespace, path.substring(0, path.length() - 5));
    }

    private record LayerState<T>(Map<Identifier, ElementEntry<T>> entries, Map<Identifier, Set<Identifier>> tagsByElement) {
        private LayerState<T> copy() {
                return new LayerState<>(new LinkedHashMap<>(entries), copyTagMemberships());
            }

            private Map<Identifier, Set<Identifier>> copyTagMemberships() {
                Map<Identifier, Set<Identifier>> copy = new LinkedHashMap<>();
                for (var entry : tagsByElement.entrySet())
                    copy.put(entry.getKey(), new LinkedHashSet<>(entry.getValue()));
                return copy;
            }
        }
}
