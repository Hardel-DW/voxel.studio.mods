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
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.CachedOrthoProjectionMatrixBuffer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.rendertype.RenderType;
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
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;
import org.joml.Matrix4fStack;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public final class StructureSceneRenderer {
    private static final Logger LOGGER = LoggerFactory.getLogger(StructureSceneRenderer.class);
    private static final boolean DEBUG_TIMING = true;
    private static final int MAX_WIDTH = 4096;
    private static final int MAX_HEIGHT = 4096;
    private static final float NEAR_PLANE = -32768.0F;
    private static final float FAR_PLANE = 32768.0F;
    private static final int CACHE_CAPACITY = 3;
    private static final int MAX_PENDING = 4;

    private static final AtomicReference<Request> pendingRequest = new AtomicReference<>();
    private static final CopyOnWriteArrayList<Consumer<String>> listeners = new CopyOnWriteArrayList<>();
    private static final Map<String, BlockState> stateCache = new ConcurrentHashMap<>();

    private static final Map<Integer, CompiledScene> sceneCache = Collections.synchronizedMap(new LinkedHashMap<>(CACHE_CAPACITY + 1, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Integer, CompiledScene> eldest) {
            return size() > CACHE_CAPACITY;
        }
    });
    private static final Set<Integer> pendingHashes = ConcurrentHashMap.newKeySet();
    private static final Queue<CompletedScene> completedScenes = new ConcurrentLinkedQueue<>();
    private static final ExecutorService tessellationExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "StructureScene-Tessellate");
        t.setDaemon(true);
        return t;
    });

    private static volatile Result result;
    private static volatile CachedLevel cachedLevel;
    private static volatile int lastDrawnHash = 0;

    private record CachedLevel(int sceneHash, StructureBlockView view) {}
    private record CompletedScene(int hash, CompiledScene scene) {}

    public record Camera(float yaw, float pitch, float zoom, float panX, float panY) {}

    public record Voxel(Identifier blockId, String state, int x, int y, int z, float yOffset, boolean highlighted) {}

    public record Request(String key, int width, int height, int sizeX, int sizeY, int sizeZ, List<Voxel> voxels, Camera camera) {}

    public record Result(String key, int width, int height, int[] argbPixels) {}

    public static void request(Request request) {
        if (request.width() <= 0 || request.height() <= 0) {
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
        long t0 = DEBUG_TIMING ? System.nanoTime() : 0L;
        try {
            render(request);
        } catch (Exception exception) {
            LOGGER.error("Structure scene render failed for {}", request.key(), exception);
            return;
        }
        if (DEBUG_TIMING) {
            long elapsedMs = (System.nanoTime() - t0) / 1_000_000L;
            LOGGER.info("[render] total={}ms voxels={} viewport={}x{}", elapsedMs, request.voxels().size(), request.width(), request.height());
        }
    }

    private static void render(Request request) {
        if (request.voxels().isEmpty()) {
            return;
        }

        drainCompletedScenes();
        int hash = sceneHash(request);
        CompiledScene compiled = sceneCache.get(hash);
        if (compiled == null) {
            compiled = sceneCache.get(lastDrawnHash);
        }

        if (compiled == null) {
            scheduleTessellation(request, hash);
            pendingRequest.compareAndSet(null, request);
            if (DEBUG_TIMING) LOGGER.info("[render] deferred (no cache, tessellation pending) hash={}", hash);
            return;
        }

        long t0 = DEBUG_TIMING ? System.nanoTime() : 0L;
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

        CachedOrthoProjectionMatrixBuffer projection = new CachedOrthoProjectionMatrixBuffer("structureScene", NEAR_PLANE, FAR_PLANE, true);
        RenderSystem.setProjectionMatrix(projection.getBuffer(width, height), ProjectionType.ORTHOGRAPHIC);
        RenderSystem.enableScissorForRenderTypeDraws(0, 0, width, height);

        long tSetup = DEBUG_TIMING ? System.nanoTime() : 0L;
        Minecraft mc = Minecraft.getInstance();
        mc.gameRenderer.getLighting().setupFor(Lighting.Entry.LEVEL);

        boolean cacheHit = compiled.sceneHash == hash;
        if (cacheHit) {
            lastDrawnHash = hash;
        } else {
            scheduleTessellation(request, hash);
        }
        drawCompiled(compiled, request, width, height);
        long tRender = DEBUG_TIMING ? System.nanoTime() : 0L;

        RenderSystem.disableScissorForRenderTypeDraws();
        RenderSystem.outputColorTextureOverride = savedColorOverride;
        RenderSystem.outputDepthTextureOverride = savedDepthOverride;
        RenderSystem.restoreProjectionMatrix();
        projection.close();
        depthView.close();
        depthTexture.close();

        readback(device, colorTexture, colorView, request.key(), width, height);

        if (DEBUG_TIMING) {
            long setupMs = (tSetup - t0) / 1_000_000L;
            long drawMs = (tRender - tSetup) / 1_000_000L;
            long endMs = (System.nanoTime() - tRender) / 1_000_000L;
            LOGGER.info("[render] cache={} setup={}ms draw={}ms readback-submit={}ms", cacheHit ? "HIT" : "FALLBACK", setupMs, drawMs, endMs);
        }
    }

    private static void scheduleTessellation(Request request, int hash) {
        if (pendingHashes.size() >= MAX_PENDING || !pendingHashes.add(hash)) {
            return;
        }
        tessellationExecutor.submit(() -> {
            long t0 = DEBUG_TIMING ? System.nanoTime() : 0L;
            try {
                CompiledScene compiled = tessellate(request, hash);
                completedScenes.offer(new CompletedScene(hash, compiled));
                if (DEBUG_TIMING) {
                    long ms = (System.nanoTime() - t0) / 1_000_000L;
                    LOGGER.info("[worker] tessellate={}ms blocks={} layers={}", ms, request.voxels().size(), compiled.layers.size());
                }
            } catch (Throwable t) {
                LOGGER.error("Worker tessellation failed for hash {}", hash, t);
            } finally {
                pendingHashes.remove(hash);
            }
        });
    }

    private static void drainCompletedScenes() {
        CompletedScene done;
        while ((done = completedScenes.poll()) != null) {
            sceneCache.put(done.hash(), done.scene());
        }
    }

    private static CompiledScene tessellate(Request request, int hash) {
        StructureBlockView level = resolveLevel(request);
        Minecraft mc = Minecraft.getInstance();
        BlockRenderDispatcher blockRenderer = mc.getBlockRenderer();
        RandomSource random = RandomSource.create();
        List<BlockModelPart> parts = new ArrayList<>();

        Map<RenderType, LayerBuilder> builders = new LinkedHashMap<>();
        PoseStack poseStack = new PoseStack();

        for (Voxel voxel : request.voxels()) {
            BlockState state = blockState(voxel);
            poseStack.pushPose();
            poseStack.translate(voxel.x(), voxel.y() + voxel.yOffset(), voxel.z());
            if (voxel.highlighted()) {
                poseStack.translate(0.5F, 0.5F, 0.5F);
                poseStack.scale(1.04F, 1.04F, 1.04F);
                poseStack.translate(-0.5F, -0.5F, -0.5F);
            }
            if (state.getRenderShape() == RenderShape.MODEL && voxel.yOffset() == 0f) {
                BlockPos pos = new BlockPos(voxel.x(), voxel.y(), voxel.z());
                random.setSeed(state.getSeed(pos));
                parts.clear();
                blockRenderer.getBlockModel(state).collectParts(random, parts);
                RenderType type = ItemBlockRenderTypes.getRenderType(state);
                LayerBuilder layer = builders.computeIfAbsent(type, LayerBuilder::create);
                blockRenderer.renderBatched(state, pos, level, poseStack, layer.consumer, true, parts);
            }
            poseStack.popPose();
        }

        List<CompiledLayer> layers = new ArrayList<>(builders.size());
        for (Map.Entry<RenderType, LayerBuilder> entry : builders.entrySet()) {
            LayerBuilder lb = entry.getValue();
            MeshData mesh = lb.builder.build();
            if (mesh == null) {
                lb.byteBuilder.close();
                continue;
            }
            ByteBuffer src = mesh.vertexBuffer();
            ByteBuffer cached = ByteBuffer.allocateDirect(src.remaining());
            cached.put(src.duplicate());
            cached.flip();
            MeshData.DrawState drawState = mesh.drawState();
            mesh.close();
            lb.byteBuilder.close();
            layers.add(new CompiledLayer(entry.getKey(), cached, drawState));
        }
        return new CompiledScene(hash, layers);
    }

    private static void drawCompiled(CompiledScene compiled, Request request, int width, int height) {
        if (compiled.layers.isEmpty()) {
            return;
        }
        Matrix4fStack mvm = RenderSystem.getModelViewStack();
        mvm.pushMatrix();
        applyCameraToMvm(mvm, request, width, height);
        try {
            for (CompiledLayer layer : compiled.layers) {
                drawLayer(layer);
            }
        } finally {
            mvm.popMatrix();
        }
    }

    private static void drawLayer(CompiledLayer layer) {
        int size = layer.vertexBytes.remaining();
        if (size == 0) return;
        ByteBufferBuilder builder = new ByteBufferBuilder(size);
        try {
            long ptr = builder.reserve(size);
            ByteBuffer target = MemoryUtil.memByteBuffer(ptr, size);
            target.put(layer.vertexBytes.duplicate());
            ByteBufferBuilder.Result result = builder.build();
            if (result == null) return;
            MeshData mesh = new MeshData(result, layer.drawState);
            layer.renderType.draw(mesh);
        } finally {
            builder.close();
        }
    }

    private static void applyCameraToMvm(Matrix4fStack mvm, Request request, int width, int height) {
        Camera camera = request.camera();
        mvm.translate(width * 0.5F + camera.panX(), height * 0.5F + camera.panY(), 0.0F);
        mvm.scale(camera.zoom(), -camera.zoom(), camera.zoom());
        mvm.rotateX((float) Math.toRadians(camera.pitch()));
        mvm.rotateY((float) Math.toRadians(camera.yaw()));
        mvm.translate(-request.sizeX() * 0.5F, -request.sizeY() * 0.5F, -request.sizeZ() * 0.5F);
    }

    private static StructureBlockView resolveLevel(Request request) {
        int hash = sceneHash(request);
        CachedLevel cached = cachedLevel;
        if (cached != null && cached.sceneHash() == hash) {
            return cached.view();
        }
        StructureBlockView view = new StructureBlockView(request.voxels(), request.sizeY());
        cachedLevel = new CachedLevel(hash, view);
        return view;
    }

    private static int sceneHash(Request request) {
        int h = 1;
        h = 31 * h + request.sizeX();
        h = 31 * h + request.sizeY();
        h = 31 * h + request.sizeZ();
        h = 31 * h + request.voxels().size();
        if (!request.voxels().isEmpty()) {
            Voxel first = request.voxels().get(0);
            Voxel last = request.voxels().get(request.voxels().size() - 1);
            h = 31 * h + first.state().hashCode();
            h = 31 * h + first.x() + first.y() * 17 + first.z() * 31;
            h = 31 * h + Float.floatToIntBits(first.yOffset());
            h = 31 * h + last.state().hashCode();
            h = 31 * h + last.x() + last.y() * 17 + last.z() * 31;
            h = 31 * h + Float.floatToIntBits(last.yOffset());
        }
        return h;
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
        long t0 = DEBUG_TIMING ? System.nanoTime() : 0L;
        int bufferSize = width * height * 4;
        GpuBuffer readbackBuffer = device.createBuffer(() -> "StructureScene/Readback", GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_MAP_READ, bufferSize);

        device.createCommandEncoder().copyTextureToBuffer(colorTexture, readbackBuffer, 0, () -> {
            long tCallback = DEBUG_TIMING ? System.nanoTime() : 0L;
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
                if (DEBUG_TIMING) {
                    long convertMs = (System.nanoTime() - tCallback) / 1_000_000L;
                    long waitMs = (tCallback - t0) / 1_000_000L;
                    LOGGER.info("[readback] gpu-wait={}ms argb-convert={}ms size={}KB", waitMs, convertMs, bufferSize / 1024);
                }
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

    private static final class CompiledScene {
        final int sceneHash;
        final List<CompiledLayer> layers;

        CompiledScene(int sceneHash, List<CompiledLayer> layers) {
            this.sceneHash = sceneHash;
            this.layers = layers;
        }
    }

    private static final class CompiledLayer {
        final RenderType renderType;
        final ByteBuffer vertexBytes;
        final MeshData.DrawState drawState;

        CompiledLayer(RenderType renderType, ByteBuffer vertexBytes, MeshData.DrawState drawState) {
            this.renderType = renderType;
            this.vertexBytes = vertexBytes;
            this.drawState = drawState;
        }
    }

    private static final class LayerBuilder {
        final ByteBufferBuilder byteBuilder;
        final BufferBuilder builder;
        final VertexConsumer consumer;

        private LayerBuilder(ByteBufferBuilder byteBuilder, BufferBuilder builder) {
            this.byteBuilder = byteBuilder;
            this.builder = builder;
            this.consumer = builder;
        }

        static LayerBuilder create(RenderType type) {
            ByteBufferBuilder bb = new ByteBufferBuilder(Math.max(786432, type.bufferSize()));
            BufferBuilder builder = new BufferBuilder(bb, type.mode(), type.format());
            return new LayerBuilder(bb, builder);
        }
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
        public float getShade(@NotNull Direction direction, boolean shade) {
            if (!shade) {
                return 1.0F;
            }
            return switch (direction) {
                case DOWN -> 0.45F;
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
        public int getBrightness(@NotNull LightLayer layer, @NotNull BlockPos pos) {
            return 15;
        }

        @Override
        public int getRawBrightness(@NotNull BlockPos pos, int darkening) {
            return 15;
        }

        @Override
        public int getBlockTint(@NotNull BlockPos pos, @NotNull ColorResolver color) {
            return -1;
        }

        @Override
        public @Nullable BlockEntity getBlockEntity(@NotNull BlockPos pos) {
            return null;
        }

        @Override
        public @NotNull BlockState getBlockState(@NotNull BlockPos pos) {
            return states.getOrDefault(pos.asLong(), Blocks.AIR.defaultBlockState());
        }

        @Override
        public @NotNull FluidState getFluidState(@NotNull BlockPos pos) {
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
