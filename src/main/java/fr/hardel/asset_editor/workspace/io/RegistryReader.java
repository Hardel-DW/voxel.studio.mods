package fr.hardel.asset_editor.workspace.io;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import fr.hardel.asset_editor.workspace.flush.CustomFields;
import fr.hardel.asset_editor.workspace.flush.ElementEntry;
import fr.hardel.asset_editor.workspace.WorkspaceDefinition;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class RegistryReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegistryReader.class);

    public <T> Map<Identifier, ElementEntry<T>> readBaseline(WorkspaceDefinition<T> binding,
        ResourceManager resourceManager,
        @org.jspecify.annotations.Nullable Registry<T> registry,
        HolderLookup.Provider registries) {
        var ops = registries.createSerializationContext(JsonOps.INSTANCE);
        Map<Identifier, T> elements = loadElements(binding, resourceManager, ops);
        Map<Identifier, Set<Identifier>> tagsByElement = registry != null
            ? loadTags(binding, resourceManager, TagLoader.ElementLookup.fromFrozenRegistry(registry))
            : Map.of();
        Map<Identifier, ElementEntry<T>> result = new LinkedHashMap<>();

        for (var entry : elements.entrySet()) {
            Identifier id = entry.getKey();
            Set<Identifier> tags = tagsByElement.getOrDefault(id, Set.of());
            ElementEntry<T> raw = new ElementEntry<>(id, entry.getValue(), Set.copyOf(tags), CustomFields.EMPTY);
            result.put(id, binding.initializeEntry(raw));
        }

        return Map.copyOf(result);
    }

    private <T, H extends Holder<T>> Map<Identifier, Set<Identifier>> loadTags(WorkspaceDefinition<T> binding,
        ResourceManager resourceManager,
        TagLoader.ElementLookup<H> elementLookup) {
        var loader = new TagLoader<>(elementLookup, Registries.tagsDirPath(binding.registryKey()));

        Map<Identifier, Set<Identifier>> tagsByElement = new HashMap<>();
        loader.build(loader.load(resourceManager)).forEach((tagId, holders) -> {
            for (H holder : holders) {
                holder.unwrapKey().ifPresent(key ->
                    tagsByElement.computeIfAbsent(key.identifier(), ignored -> new java.util.HashSet<>()).add(tagId));
            }
        });
        return tagsByElement;
    }

    private <T> Map<Identifier, T> loadElements(WorkspaceDefinition<T> binding,
        ResourceManager resourceManager,
        com.mojang.serialization.DynamicOps<JsonElement> ops) {
        String registryDir = binding.registryKey().identifier().getPath();
        Map<Identifier, T> result = new LinkedHashMap<>();

        resourceManager.listResources(registryDir, id -> id.getPath().endsWith(".json"))
            .forEach((resourceId, resource) -> {
                String fullPath = resourceId.getPath();
                String elementPath = fullPath.substring(registryDir.length() + 1, fullPath.length() - 5);
                Identifier elementId = Identifier.fromNamespaceAndPath(resourceId.getNamespace(), elementPath);

                try (var reader = resource.openAsReader()) {
                    JsonElement json = JsonParser.parseReader(reader);
                    binding.codec().parse(ops, json)
                        .ifSuccess(value -> result.put(elementId, value))
                        .ifError(error -> LOGGER.warn("Failed to decode baseline {}: {}", elementId, error.message()));
                } catch (IOException exception) {
                    LOGGER.warn("Failed to read baseline resource {}: {}", elementId, exception.getMessage());
                }
            });

        return result;
    }
}
