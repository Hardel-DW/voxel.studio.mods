package fr.hardel.asset_editor.network.structure;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.block.Blocks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

public final class StructureTemplateCatalog {

    private static final Logger LOGGER = LoggerFactory.getLogger(StructureTemplateCatalog.class);
    private static final FileToIdConverter LISTER = new FileToIdConverter("structure", ".nbt");
    private static final Identifier AIR = BuiltInRegistries.BLOCK.getKey(Blocks.AIR);
    private static final Identifier STRUCTURE_VOID = BuiltInRegistries.BLOCK.getKey(Blocks.STRUCTURE_VOID);
    private static final Identifier JIGSAW = BuiltInRegistries.BLOCK.getKey(Blocks.JIGSAW);

    public static List<StructureTemplateSnapshot> build(MinecraftServer server) {
        ResourceManager resources = server.getResourceManager();
        Map<Identifier, StructureTemplateSnapshot> snapshots = new LinkedHashMap<>();
        LISTER.listMatchingResources(resources).entrySet().stream()
            .map(entry -> read(entry.getKey(), entry.getValue()))
            .flatMap(Optional::stream)
            .forEach(snapshot -> snapshots.put(snapshot.id(), snapshot));
        readWorldDatapacks(server).forEach(snapshot -> snapshots.put(snapshot.id(), snapshot));
        return snapshots.values().stream()
            .sorted(Comparator.comparing(snapshot -> snapshot.id().toString()))
            .toList();
    }

    public static Optional<CompoundTag> readRaw(MinecraftServer server, Identifier structureId) {
        Identifier fileId = LISTER.idToFile(structureId);
        return server.getResourceManager().getResource(fileId).flatMap(StructureTemplateCatalog::readTag);
    }

    public static int replaceBlocks(CompoundTag root, Identifier fromBlock, Identifier toBlock) {
        ListTag palette = palette(root);
        int replacements = 0;
        for (int i = 0; i < palette.size(); i++) {
            CompoundTag state = palette.getCompoundOrEmpty(i);
            if (!fromBlock.toString().equals(state.getStringOr("Name", ""))) {
                continue;
            }
            state.putString("Name", toBlock.toString());
            state.remove("Properties");
            replacements++;
        }
        return replacements;
    }

    public static Path structurePath(Path packRoot, Identifier id) {
        return packRoot.resolve("data")
            .resolve(id.getNamespace())
            .resolve("structure")
            .resolve(id.getPath() + ".nbt")
            .normalize();
    }

    public static void writeRaw(Path packRoot, Identifier id, CompoundTag tag) throws IOException {
        Path file = structurePath(packRoot, id);
        Path root = packRoot.toAbsolutePath().normalize();
        Path absolute = file.toAbsolutePath().normalize();
        if (!absolute.startsWith(root)) {
            throw new IOException("Invalid structure path: " + id);
        }
        Files.createDirectories(absolute.getParent());
        NbtIo.writeCompressed(tag, absolute);
    }

    private static Optional<StructureTemplateSnapshot> read(Identifier fileId, Resource resource) {
        Identifier id = LISTER.fileToId(fileId);
        return readTag(resource).map(tag -> fromTag(id, resource.sourcePackId(), tag));
    }

    private static List<StructureTemplateSnapshot> readWorldDatapacks(MinecraftServer server) {
        Path datapacks = server.getWorldPath(LevelResource.DATAPACK_DIR);
        if (!Files.isDirectory(datapacks)) {
            return List.of();
        }

        List<StructureTemplateSnapshot> snapshots = new ArrayList<>();
        try (Stream<Path> files = Files.find(datapacks, Integer.MAX_VALUE, (path, attrs) -> attrs.isRegularFile() && path.toString().endsWith(".nbt"))) {
            files.forEach(file -> structureId(datapacks, file).ifPresent(id -> readTag(file)
                .map(tag -> fromTag(id, "file/" + datapacks.relativize(file).getName(0), tag))
                .ifPresent(snapshots::add)));
        } catch (IOException exception) {
            LOGGER.warn("Failed to scan world datapack structures: {}", exception.getMessage());
        }
        return snapshots;
    }

