package fr.hardel.asset_editor.client.rendering;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.math.Axis;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.CachedOrthoProjectionMatrixBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jspecify.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public final class StructureSceneRenderer {
    private static final Logger LOGGER = LoggerFactory.getLogger(StructureSceneRenderer.class);
    private static final int MAX_WIDTH = 1600;
    private static final int MAX_HEIGHT = 1100;

    private static final AtomicReference<Request> pendingRequest = new AtomicReference<>();
    private static final CopyOnWriteArrayList<Consumer<String>> listeners = new CopyOnWriteArrayList<>();
    private static final Map<String, BlockState> stateCache = new ConcurrentHashMap<>();

    private static volatile Result result;

    public record Camera(float yaw, float pitch, float zoom, float panX, float panY) {}

    public record Voxel(Identifier blockId, String state, int x, int y, int z, float yOffset, boolean highlighted) {}

    public record Request(String key, int width, int height, int sizeX, int sizeY, int sizeZ, List<Voxel> voxels, Camera camera) {}

    public record Result(String key, int width, int height, int[] argbPixels) {}

    public static void request(Request request) {
        if (request.width() <= 0 || request.height() <= 0 || request.voxels().isEmpty()) {
            return;
        }
        pendingRequest.set(request);
    }

    public static Result getResult(String key) {
        Result current = result;
        if (current == null || !current.key().equals(key)) {
            return null;
        }
        return current;
    }

    public static Runnable subscribe(Consumer<String> listener) {
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    public static void tick() {
        Request request = pendingRequest.getAndSet(null);
        if (request == null) {
            return;
        }

        RenderSystem.assertOnRenderThread();
        try {
            render(request);
        } catch (Exception exception) {
            LOGGER.error("Structure scene render failed for {}", request.key(), exception);
        }
    }

    private static void render(Request request) {
        int width = Math.min(request.width(), MAX_WIDTH);
        int height = Math.min(request.height(), MAX_HEIGHT);

        GpuDevice device = RenderSystem.getDevice();
        GpuTexture colorTexture = device.createTexture("StructureScene/Color", 12 | GpuTexture.USAGE_COPY_SRC, TextureFormat.RGBA8, width, height, 1, 1);
        GpuTexture depthTexture = device.createTexture("StructureScene/Depth", 8, TextureFormat.DEPTH32, width, height, 1, 1);
        GpuTextureView colorView = device.createTextureView(colorTexture);
        GpuTextureView depthView = device.createTextureView(depthTexture);

        CommandEncoder encoder = device.createCommandEncoder();
        encoder.clearColorAndDepthTextures(colorTexture, 0, depthTexture, 1.0);

        RenderSystem.backupProjectionMatrix();
        var savedColorOverride = RenderSystem.outputColorTextureOverride;
        var savedDepthOverride = RenderSystem.outputDepthTextureOverride;
        RenderSystem.outputColorTextureOverride = colorView;
        RenderSystem.outputDepthTextureOverride = depthView;

        CachedOrthoProjectionMatrixBuffer projection = new CachedOrthoProjectionMatrixBuffer("structureScene", -10000.0F, 10000.0F, true);
        RenderSystem.setProjectionMatrix(projection.getBuffer(width, height), ProjectionType.ORTHOGRAPHIC);
        RenderSystem.enableScissorForRenderTypeDraws(0, 0, width, height);

        renderVoxels(request, width, height);

        RenderSystem.disableScissorForRenderTypeDraws();
        RenderSystem.outputColorTextureOverride = savedColorOverride;
        RenderSystem.outputDepthTextureOverride = savedDepthOverride;
        RenderSystem.restoreProjectionMatrix();
        projection.close();
        depthView.close();
        depthTexture.close();

        readback(device, colorTexture, colorView, request.key(), width, height);
    }

    private static void renderVoxels(Request request, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        StructureBlockView level = new StructureBlockView(request.voxels(), request.sizeY());
        var fixedBuffers = new Object2ObjectLinkedOpenHashMap<RenderType, ByteBufferBuilder>();
        fixedBuffers.put(Sheets.translucentItemSheet(), new ByteBufferBuilder(786432));
        fixedBuffers.put(RenderTypes.armorEntityGlint(), new ByteBufferBuilder(RenderTypes.armorEntityGlint().bufferSize()));
        fixedBuffers.put(RenderTypes.glint(), new ByteBufferBuilder(RenderTypes.glint().bufferSize()));
        fixedBuffers.put(RenderTypes.glintTranslucent(), new ByteBufferBuilder(RenderTypes.glintTranslucent().bufferSize()));
        fixedBuffers.put(RenderTypes.entityGlint(), new ByteBufferBuilder(RenderTypes.entityGlint().bufferSize()));
        MultiBufferSource.BufferSource bufferSource = MultiBufferSource.immediateWithBuffers(fixedBuffers, new ByteBufferBuilder(2_097_152));

        mc.gameRenderer.getLighting().setupFor(Lighting.Entry.ITEMS_3D);

        PoseStack poseStack = new PoseStack();
        applyCamera(poseStack, request, width, height);

        BlockRenderDispatcher blockRenderer = mc.getBlockRenderer();
        RandomSource random = RandomSource.create();
        List<BlockModelPart> parts = new ArrayList<>();
        List<Voxel> sorted = request.voxels().stream()
            .sorted(Comparator.comparingInt(Voxel::y).thenComparingInt(Voxel::z).thenComparingInt(Voxel::x))
            .toList();
        for (Voxel voxel : sorted) {
            BlockState state = blockState(voxel);
            poseStack.pushPose();
            poseStack.translate(voxel.x(), voxel.y() + voxel.yOffset(), voxel.z());
            if (voxel.highlighted()) {
                poseStack.translate(0.5F, 0.5F, 0.5F);
                poseStack.scale(1.035F, 1.035F, 1.035F);
                poseStack.translate(-0.5F, -0.5F, -0.5F);
            }
            if (state.getRenderShape() == RenderShape.MODEL && voxel.yOffset() == 0f) {
                BlockPos pos = new BlockPos(voxel.x(), voxel.y(), voxel.z());
                random.setSeed(state.getSeed(pos));
                parts.clear();
                blockRenderer.getBlockModel(state).collectParts(random, parts);
                VertexConsumer consumer = bufferSource.getBuffer(ItemBlockRenderTypes.getRenderType(state));
                blockRenderer.renderBatched(state, pos, level, poseStack, consumer, true, parts);
            } else {
                blockRenderer.renderSingleBlock(state, poseStack, bufferSource, 15728880, OverlayTexture.NO_OVERLAY);
            }
            poseStack.popPose();
        }
        bufferSource.endBatch();
    }

    private static void applyCamera(PoseStack poseStack, Request request, int width, int height) {
        Camera camera = request.camera();
        poseStack.pushPose();
        poseStack.translate(width * 0.5F + camera.panX(), height * 0.5F + camera.panY(), 0.0F);
        poseStack.scale(camera.zoom(), -camera.zoom(), camera.zoom());
        poseStack.mulPose(Axis.XP.rotationDegrees(camera.pitch()));
        poseStack.mulPose(Axis.YP.rotationDegrees(camera.yaw()));
        poseStack.translate(-request.sizeX() * 0.5F, -request.sizeY() * 0.5F, -request.sizeZ() * 0.5F);
    }

    private static BlockState blockState(Voxel voxel) {
        return stateCache.computeIfAbsent(voxel.state(), key -> {
            try {
                return BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK, key, false).blockState();
            } catch (CommandSyntaxException exception) {
                return BuiltInRegistries.BLOCK.getValue(voxel.blockId()).defaultBlockState();
            }
        });
    }

    private static void readback(GpuDevice device, GpuTexture colorTexture, GpuTextureView colorView, String key, int width, int height) {
        int bufferSize = width * height * 4;
        GpuBuffer readbackBuffer = device.createBuffer(() -> "StructureScene/Readback", GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_MAP_READ, bufferSize);

        device.createCommandEncoder().copyTextureToBuffer(colorTexture, readbackBuffer, 0, () -> {
            try (GpuBuffer.MappedView view = device.createCommandEncoder().mapBuffer(readbackBuffer, true, false)) {
                ByteBuffer pixels = view.data();
                int[] argb = new int[width * height];
                for (int srcY = 0; srcY < height; srcY++) {
                    int dstY = height - 1 - srcY;
                    for (int x = 0; x < width; x++) {
                        int offset = (srcY * width + x) * 4;
                        int r = pixels.get(offset) & 0xFF;
                        int g = pixels.get(offset + 1) & 0xFF;
                        int b = pixels.get(offset + 2) & 0xFF;
                        int a = pixels.get(offset + 3) & 0xFF;
                        argb[dstY * width + x] = (a << 24) | (r << 16) | (g << 8) | b;
                    }
                }
                result = new Result(key, width, height, argb);
                for (Consumer<String> listener : listeners) {
                    listener.accept(key);
                }
            } finally {
                readbackBuffer.close();
                colorView.close();
                colorTexture.close();
            }
        }, 0);
    }

    private static final class StructureBlockView implements BlockAndTintGetter {
        private final Map<Long, BlockState> states = new HashMap<>();
        private final int height;

        StructureBlockView(List<Voxel> voxels, int height) {
            this.height = Math.max(1, height + 2);
            for (Voxel voxel : voxels) {
                if (voxel.yOffset() == 0f) {
                    states.put(BlockPos.asLong(voxel.x(), voxel.y(), voxel.z()), blockState(voxel));
                }
            }
        }

        @Override
        public float getShade(Direction direction, boolean shade) {
            if (!shade) {
                return 1.0F;
            }
            return switch (direction) {
                case DOWN -> 0.5F;
                case UP -> 1.0F;
                case NORTH, SOUTH -> 0.8F;
                case WEST, EAST -> 0.6F;
            };
        }

        @Override
        public LevelLightEngine getLightEngine() {
            return LevelLightEngine.EMPTY;
        }

        @Override
        public int getBrightness(LightLayer layer, BlockPos pos) {
            return 15;
        }

        @Override
        public int getRawBrightness(BlockPos pos, int darkening) {
            return 15;
        }

        @Override
        public int getBlockTint(BlockPos pos, ColorResolver color) {
            return -1;
        }

        @Override
        public @Nullable BlockEntity getBlockEntity(BlockPos pos) {
            return null;
        }

        @Override
        public BlockState getBlockState(BlockPos pos) {
            return states.getOrDefault(pos.asLong(), Blocks.AIR.defaultBlockState());
        }

        @Override
        public FluidState getFluidState(BlockPos pos) {
            return states.getOrDefault(pos.asLong(), Blocks.AIR.defaultBlockState()).getFluidState();
        }

        @Override
        public int getHeight() {
            return height;
        }

        @Override
        public int getMinY() {
            return 0;
        }
    }

    private StructureSceneRenderer() {}
}
