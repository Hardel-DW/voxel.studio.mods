package fr.hardel.asset_editor.network.structure;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    public static List<StructureTemplateSnapshot> build(MinecraftServer server, List<Identifier> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        ResourceManager resources = server.getResourceManager();
        Map<Identifier, StructureTemplateSnapshot> snapshots = new LinkedHashMap<>();
        for (Identifier id : ids) {
            Identifier fileId = LISTER.idToFile(id);
            resources.getResource(fileId)
                .flatMap(resource -> read(fileId, resource))
                .ifPresent(snapshot -> snapshots.put(snapshot.id(), snapshot));
        }
        readWorldDatapacks(server, ids).stream()
            .filter(snapshot -> ids.contains(snapshot.id()))
            .forEach(snapshot -> snapshots.put(snapshot.id(), snapshot));

        return ids.stream()
            .map(snapshots::get)
            .filter(java.util.Objects::nonNull)
            .toList();
    }

    public static List<StructureTemplateIndexEntry> buildIndex(MinecraftServer server) {
        ResourceManager resources = server.getResourceManager();
        Map<Identifier, StructureTemplateIndexEntry> entries = new LinkedHashMap<>();
        LISTER.listMatchingResources(resources).forEach((fileId, resource) -> {
            Identifier id = LISTER.fileToId(fileId);
            entries.put(id, new StructureTemplateIndexEntry(id, resource.sourcePackId()));
        });
        readWorldDatapackIndex(server).forEach(entry -> entries.put(entry.id(), entry));
        return entries.values().stream()
            .sorted(Comparator.comparing(entry -> entry.id().toString()))
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

    private static List<StructureTemplateSnapshot> readWorldDatapacks(MinecraftServer server, List<Identifier> ids) {
        Path datapacks = server.getWorldPath(LevelResource.DATAPACK_DIR);
        if (!Files.isDirectory(datapacks)) {
            return List.of();
        }

        List<StructureTemplateSnapshot> snapshots = new ArrayList<>();
        try (Stream<Path> packs = Files.list(datapacks)) {
            packs.filter(Files::isDirectory).forEach(packRoot -> {
                String sourcePack = "file/" + datapacks.relativize(packRoot).getName(0);
                for (Identifier id : ids) {
                    Path file = structurePath(packRoot, id);
                    if (!Files.isRegularFile(file)) {
                        continue;
                    }
                    readTag(file)
                        .map(tag -> fromTag(id, sourcePack, tag))
                        .ifPresent(snapshots::add);
                }
            });
        } catch (IOException exception) {
            LOGGER.warn("Failed to read world datapack structures by id: {}", exception.getMessage());
        }
        return snapshots;
    }

    private static List<StructureTemplateIndexEntry> readWorldDatapackIndex(MinecraftServer server) {
        Path datapacks = server.getWorldPath(LevelResource.DATAPACK_DIR);
        if (!Files.isDirectory(datapacks)) {
            return List.of();
        }

        List<StructureTemplateIndexEntry> entries = new ArrayList<>();
        try (Stream<Path> files = Files.find(datapacks, Integer.MAX_VALUE, (path, attrs) -> attrs.isRegularFile() && path.toString().endsWith(".nbt"))) {
            files.forEach(file -> structureId(datapacks, file)
                .map(id -> new StructureTemplateIndexEntry(id, "file/" + datapacks.relativize(file).getName(0)))
                .ifPresent(entries::add));
        } catch (IOException exception) {
            LOGGER.warn("Failed to scan world datapack structure index: {}", exception.getMessage());
        }
        return entries;
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
        List<Integer> blockStateIds = blockStateIds(palette);
        ListTag blocks = root.getListOrEmpty("blocks");
        List<RawVoxel> rawVoxels = new ArrayList<>();
        List<StructureJigsawNode> jigsaws = new ArrayList<>();
        Map<Identifier, Integer> counts = new LinkedHashMap<>();

        for (int i = 0; i < blocks.size(); i++) {
            CompoundTag block = blocks.getCompoundOrEmpty(i);
            int paletteIndex = block.getIntOr("state", -1);
            Identifier blockId = blockId(blockIds, paletteIndex);
            if (blockId == null || AIR.equals(blockId)) {
                continue;
            }
            int stateId = stateId(blockStateIds, paletteIndex);
            CompoundTag blockNbt = block.getCompound("nbt").orElse(null);
            int finalStateId = JIGSAW.equals(blockId) ? parseFinalStateId(blockNbt) : 0;

            ListTag pos = block.getListOrEmpty("pos");
            RawVoxel voxel = new RawVoxel(blockId, stateId, pos.getIntOr(0, 0), pos.getIntOr(1, 0), pos.getIntOr(2, 0), finalStateId);
            counts.merge(blockId, 1, Integer::sum);
            if (!STRUCTURE_VOID.equals(blockId)) {
                rawVoxels.add(voxel);
            }
            if (JIGSAW.equals(blockId)) {
                jigsaws.add(jigsaw(voxel, blockNbt));
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

    private static List<Integer> blockStateIds(ListTag palette) {
        List<Integer> ids = new ArrayList<>(palette.size());
        for (int i = 0; i < palette.size(); i++) {
            BlockState state = NbtUtils.readBlockState(BuiltInRegistries.BLOCK, palette.getCompoundOrEmpty(i));
            ids.add(Block.BLOCK_STATE_REGISTRY.getId(state));
        }
        return ids;
    }

    private static Identifier blockId(List<Identifier> ids, int index) {
        if (index < 0 || index >= ids.size()) {
            return null;
        }
        return ids.get(index);
    }

    private static int stateId(List<Integer> ids, int index) {
        if (index < 0 || index >= ids.size()) {
            return Block.BLOCK_STATE_REGISTRY.getId(Blocks.AIR.defaultBlockState());
        }
        return ids.get(index);
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
            .filter(voxel -> isSurface(voxel, byPos))
            .sorted(Comparator.comparingInt((RawVoxel voxel) -> voxel.x + voxel.y + voxel.z).thenComparingInt(voxel -> voxel.y))
            .map(voxel -> new StructureBlockVoxel(voxel.blockId, voxel.stateId, voxel.x, voxel.y, voxel.z, voxel.finalStateId))
            .toList();
    }

    private static int parseFinalStateId(CompoundTag nbt) {
        if (nbt == null) return 0;
        String value = nbt.getStringOr("final_state", "");
        if (value.isBlank() || "minecraft:air".equals(value)) return 0;
        try {
            BlockStateParser.BlockResult result = BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK, value, false);
            return Block.BLOCK_STATE_REGISTRY.getId(result.blockState());
        } catch (CommandSyntaxException exception) {
            LOGGER.debug("Unparseable jigsaw final_state '{}': {}", value, exception.getMessage());
            return 0;
        }
    }

    private static boolean isSurface(RawVoxel voxel, Map<Long, RawVoxel> byPos) {
        return !occludesFace(byPos, voxel.x + 1, voxel.y, voxel.z, Direction.WEST)
            || !occludesFace(byPos, voxel.x - 1, voxel.y, voxel.z, Direction.EAST)
            || !occludesFace(byPos, voxel.x, voxel.y + 1, voxel.z, Direction.DOWN)
            || !occludesFace(byPos, voxel.x, voxel.y - 1, voxel.z, Direction.UP)
            || !occludesFace(byPos, voxel.x, voxel.y, voxel.z + 1, Direction.NORTH)
            || !occludesFace(byPos, voxel.x, voxel.y, voxel.z - 1, Direction.SOUTH);
    }

    private static boolean occludesFace(Map<Long, RawVoxel> byPos, int x, int y, int z, Direction faceTowardUs) {
        RawVoxel neighbor = byPos.get(key(x, y, z));
        if (neighbor == null) {
            return false;
        }
        BlockState state = Block.stateById(neighbor.stateId);
        if (state == null || !state.canOcclude()) {
            return false;
        }
        return Block.isFaceFull(state.getOcclusionShape(), faceTowardUs);
    }

    private static long key(int x, int y, int z) {
        return (((long)x & 0x1FFFFFL) << 42) | (((long)y & 0xFFFFFL) << 22) | ((long)z & 0x3FFFFFL);
    }

    private record RawVoxel(Identifier blockId, int stateId, int x, int y, int z, int finalStateId) {
    }

    private StructureTemplateCatalog() {}
}
