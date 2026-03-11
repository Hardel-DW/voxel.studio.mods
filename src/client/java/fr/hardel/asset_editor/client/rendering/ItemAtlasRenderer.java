package fr.hardel.asset_editor.client.rendering;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.CachedOrthoProjectionMatrixBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.TrackingItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ItemAtlasRenderer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ItemAtlasRenderer.class);
    private static final Identifier DEBUG_TEXTURE_ID = Identifier.fromNamespaceAndPath("asset_editor", "item_atlas");
    private static final int ITEM_SIZE = 32;

    private static final AtomicBoolean needsGeneration = new AtomicBoolean(false);
    private static volatile int[] argbPixels;
    private static volatile int atlasWidth;
    private static volatile int atlasHeight;
    private static volatile Map<Identifier, AtlasEntry> entries = Map.of();
    private static volatile long generation;

    public record AtlasEntry(int x, int y, int size) {}

    public static boolean isReady() {
        return argbPixels != null;
    }

    public static AtlasEntry getEntry(Identifier itemId) {
        return entries.get(itemId);
    }

    public static int[] getArgbPixels() {
        return argbPixels;
    }

    public static int getWidth() {
        return atlasWidth;
    }

    public static int getHeight() {
        return atlasHeight;
    }

    public static long getGeneration() {
        return generation;
    }

    public static void requestGeneration() {
        needsGeneration.set(true);
    }

    public static void tryGenerate() {
        if (!needsGeneration.compareAndSet(true, false)) return;
        RenderSystem.assertOnRenderThread();
        generate();
    }

    private static void generate() {
        Minecraft mc = Minecraft.getInstance();
        ItemModelResolver modelResolver = mc.getItemModelResolver();

        List<Identifier> itemIds = new ArrayList<>(BuiltInRegistries.ITEM.keySet());
        int itemCount = itemIds.size();
        int gridCols = Mth.smallestEncompassingPowerOfTwo((int) Math.ceil(Math.sqrt(itemCount)));
        int gridRows = (itemCount + gridCols - 1) / gridCols;
        int width = gridCols * ITEM_SIZE;
        int height = gridRows * ITEM_SIZE;

        SubmitNodeStorage submitStorage = new SubmitNodeStorage();
        var fixedBuffers = new Object2ObjectLinkedOpenHashMap<RenderType, ByteBufferBuilder>();
        fixedBuffers.put(Sheets.translucentItemSheet(), new ByteBufferBuilder(786432));
        fixedBuffers.put(RenderTypes.armorEntityGlint(), new ByteBufferBuilder(RenderTypes.armorEntityGlint().bufferSize()));
        fixedBuffers.put(RenderTypes.glint(), new ByteBufferBuilder(RenderTypes.glint().bufferSize()));
        fixedBuffers.put(RenderTypes.glintTranslucent(), new ByteBufferBuilder(RenderTypes.glintTranslucent().bufferSize()));
        fixedBuffers.put(RenderTypes.entityGlint(), new ByteBufferBuilder(RenderTypes.entityGlint().bufferSize()));
        MultiBufferSource.BufferSource bufferSource = MultiBufferSource.immediateWithBuffers(fixedBuffers, new ByteBufferBuilder(786432));
        FeatureRenderDispatcher featureDispatcher = new FeatureRenderDispatcher(
                submitStorage,
                mc.getBlockRenderer(),
                bufferSource,
                mc.getAtlasManager(),
                new OutlineBufferSource(),
                MultiBufferSource.immediate(new ByteBufferBuilder(1536)),
                mc.font
        );

        GpuDevice device = RenderSystem.getDevice();
        GpuTexture colorTexture = device.createTexture("ItemAtlas/Color", 12 | GpuTexture.USAGE_COPY_SRC, TextureFormat.RGBA8, width, height, 1, 1);
        GpuTexture depthTexture = device.createTexture("ItemAtlas/Depth", 8, TextureFormat.DEPTH32, width, height, 1, 1);
        var colorView = device.createTextureView(colorTexture);
        var depthView = device.createTextureView(depthTexture);

        CommandEncoder encoder = device.createCommandEncoder();
        encoder.clearColorAndDepthTextures(colorTexture, 0, depthTexture, 1.0);

        RenderSystem.backupProjectionMatrix();
        var savedColorOverride = RenderSystem.outputColorTextureOverride;
        var savedDepthOverride = RenderSystem.outputDepthTextureOverride;

        RenderSystem.outputColorTextureOverride = colorView;
        RenderSystem.outputDepthTextureOverride = depthView;

        CachedOrthoProjectionMatrixBuffer projBuffer = new CachedOrthoProjectionMatrixBuffer("itemAtlas", -1000.0F, 1000.0F, true);
        RenderSystem.setProjectionMatrix(projBuffer.getBuffer(width, height), ProjectionType.ORTHOGRAPHIC);

        mc.gameRenderer.getLighting().setupFor(Lighting.Entry.ITEMS_3D);

        Map<Identifier, AtlasEntry> newEntries = new ConcurrentHashMap<>();
        TrackingItemStackRenderState renderState = new TrackingItemStackRenderState();
        PoseStack poseStack = new PoseStack();

        for (int i = 0; i < itemIds.size(); i++) {
            Identifier itemId = itemIds.get(i);
            Item item = BuiltInRegistries.ITEM.getValue(itemId);
            ItemStack stack = new ItemStack(item);

            renderState.clear();
            modelResolver.updateForTopItem(renderState, stack, ItemDisplayContext.GUI, mc.level, null, 0);
            if (renderState.isEmpty()) continue;

            int col = i % gridCols;
            int row = i / gridCols;
            int renderX = col * ITEM_SIZE;
            int renderY = row * ITEM_SIZE;

            newEntries.put(itemId, new AtlasEntry(renderX, renderY, ITEM_SIZE));
            renderItem(renderState, poseStack, renderX, renderY, height, featureDispatcher, bufferSource, submitStorage);
        }

        RenderSystem.outputColorTextureOverride = savedColorOverride;
        RenderSystem.outputDepthTextureOverride = savedDepthOverride;
        RenderSystem.restoreProjectionMatrix();
        projBuffer.close();

        depthView.close();
        depthTexture.close();

        readback(device, colorTexture, colorView, width, height, newEntries);
    }

    private static void renderItem(
            TrackingItemStackRenderState renderState,
            PoseStack poseStack,
            int renderX, int renderY,
            int atlasHeight,
            FeatureRenderDispatcher featureDispatcher,
            MultiBufferSource.BufferSource bufferSource,
            SubmitNodeStorage submitStorage
    ) {
        Minecraft mc = Minecraft.getInstance();
        poseStack.pushPose();
        poseStack.translate(renderX + ITEM_SIZE / 2.0F, renderY + ITEM_SIZE / 2.0F, 0.0F);
        poseStack.scale(ITEM_SIZE, -ITEM_SIZE, ITEM_SIZE);

        boolean flat = !renderState.usesBlockLight();
        mc.gameRenderer.getLighting().setupFor(flat ? Lighting.Entry.ITEMS_FLAT : Lighting.Entry.ITEMS_3D);

        RenderSystem.enableScissorForRenderTypeDraws(renderX, atlasHeight - renderY - ITEM_SIZE, ITEM_SIZE, ITEM_SIZE);
        renderState.submit(poseStack, submitStorage, 15728880, OverlayTexture.NO_OVERLAY, 0);
        featureDispatcher.renderAllFeatures();
        bufferSource.endBatch();
        RenderSystem.disableScissorForRenderTypeDraws();

        poseStack.popPose();
    }

    private static void readback(GpuDevice device, GpuTexture colorTexture, GpuTextureView colorView, int width, int height, Map<Identifier, AtlasEntry> newEntries) {
        int bufferSize = width * height * 4;
        GpuBuffer readbackBuffer = device.createBuffer(() -> "ItemAtlas/Readback", GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_MAP_READ, bufferSize);

        device.createCommandEncoder().copyTextureToBuffer(colorTexture, readbackBuffer, 0, () -> {
            try (GpuBuffer.MappedView view = device.createCommandEncoder().mapBuffer(readbackBuffer, true, false)) {
                ByteBuffer pixels = view.data();
                int[] result = new int[width * height];

                for (int srcY = 0; srcY < height; srcY++) {
                    int dstY = height - 1 - srcY;
                    for (int x = 0; x < width; x++) {
                        int srcOffset = (srcY * width + x) * 4;
                        int r = pixels.get(srcOffset) & 0xFF;
                        int g = pixels.get(srcOffset + 1) & 0xFF;
                        int b = pixels.get(srcOffset + 2) & 0xFF;
                        int a = pixels.get(srcOffset + 3) & 0xFF;
                        result[dstY * width + x] = (a << 24) | (r << 16) | (g << 8) | b;
                    }
                }

                atlasWidth = width;
                atlasHeight = height;
                entries = newEntries;
                argbPixels = result;
                generation++;
                registerDebugTexture(result, width, height);
                LOGGER.info("Item atlas rendered: {}x{} with {} items", width, height, newEntries.size());
            } finally {
                readbackBuffer.close();
                colorView.close();
                colorTexture.close();
            }
        }, 0);
    }

    private static void registerDebugTexture(int[] argb, int width, int height) {
        NativeImage image = new NativeImage(width, height, false);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                image.setPixel(x, y, argb[y * width + x]);
            }
        }
        Minecraft.getInstance().getTextureManager().register(DEBUG_TEXTURE_ID, new DumpableAtlasTexture(image, entries));
    }

    private static final class DumpableAtlasTexture extends DynamicTexture {
        private final Map<Identifier, AtlasEntry> entries;

        DumpableAtlasTexture(NativeImage image, Map<Identifier, AtlasEntry> entries) {
            super(image::toString, image);
            this.entries = entries;
        }

        @Override
        public void dumpContents(Identifier selfId, Path dir) throws IOException {
            super.dumpContents(selfId, dir);
            String baseName = selfId.toDebugFileName();
            try (var writer = Files.newBufferedWriter(dir.resolve(baseName + ".png.txt"))) {
                entries.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .forEach(e -> {
                            AtlasEntry entry = e.getValue();
                            try {
                                writer.write("%s\tx=%d\ty=%d\tw=%d\th=%d%n".formatted(
                                        e.getKey(), entry.x(), entry.y(), entry.size(), entry.size()));
                            } catch (IOException ex) {
                                throw new java.io.UncheckedIOException(ex);
                            }
                        });
            }
        }
    }

    private ItemAtlasRenderer() {}
}
