package fr.hardel.asset_editor.network.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

public final class StructureAssemblyResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(StructureAssemblyResolver.class);

    public static Optional<StructureAssemblySnapshot> resolve(MinecraftServer server, Identifier structureId) {
        ServerLevel overworld = server.overworld();
        if (overworld == null) {
            return Optional.empty();
        }
        Optional<Structure> structure = lookupStructure(server, structureId);
        if (structure.isEmpty()) {
            return Optional.empty();
        }
        try {
            return assemble(server, overworld, structureId, structure.get());
        } catch (Exception exception) {
            LOGGER.warn("Failed to assemble worldgen structure {}", structureId, exception);
            return Optional.empty();
        }
    }

    private static Optional<Structure> lookupStructure(MinecraftServer server, Identifier structureId) {
        return server.registryAccess()
            .lookup(Registries.STRUCTURE)
            .flatMap(registry -> registry.get(ResourceKey.create(Registries.STRUCTURE, structureId)))
            .map(Holder.Reference::value);
    }

    private static Optional<StructureAssemblySnapshot> assemble(
        MinecraftServer server,
        ServerLevel overworld,
        Identifier structureId,
        Structure structure) {
        Optional<PiecesContainer> container = generatePieces(server, overworld, structure);
        if (container.isEmpty()) {
            return Optional.empty();
        }

        AssemblyAccumulator accumulator = new AssemblyAccumulator();
        int pieceIndex = 0;
        for (StructurePiece piece : container.get().pieces()) {
            absorbPiece(server, piece, pieceIndex, accumulator);
            pieceIndex++;
        }

        return accumulator.toSnapshot(structureId, pieceIndex);
    }

    private static Optional<PiecesContainer> generatePieces(MinecraftServer server, ServerLevel overworld, Structure structure) {
        RegistryAccess access = server.registryAccess();
        Predicate<Holder<Biome>> anyBiome = biome -> true;
        Structure.GenerationContext context = new Structure.GenerationContext(
            access,
            overworld.getChunkSource().getGenerator(),
            overworld.getChunkSource().getGenerator().getBiomeSource(),
            overworld.getChunkSource().randomState(),
            server.getStructureManager(),
            overworld.getSeed(),
            new ChunkPos(0, 0),
            overworld,
            anyBiome);
        return structure.findValidGenerationPoint(context)
            .map(stub -> stub.getPiecesBuilder().build())
            .filter(c -> !c.isEmpty());
    }

    private static void absorbPiece(MinecraftServer server, StructurePiece piece, int pieceIndex, AssemblyAccumulator accumulator) {
        PieceTemplate pieceTemplate = extractTemplate(piece).orElse(null);
        if (pieceTemplate == null) {
            return;
        }
        StructureTemplateSnapshot template = StructureTemplateRepository.get()
            .resolve(server, pieceTemplate.templateId())
            .orElse(null);

        if (template == null) {
            return;
        }

        BoundingBox box = piece.getBoundingBox();
        accumulator.addPieceBox(new StructurePieceBox(
            pieceTemplate.templateId(),
            pieceIndex,
            box.minX(), box.minY(), box.minZ(),
            box.maxX(), box.maxY(), box.maxZ()));

        accumulator.absorbCounts(template.blockCounts());

        Rotation rotation = pieceTemplate.rotation();
        BlockPos origin = pieceTemplate.position();
        for (StructureBlockVoxel voxel : template.voxels()) {
            BlockPos rotated = StructureTemplate.transform(
                new BlockPos(voxel.x(), voxel.y(), voxel.z()),
                Mirror.NONE,
                rotation,
                BlockPos.ZERO);
            int wx = origin.getX() + rotated.getX();
            int wy = origin.getY() + rotated.getY();
            int wz = origin.getZ() + rotated.getZ();
            int rotatedStateId = rotateStateId(voxel.blockStateId(), rotation);
            int rotatedFinalStateId = voxel.finalStateId() == 0 ? 0 : rotateStateId(voxel.finalStateId(), rotation);
            accumulator.addVoxel(new StructureAssemblyVoxel(voxel.blockId(), rotatedStateId, wx, wy, wz, pieceIndex, rotatedFinalStateId));
        }
    }

    private static Optional<PieceTemplate> extractTemplate(StructurePiece piece) {
        try {
            if (piece instanceof PoolElementStructurePiece pool && pool.element instanceof SinglePoolElement single) {
                return Optional.of(new PieceTemplate(single.getTemplateLocation(), pool.position, pool.rotation));
            }
            if (piece instanceof TemplateStructurePiece template) {
                return Optional.of(new PieceTemplate(
                    template.makeTemplateLocation(),
                    template.templatePosition(),
                    template.getRotation()));
            }
        } catch (RuntimeException exception) {
            LOGGER.debug("Skipped piece {} during assembly: {}", piece.getClass().getSimpleName(), exception.getMessage());
        }
        return Optional.empty();
    }

    private static int rotateStateId(int stateId, Rotation rotation) {
        BlockState state = Block.stateById(stateId);
        return Block.BLOCK_STATE_REGISTRY.getId(state.rotate(rotation));
    }

    private record PieceTemplate(Identifier templateId, BlockPos position, Rotation rotation) {}

    private static final class AssemblyAccumulator {
        private final List<StructureAssemblyVoxel> voxels = new ArrayList<>();
        private final List<StructurePieceBox> pieceBoxes = new ArrayList<>();
        private final Map<Identifier, Integer> aggregatedCounts = new HashMap<>();
        private int totalBlocks = 0;
        private int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        private int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

        void addPieceBox(StructurePieceBox box) {
            pieceBoxes.add(box);
        }

        void addVoxel(StructureAssemblyVoxel voxel) {
            voxels.add(voxel);
            if (voxel.x() < minX)
                minX = voxel.x();
            if (voxel.y() < minY)
                minY = voxel.y();
            if (voxel.z() < minZ)
                minZ = voxel.z();
            if (voxel.x() > maxX)
                maxX = voxel.x();
            if (voxel.y() > maxY)
                maxY = voxel.y();
            if (voxel.z() > maxZ)
                maxZ = voxel.z();
        }

        void absorbCounts(List<StructureBlockCount> counts) {
            for (StructureBlockCount count : counts) {
                aggregatedCounts.merge(count.blockId(), count.count(), Integer::sum);
                totalBlocks += count.count();
            }
        }

        Optional<StructureAssemblySnapshot> toSnapshot(Identifier structureId, int pieceCount) {
            if (voxels.isEmpty()) {
                return Optional.empty();
            }
            int dx = minX, dy = minY, dz = minZ;
            List<StructureAssemblyVoxel> normalizedVoxels = voxels.stream()
                .map(v -> new StructureAssemblyVoxel(v.blockId(), v.blockStateId(), v.x() - dx, v.y() - dy, v.z() - dz, v.pieceIndex(), v.finalStateId()))
                .toList();
            List<StructurePieceBox> normalizedBoxes = pieceBoxes.stream()
                .map(b -> new StructurePieceBox(
                    b.templateId(),
                    b.pieceIndex(),
                    b.minX() - dx, b.minY() - dy, b.minZ() - dz,
                    b.maxX() - dx, b.maxY() - dy, b.maxZ() - dz))
                .toList();
            List<StructureBlockCount> blockCounts = aggregatedCounts.entrySet().stream()
                .sorted(Map.Entry.<Identifier, Integer> comparingByValue().reversed().thenComparing(entry -> entry.getKey().toString()))
                .map(entry -> new StructureBlockCount(entry.getKey(), entry.getValue()))
                .toList();
            return Optional.of(new StructureAssemblySnapshot(
                structureId,
                maxX - minX + 1,
                maxY - minY + 1,
                maxZ - minZ + 1,
                pieceCount,
                totalBlocks,
                blockCounts,
                normalizedVoxels,
                normalizedBoxes));
        }
    }

    private StructureAssemblyResolver() {}
}
