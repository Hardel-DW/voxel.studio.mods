package fr.hardel.asset_editor.network.structure;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.StrictJsonParser;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public final class StructureWorldgenCatalog {

    private static final Logger LOGGER = LoggerFactory.getLogger(StructureWorldgenCatalog.class);
    private static final FileToIdConverter LISTER = new FileToIdConverter("worldgen/structure", ".json");

    public static List<StructureWorldgenSnapshot> build(MinecraftServer server) {
        ResourceManager resources = server.getResourceManager();
        Map<Identifier, StructureWorldgenSnapshot> snapshots = new LinkedHashMap<>();
        LISTER.listMatchingResources(resources).forEach((fileId, resource) -> read(fileId, resource).ifPresent(s -> snapshots.put(s.id(), s)));
        readWorldDatapacks(server).forEach(s -> snapshots.put(s.id(), s));
        return snapshots.values().stream()
            .sorted(Comparator.comparing(s -> s.id().toString()))
            .toList();
    }

    private static Optional<StructureWorldgenSnapshot> read(Identifier fileId, Resource resource) {
        Identifier id = LISTER.fileToId(fileId);
        return parse(resource).map(json -> fromJson(id, resource.sourcePackId(), json));
    }

    private static List<StructureWorldgenSnapshot> readWorldDatapacks(MinecraftServer server) {
        Path datapacks = server.getWorldPath(LevelResource.DATAPACK_DIR);
        if (!Files.isDirectory(datapacks)) {
            return List.of();
        }

        List<StructureWorldgenSnapshot> snapshots = new ArrayList<>();
        try (Stream<Path> files = Files.find(datapacks, Integer.MAX_VALUE, (path, attrs) -> attrs.isRegularFile() && path.toString().endsWith(".json"))) {
            files.forEach(file -> structureId(datapacks, file).ifPresent(id -> parse(file)
                .map(json -> fromJson(id, "file/" + datapacks.relativize(file).getName(0), json))
                .ifPresent(snapshots::add)));
        } catch (IOException exception) {
            LOGGER.warn("Failed to scan world datapack worldgen structures: {}", exception.getMessage());
        }
        return snapshots;
    }

    private static Optional<Identifier> structureId(Path datapacks, Path file) {
        Path relative = datapacks.relativize(file);
        for (int i = 0; i < relative.getNameCount() - 4; i++) {
            if (!"data".equals(relative.getName(i).toString())) {
                continue;
            }
            String namespace = relative.getName(i + 1).toString();
            if (!"worldgen".equals(relative.getName(i + 2).toString())
                || !"structure".equals(relative.getName(i + 3).toString())) {
                continue;
            }
            Path path = relative.subpath(i + 4, relative.getNameCount());
            String value = path.toString().replace('\\', '/');
            if (!value.endsWith(".json")) {
                return Optional.empty();
            }
            return Optional.of(Identifier.fromNamespaceAndPath(namespace, value.substring(0, value.length() - 5)));
        }
        return Optional.empty();
    }

    private static Optional<JsonObject> parse(Resource resource) {
        try (Reader reader = resource.openAsReader()) {
            JsonElement element = StrictJsonParser.parse(reader);
            return element.isJsonObject() ? Optional.of(element.getAsJsonObject()) : Optional.empty();
        } catch (Exception exception) {
            LOGGER.warn("Failed to read worldgen structure from {}: {}", resource.sourcePackId(), exception.getMessage());
            return Optional.empty();
        }
    }

    private static Optional<JsonObject> parse(Path file) {
        try (Reader reader = Files.newBufferedReader(file)) {
            JsonElement element = StrictJsonParser.parse(reader);
            return element.isJsonObject() ? Optional.of(element.getAsJsonObject()) : Optional.empty();
        } catch (Exception exception) {
            LOGGER.warn("Failed to read worldgen structure from {}: {}", file, exception.getMessage());
            return Optional.empty();
        }
    }

    private static StructureWorldgenSnapshot fromJson(Identifier id, String sourcePack, JsonObject json) {
        String type = json.has("type") ? json.get("type").getAsString() : "";
        String startPool = json.has("start_pool") ? readStartPool(json.get("start_pool")) : "";
        int size = json.has("size") ? json.get("size").getAsInt() : 0;
        int maxDistance = json.has("max_distance_from_center") ? json.get("max_distance_from_center").getAsInt() : 0;
        return new StructureWorldgenSnapshot(id, sourcePack, type, startPool, size, maxDistance, "");
    }

    private static String readStartPool(JsonElement element) {
        if (element.isJsonPrimitive()) {
            return element.getAsString();
        }
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            return object.has("location") ? object.get("location").getAsString() : "";
        }
        return "";
    }

    private StructureWorldgenCatalog() {}
}
