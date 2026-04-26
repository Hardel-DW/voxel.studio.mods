package fr.hardel.asset_editor.network.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
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

        Optional<Holder.Reference<Structure>> structureHolder = server.registryAccess()
            .lookup(Registries.STRUCTURE)
            .flatMap(registry -> registry.get(ResourceKey.create(Registries.STRUCTURE, structureId)));
        if (structureHolder.isEmpty()) {
            return Optional.empty();
        }

        try {
            return assemble(server, overworld, structureId, structureHolder.get().value());
        } catch (Exception exception) {
            LOGGER.warn("Failed to assemble worldgen structure {}: {}", structureId, exception.getMessage());
            return Optional.empty();
        }
    }

    private static Optional<StructureAssemblySnapshot> assemble(
        MinecraftServer server,
        ServerLevel overworld,
        Identifier structureId,
        Structure structure
    ) {
        RegistryAccess access = server.registryAccess();
        Predicate<Holder<net.minecraft.world.level.biome.Biome>> anyBiome = biome -> true;
        Structure.GenerationContext context = new Structure.GenerationContext(
            access,
            overworld.getChunkSource().getGenerator(),
            overworld.getChunkSource().getGenerator().getBiomeSource(),
            overworld.getChunkSource().randomState(),
            server.getStructureManager(),
            overworld.getSeed(),
            new net.minecraft.world.level.ChunkPos(0, 0),
            overworld,
            anyBiome
        );

        Optional<Structure.GenerationStub> stub = structure.findValidGenerationPoint(context);
        if (stub.isEmpty()) {
            return Optional.empty();
        }

        StructurePiecesBuilder builder = stub.get().getPiecesBuilder();
        PiecesContainer container = builder.build();
        if (container.isEmpty()) {
            return Optional.empty();
        }

        Map<Identifier, StructureTemplateSnapshot> templates = StructureTemplateCatalog.build(server).stream()
            .collect(HashMap::new, (m, s) -> m.put(s.id(), s), HashMap::putAll);

        List<StructureAssemblyVoxel> voxels = new ArrayList<>();
        List<StructurePieceBox> pieceBoxes = new ArrayList<>();
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        int pieceIndex = 0;

        for (StructurePiece piece : container.pieces()) {
            if (!(piece instanceof PoolElementStructurePiece pool)) {
                pieceIndex++;
                continue;
            }
            StructurePoolElement element = pool.element;
            if (!(element instanceof SinglePoolElement single)) {
                pieceIndex++;
                continue;
            }

            Identifier templateId;
            try {
                templateId = single.getTemplateLocation();
            } catch (Exception ignored) {
                pieceIndex++;
                continue;
            }
            StructureTemplateSnapshot template = templates.get(templateId);
            if (template == null) {
                pieceIndex++;
                continue;
            }

            BlockPos position = pool.position;
            Rotation rotation = pool.rotation;
            BoundingBox box = pool.getBoundingBox();
            pieceBoxes.add(new StructurePieceBox(templateId, pieceIndex, box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ()));

            for (StructureBlockVoxel voxel : template.voxels()) {
                BlockPos rotatedPos = StructureTemplate.transform(
                    new BlockPos(voxel.x(), voxel.y(), voxel.z()),
                    Mirror.NONE,
                    rotation,
                    BlockPos.ZERO
                );
                int wx = position.getX() + rotatedPos.getX();
                int wy = position.getY() + rotatedPos.getY();
                int wz = position.getZ() + rotatedPos.getZ();
                int rotatedStateId = rotateStateId(voxel.blockStateId(), rotation);
                int rotatedFinalStateId = voxel.finalStateId() == 0 ? 0 : rotateStateId(voxel.finalStateId(), rotation);
                voxels.add(new StructureAssemblyVoxel(voxel.blockId(), rotatedStateId, wx, wy, wz, pieceIndex, rotatedFinalStateId));
                if (wx < minX) minX = wx;
                if (wy < minY) minY = wy;
                if (wz < minZ) minZ = wz;
                if (wx > maxX) maxX = wx;
                if (wy > maxY) maxY = wy;
                if (wz > maxZ) maxZ = wz;
            }
            pieceIndex++;
        }

        if (voxels.isEmpty()) {
            return Optional.empty();
        }

        int finalMinX = minX, finalMinY = minY, finalMinZ = minZ;
        List<StructureAssemblyVoxel> normalized = voxels.stream()
            .map(v -> new StructureAssemblyVoxel(v.blockId(), v.blockStateId(), v.x() - finalMinX, v.y() - finalMinY, v.z() - finalMinZ, v.pieceIndex(), v.finalStateId()))
            .toList();
        List<StructurePieceBox> normalizedBoxes = pieceBoxes.stream()
            .map(b -> new StructurePieceBox(
                b.templateId(),
                b.pieceIndex(),
                b.minX() - finalMinX, b.minY() - finalMinY, b.minZ() - finalMinZ,
                b.maxX() - finalMinX, b.maxY() - finalMinY, b.maxZ() - finalMinZ))
            .toList();

        return Optional.of(new StructureAssemblySnapshot(
            structureId,
            maxX - minX + 1,
            maxY - minY + 1,
            maxZ - minZ + 1,
            pieceIndex,
            normalized,
            normalizedBoxes
        ));
    }

    private static int rotateStateId(int stateId, Rotation rotation) {
        BlockState state = Block.stateById(stateId);
        return Block.BLOCK_STATE_REGISTRY.getId(state.rotate(rotation));
    }

    private StructureAssemblyResolver() {}
}
