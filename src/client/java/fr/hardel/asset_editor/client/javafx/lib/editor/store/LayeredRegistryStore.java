package fr.hardel.asset_editor.client.javafx.lib.editor.store;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class LayeredRegistryStore {

    public record OverlayEntry<T>(String registry, Identifier id, T value, Codec<T> codec) {}

    private final Map<Path, Map<String, Map<Identifier, OverlayEntry<?>>>> overlaysByPack = new HashMap<>();
    private final Map<Path, Set<String>> dirtyKeysByPack = new HashMap<>();
    private final IntegerProperty version = new SimpleIntegerProperty(0);

    public IntegerProperty versionProperty() {
        return version;
    }

    public <T> Optional<T> getOverlay(Path packRoot, String registry, Identifier id, Class<T> type) {
        var packMap = overlaysByPack.get(normalize(packRoot));
        if (packMap == null) return Optional.empty();
        var registryMap = packMap.get(registry);
        if (registryMap == null) return Optional.empty();
        var entry = registryMap.get(id);
        if (entry == null) return Optional.empty();
        return Optional.of(type.cast(entry.value()));
    }

    public <T> void putOverlay(Path packRoot, String registry, Identifier id, T value, Codec<T> codec) {
        overlaysByPack.computeIfAbsent(normalize(packRoot), k -> new HashMap<>())
                .computeIfAbsent(registry, k -> new HashMap<>())
                .put(id, new OverlayEntry<>(registry, id, value, codec));
    }

    public boolean hasOverlay(Path packRoot, String registry, Identifier id) {
        var packMap = overlaysByPack.get(normalize(packRoot));
        if (packMap == null) return false;
        var registryMap = packMap.get(registry);
        return registryMap != null && registryMap.containsKey(id);
    }

    public void markDirty(Path packRoot, String registry, Identifier id) {
        dirtyKeysByPack.computeIfAbsent(normalize(packRoot), k -> new HashSet<>()).add(registry + "/" + id);
    }

    public void incrementVersion() {
        version.set(version.get() + 1);
    }

    @SuppressWarnings("unchecked")
    public void flush(Path packRoot, HolderLookup.Provider registries) {
        Path normalizedPackRoot = normalize(packRoot);
        var dirtyKeys = dirtyKeysByPack.get(normalizedPackRoot);
        if (dirtyKeys == null || dirtyKeys.isEmpty()) return;

        var packOverlays = overlaysByPack.get(normalizedPackRoot);
        if (packOverlays == null || packOverlays.isEmpty()) return;

        var ops = registries.createSerializationContext(JsonOps.INSTANCE);
        var gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        var writtenKeys = new HashSet<String>();

        for (var registryMap : packOverlays.values()) {
            for (var entry : registryMap.values()) {
                String key = entry.registry() + "/" + entry.id();
                if (!dirtyKeys.contains(key)) continue;

                var rawCodec = (Codec<Object>) (Codec<?>) entry.codec();
                JsonElement json = rawCodec.encodeStart(ops, entry.value())
                        .getOrThrow(msg -> new IllegalStateException("Encode failed: " + msg));

                Path filePath = packRoot.resolve("data")
                        .resolve(entry.id().getNamespace())
                        .resolve(entry.registry())
                        .resolve(entry.id().getPath() + ".json");

                try {
                    Files.createDirectories(filePath.getParent());
                    Files.writeString(filePath, gson.toJson(json));
                    writtenKeys.add(key);
                } catch (IOException e) {
                    System.err.println("Failed to write overlay: " + filePath + " - " + e.getMessage());
                }
            }
        }

        dirtyKeys.removeAll(writtenKeys);
        if (dirtyKeys.isEmpty()) {
            dirtyKeysByPack.remove(normalizedPackRoot);
        }
    }

    public void clearAll() {
        overlaysByPack.clear();
        dirtyKeysByPack.clear();
        version.set(0);
    }

    private static Path normalize(Path packRoot) {
        return packRoot.toAbsolutePath().normalize();
    }
}