    private static Optional<Identifier> structureId(Path datapacks, Path file) {
        Path relative = datapacks.relativize(file);
        for (int i = 0; i < relative.getNameCount() - 3; i++) {
            if (!"data".equals(relative.getName(i).toString())) {
                continue;
            }
            String namespace = relative.getName(i + 1).toString();
            if (!"structure".equals(relative.getName(i + 2).toString())) {
                continue;
            }
            Path path = relative.subpath(i + 3, relative.getNameCount());
            String value = path.toString().replace('\\', '/');
            if (!value.endsWith(".nbt")) {
                return Optional.empty();
            }
            return Optional.of(Identifier.fromNamespaceAndPath(namespace, value.substring(0, value.length() - 4)));
        }
        return Optional.empty();
    }

    private static Optional<CompoundTag> readTag(Path file) {
        try {
            return Optional.of(NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap()));
        } catch (IOException exception) {
            LOGGER.warn("Failed to read structure template {}: {}", file, exception.getMessage());
            return Optional.empty();
        }
    }

    private static Optional<CompoundTag> readTag(Resource resource) {
        try (InputStream input = resource.open()) {
            return Optional.of(NbtIo.readCompressed(input, NbtAccounter.unlimitedHeap()));
        } catch (IOException exception) {
            LOGGER.warn("Failed to read structure template from {}: {}", resource.sourcePackId(), exception.getMessage());
            return Optional.empty();
        }
    }

    private static StructureTemplateSnapshot fromTag(Identifier id, String sourcePack, CompoundTag root) {
        ListTag size = root.getListOrEmpty("size");
        int sizeX = size.getIntOr(0, 0);
        int sizeY = size.getIntOr(1, 0);
        int sizeZ = size.getIntOr(2, 0);
        ListTag palette = palette(root);
        List<Identifier> blockIds = blockIds(palette);
        List<String> blockStates = blockStates(palette, blockIds);
        ListTag blocks = root.getListOrEmpty("blocks");
        List<RawVoxel> rawVoxels = new ArrayList<>();
        List<StructureJigsawNode> jigsaws = new ArrayList<>();
        Map<Identifier, Integer> counts = new LinkedHashMap<>();

        for (int i = 0; i < blocks.size(); i++) {
            CompoundTag block = blocks.getCompoundOrEmpty(i);
            Identifier blockId = blockId(blockIds, block.getIntOr("state", -1));
            if (blockId == null || AIR.equals(blockId)) {
                continue;
            }
            String blockState = blockState(blockStates, block.getIntOr("state", -1), blockId);

            ListTag pos = block.getListOrEmpty("pos");
            RawVoxel voxel = new RawVoxel(blockId, blockState, pos.getIntOr(0, 0), pos.getIntOr(1, 0), pos.getIntOr(2, 0));
            counts.merge(blockId, 1, Integer::sum);
            if (!STRUCTURE_VOID.equals(blockId)) {
                rawVoxels.add(voxel);
            }
            if (JIGSAW.equals(blockId)) {
                jigsaws.add(jigsaw(voxel, block.getCompound("nbt").orElse(null)));
            }
        }

        List<StructureBlockVoxel> visible = visibleVoxels(rawVoxels);
        List<StructureBlockCount> blockCounts = counts.entrySet().stream()
            .sorted(Map.Entry.<Identifier, Integer>comparingByValue().reversed().thenComparing(entry -> entry.getKey().toString()))
            .map(entry -> new StructureBlockCount(entry.getKey(), entry.getValue()))
            .toList();

        return new StructureTemplateSnapshot(
            id,
            sourcePack,
            sizeX,
            sizeY,
            sizeZ,
            counts.values().stream().mapToInt(Integer::intValue).sum(),
            root.getListOrEmpty("entities").size(),
            blockCounts,
            visible,
            jigsaws
        );
    }

    private static ListTag palette(CompoundTag root) {
        ListTag palettes = root.getListOrEmpty("palettes");
        if (!palettes.isEmpty()) {
            return palettes.getListOrEmpty(0);
        }
        return root.getListOrEmpty("palette");
    }

    private static List<Identifier> blockIds(ListTag palette) {
        List<Identifier> ids = new ArrayList<>(palette.size());
        for (int i = 0; i < palette.size(); i++) {
            Identifier id = Identifier.tryParse(palette.getCompoundOrEmpty(i).getStringOr("Name", ""));
            ids.add(id);
        }
        return ids;
    }

    private static List<String> blockStates(ListTag palette, List<Identifier> blockIds) {
        List<String> states = new ArrayList<>(palette.size());
        for (int i = 0; i < palette.size(); i++) {
            Identifier blockId = i < blockIds.size() ? blockIds.get(i) : null;
            states.add(blockStateString(blockId, palette.getCompoundOrEmpty(i)));
        }
        return states;
    }

    private static Identifier blockId(List<Identifier> ids, int index) {
        if (index < 0 || index >= ids.size()) {
            return null;
        }
        return ids.get(index);
    }

    private static String blockState(List<String> states, int index, Identifier fallback) {
        if (index < 0 || index >= states.size()) {
            return fallback.toString();
        }
        return states.get(index);
    }

    private static String blockStateString(Identifier blockId, CompoundTag paletteEntry) {
        String id = blockId == null ? paletteEntry.getStringOr("Name", "minecraft:air") : blockId.toString();
        CompoundTag properties = paletteEntry.getCompoundOrEmpty("Properties");
        if (properties.isEmpty()) {
            return id;
        }

        List<String> keys = new ArrayList<>(properties.keySet());
        Collections.sort(keys);
        StringBuilder builder = new StringBuilder(id).append('[');
        for (int i = 0; i < keys.size(); i++) {
            if (i > 0) {
                builder.append(',');
            }
            String key = keys.get(i);
            builder.append(key).append('=').append(properties.getStringOr(key, ""));
        }
        return builder.append(']').toString();
    }

    private static StructureJigsawNode jigsaw(RawVoxel voxel, CompoundTag nbt) {
        if (nbt == null) {
            return new StructureJigsawNode(voxel.x, voxel.y, voxel.z, "", "", "", "");
        }
        return new StructureJigsawNode(
            voxel.x,
            voxel.y,
            voxel.z,
            nbt.getStringOr("name", ""),
            nbt.getStringOr("target", ""),
            nbt.getStringOr("pool", ""),
            nbt.getStringOr("final_state", "")
        );
    }

    private static List<StructureBlockVoxel> visibleVoxels(List<RawVoxel> rawVoxels) {
        Map<Long, RawVoxel> byPos = new HashMap<>();
        for (RawVoxel voxel : rawVoxels) {
            byPos.put(key(voxel.x, voxel.y, voxel.z), voxel);
        }

        return rawVoxels.stream()
            .filter(voxel -> isSurface(voxel, byPos.keySet()))
            .sorted(Comparator.comparingInt((RawVoxel voxel) -> voxel.x + voxel.y + voxel.z).thenComparingInt(voxel -> voxel.y))
            .map(voxel -> new StructureBlockVoxel(voxel.blockId, voxel.state, voxel.x, voxel.y, voxel.z))
            .toList();
    }

    private static boolean isSurface(RawVoxel voxel, Set<Long> occupied) {
        return !occupied.contains(key(voxel.x + 1, voxel.y, voxel.z))
            || !occupied.contains(key(voxel.x - 1, voxel.y, voxel.z))
            || !occupied.contains(key(voxel.x, voxel.y + 1, voxel.z))
            || !occupied.contains(key(voxel.x, voxel.y - 1, voxel.z))
            || !occupied.contains(key(voxel.x, voxel.y, voxel.z + 1))
            || !occupied.contains(key(voxel.x, voxel.y, voxel.z - 1));
    }

    private static long key(int x, int y, int z) {
        return (((long)x & 0x1FFFFFL) << 42) | (((long)y & 0xFFFFFL) << 22) | ((long)z & 0x3FFFFFL);
    }

    private record RawVoxel(Identifier blockId, String state, int x, int y, int z) {
    }

    private StructureTemplateCatalog() {}
}
