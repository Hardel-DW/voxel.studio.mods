package fr.hardel.asset_editor.client.rendering;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.CachedOrthoProjectionMatrixBuffer;
import org.joml.Matrix4fStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.function.Consumer;

import fr.hardel.asset_editor.client.rendering.StructureSceneTessellator.AnimatingMesh;
import fr.hardel.asset_editor.client.rendering.StructureSceneTessellator.CompiledLayer;
import fr.hardel.asset_editor.client.rendering.StructureSceneTessellator.StaticMesh;

/** Off-screen GPU draw of structure meshes into an RGBA8 texture, then asynchronous read-back to ARGB pixels. */
final class StructureSceneFrameDrawer {

    private static final int MAX_WIDTH = 4096;
    private static final int MAX_HEIGHT = 4096;
    private static final float NEAR_PLANE = -32768.0F;
    private static final float FAR_PLANE = 32768.0F;

    static void drawAndReadback(
        StructureSceneRenderer.Request request,
        StaticMesh staticMesh,
        AnimatingMesh animatingMesh,
        Consumer<StructureSceneRenderer.Result> onResult
    ) {
        int width = Math.min(request.width(), MAX_WIDTH);
        int height = Math.min(request.height(), MAX_HEIGHT);

        GpuDevice device = RenderSystem.getDevice();
        GpuTexture colorTexture = device.createTexture("StructureScene/Color", 12 | GpuTexture.USAGE_COPY_SRC, TextureFormat.RGBA8, width, height, 1, 1);
        GpuTexture depthTexture = device.createTexture("StructureScene/Depth", 8, TextureFormat.DEPTH32, width, height, 1, 1);
        GpuTextureView colorView = device.createTextureView(colorTexture);
        GpuTextureView depthView = device.createTextureView(depthTexture);

        device.createCommandEncoder().clearColorAndDepthTextures(colorTexture, 0, depthTexture, 1.0);

        RenderSystem.backupProjectionMatrix();
        var savedColorOverride = RenderSystem.outputColorTextureOverride;
        var savedDepthOverride = RenderSystem.outputDepthTextureOverride;
        RenderSystem.outputColorTextureOverride = colorView;
        RenderSystem.outputDepthTextureOverride = depthView;

        CachedOrthoProjectionMatrixBuffer projection = new CachedOrthoProjectionMatrixBuffer("structureScene", NEAR_PLANE, FAR_PLANE, true);
        RenderSystem.setProjectionMatrix(projection.getBuffer(width, height), ProjectionType.ORTHOGRAPHIC);
        RenderSystem.enableScissorForRenderTypeDraws(0, 0, width, height);

        Minecraft.getInstance().gameRenderer.getLighting().setupFor(Lighting.Entry.LEVEL);
        drawScene(staticMesh, animatingMesh, request, width, height);

        RenderSystem.disableScissorForRenderTypeDraws();
        RenderSystem.outputColorTextureOverride = savedColorOverride;
        RenderSystem.outputDepthTextureOverride = savedDepthOverride;
        RenderSystem.restoreProjectionMatrix();
        projection.close();
        depthView.close();
        depthTexture.close();

        GpuReadback.readArgb(device, colorTexture, "StructureScene/Readback", width, height, true,
            () -> { colorView.close(); colorTexture.close(); },
            argb -> onResult.accept(new StructureSceneRenderer.Result(request.key(), width, height, argb)));
    }

    private static void drawScene(StaticMesh staticMesh, AnimatingMesh animatingMesh, StructureSceneRenderer.Request request, int width, int height) {
        if (staticMesh.stackByY.isEmpty() && staticMesh.exposeByY.isEmpty() && animatingMesh.layers.isEmpty()) return;

        Matrix4fStack mvm = RenderSystem.getModelViewStack();
        mvm.pushMatrix();
        applyCameraToMvm(mvm, request, width, height);
        try {
            drawStaticSlice(staticMesh, request.sliceY());
            drawAnimatingPiece(animatingMesh, mvm, request.pieceOffset());
        } finally {
            mvm.popMatrix();
        }
    }

    private static void drawStaticSlice(StaticMesh staticMesh, int sliceY) {
        if (sliceY < 0) return;
        for (int y = 0; y < sliceY; y++) drawAll(staticMesh.stackByY.get(y));
        drawAll(staticMesh.exposeByY.get(sliceY));
    }

    private static void drawAnimatingPiece(AnimatingMesh animatingMesh, Matrix4fStack mvm, float offset) {
        if (animatingMesh.layers.isEmpty()) return;
        if (offset == 0f) {
            drawAll(animatingMesh.layers);
            return;
        }
        mvm.pushMatrix();
        mvm.translate(0f, offset, 0f);
        try {
            drawAll(animatingMesh.layers);
        } finally {
            mvm.popMatrix();
        }
    }

    private static void drawAll(List<CompiledLayer> layers) {
        if (layers == null) return;
        for (CompiledLayer layer : layers) drawLayer(layer);
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
            layer.renderType.draw(new MeshData(result, layer.drawState));
        } finally {
            builder.close();
        }
    }

    private static void applyCameraToMvm(Matrix4fStack mvm, StructureSceneRenderer.Request request, int width, int height) {
        StructureSceneRenderer.Camera camera = request.camera();
        mvm.translate(width * 0.5F + camera.panX(), height * 0.5F + camera.panY(), 0.0F);
        mvm.scale(camera.zoom(), -camera.zoom(), camera.zoom());
        mvm.rotateX((float) Math.toRadians(camera.pitch()));
        mvm.rotateY((float) Math.toRadians(camera.yaw()));
        mvm.translate(-request.sizeX() * 0.5F, -request.sizeY() * 0.5F, -request.sizeZ() * 0.5F);
    }

    private StructureSceneFrameDrawer() {}
}
