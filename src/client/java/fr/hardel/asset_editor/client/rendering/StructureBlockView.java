package fr.hardel.asset_editor.client.rendering;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Minimal {@link BlockAndTintGetter} backed by a pre-built state map; multiple views can share the same map. */
final class StructureBlockView implements BlockAndTintGetter {

    private final Map<Long, BlockState> states;
    private final int height;
    private final int maxYInclusive;

    StructureBlockView(Map<Long, BlockState> states, int sizeY, int maxYInclusive) {
        this.height = Math.max(1, sizeY + 2);
        this.maxYInclusive = maxYInclusive;
        this.states = states;
    }

    static Map<Long, BlockState> buildStateMap(List<StructureSceneRenderer.Voxel> voxels) {
        Map<Long, BlockState> map = new HashMap<>(Math.max(16, voxels.size()));
        for (StructureSceneRenderer.Voxel v : voxels) {
            BlockState state = Block.stateById(v.blockStateId());
            map.put(BlockPos.asLong(v.x(), v.y(), v.z()), state == null ? Blocks.AIR.defaultBlockState() : state);
        }
        return map;
    }

    @Override
    public float getShade(@NotNull Direction direction, boolean shade) {
        if (!shade) return 1.0F;
        return switch (direction) {
            case DOWN -> 0.45F;
            case UP -> 1.0F;
            case NORTH, SOUTH -> 0.8F;
            case WEST, EAST -> 0.6F;
        };
    }

    @Override public LevelLightEngine getLightEngine() { return LevelLightEngine.EMPTY; }
    @Override public int getBrightness(@NotNull LightLayer layer, @NotNull BlockPos pos) { return 15; }
    @Override public int getRawBrightness(@NotNull BlockPos pos, int darkening) { return 15; }
    @Override public int getBlockTint(@NotNull BlockPos pos, @NotNull ColorResolver color) { return -1; }
    @Override public @Nullable BlockEntity getBlockEntity(@NotNull BlockPos pos) { return null; }
    @Override public int getHeight() { return height; }
    @Override public int getMinY() { return 0; }

    @Override
    public @NotNull BlockState getBlockState(@NotNull BlockPos pos) {
        if (pos.getY() > maxYInclusive) return Blocks.AIR.defaultBlockState();
        return states.getOrDefault(pos.asLong(), Blocks.AIR.defaultBlockState());
    }

    @Override
    public @NotNull FluidState getFluidState(@NotNull BlockPos pos) {
        return getBlockState(pos).getFluidState();
    }
}
