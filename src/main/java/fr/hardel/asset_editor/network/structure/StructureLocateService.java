package fr.hardel.asset_editor.network.structure;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public final class StructureLocateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StructureLocateService.class);
    private static final int SEARCH_RADIUS_CHUNKS = 100;

    public static Optional<StructureAssemblyParameters> locate(ServerPlayer player, Identifier structureId) {
        ServerLevel level = player.level();
        if (level == null) {
            return Optional.empty();
        }

        Optional<Holder.Reference<Structure>> structureHolder = level.registryAccess()
            .lookup(Registries.STRUCTURE)
            .flatMap(registry -> registry.get(ResourceKey.create(Registries.STRUCTURE, structureId)));

        if (structureHolder.isEmpty()) {
            return Optional.empty();
        }

        BlockPos origin = player.blockPosition();
        try {
            HolderSet<Structure> target = HolderSet.direct(structureHolder.get());
            Pair<BlockPos, Holder<Structure>> nearest = level.getChunkSource()
                .getGenerator()
                .findNearestMapStructure(level, target, origin, SEARCH_RADIUS_CHUNKS, false);

            if (nearest == null) {
                return Optional.empty();
            }

            BlockPos position = nearest.getFirst();
            return Optional.of(new StructureAssemblyParameters(level.getSeed(), position.getX() >> 4, position.getZ() >> 4));
        } catch (Exception exception) {
            LOGGER.warn("Failed to locate structure {} for {}", structureId, player.getGameProfile().name(), exception);
            return Optional.empty();
        }
    }

    private StructureLocateService() {}
}
