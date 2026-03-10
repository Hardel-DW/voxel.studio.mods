package fr.hardel.asset_editor.client.javafx.lib;

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
import javafx.application.Platform;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.CachedOrthoProjectionMatrixBuffer;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.TrackingItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ItemAtlasGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger(ItemAtlasGenerator.class);
    private static final int ITEM_SIZE = 32;

    private static final AtomicBoolean needsGeneration = new AtomicBoolean(false);
    private static volatile WritableImage atlasImage;
    private static volatile Map<Identifier, AtlasEntry> entries = Map.of();

    public record AtlasEntry(int x, int y, int size) {}

    public static WritableImage getAtlasImage() {
        return atlasImage;
    }

    public static AtlasEntry getEntry(Identifier itemId) {
        return entries.get(itemId);
    }

    public static boolean isReady() {
        return atlasImage != null;
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
        int atlasWidth = gridCols * ITEM_SIZE;
        int atlasHeight = gridRows * ITEM_SIZE;

        SubmitNodeStorage submitStorage = new SubmitNodeStorage();
        ByteBufferBuilder byteBuffer = new ByteBufferBuilder(786432);
        MultiBufferSource.BufferSource bufferSource = MultiBufferSource.immediate(byteBuffer);
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
        GpuTexture colorTexture = device.createTexture("ItemAtlas/Color", 12, TextureFormat.RGBA8, atlasWidth, atlasHeight, 1, 1);
        GpuTexture depthTexture = device.createTexture("ItemAtlas/Depth", 8, TextureFormat.DEPTH32, atlasWidth, atlasHeight, 1, 1);
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
        RenderSystem.setProjectionMatrix(projBuffer.getBuffer(atlasWidth, atlasHeight), ProjectionType.ORTHOGRAPHIC);

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
            renderSingleItem(renderState, poseStack, renderX, renderY, atlasHeight, featureDispatcher, bufferSource, submitStorage);
        }

        RenderSystem.outputColorTextureOverride = savedColorOverride;
        RenderSystem.outputDepthTextureOverride = savedDepthOverride;
        RenderSystem.restoreProjectionMatrix();
        projBuffer.close();

        depthView.close();
        depthTexture.close();

        readbackAtlas(device, colorTexture, colorView, atlasWidth, atlasHeight, newEntries);
    }

    private static void renderSingleItem(
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

    private static void readbackAtlas(GpuDevice device, GpuTexture colorTexture, GpuTextureView colorView, int width, int height, Map<Identifier, AtlasEntry> newEntries) {
        int bufferSize = width * height * 4;
        GpuBuffer readbackBuffer = device.createBuffer(() -> "ItemAtlas/Readback", GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_MAP_READ, bufferSize);

        device.createCommandEncoder().copyTextureToBuffer(colorTexture, readbackBuffer, 0, () -> {
            try (GpuBuffer.MappedView view = device.createCommandEncoder().mapBuffer(readbackBuffer, true, false)) {
                ByteBuffer pixels = view.data();
                int[] argbPixels = new int[width * height];

                for (int i = 0; i < argbPixels.length; i++) {
                    int offset = i * 4;
                    int r = pixels.get(offset) & 0xFF;
                    int g = pixels.get(offset + 1) & 0xFF;
                    int b = pixels.get(offset + 2) & 0xFF;
                    int a = pixels.get(offset + 3) & 0xFF;
                    argbPixels[i] = (a << 24) | (r << 16) | (g << 8) | b;
                }

                Platform.runLater(() -> {
                    WritableImage image = new WritableImage(width, height);
                    image.getPixelWriter().setPixels(0, 0, width, height, PixelFormat.getIntArgbInstance(), argbPixels, 0, width);
                    entries = newEntries;
                    atlasImage = image;
                    LOGGER.info("Item atlas generated: {}x{} with {} items", width, height, newEntries.size());
                });
            } finally {
                readbackBuffer.close();
                colorView.close();
                colorTexture.close();
            }
        }, 0);
    }

    private ItemAtlasGenerator() {}
}
