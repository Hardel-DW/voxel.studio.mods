package fr.hardel.asset_editor.client.rendering;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.textures.GpuTexture;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

/**
 * Asynchronous read-back of an RGBA8 GPU texture into an ARGB {@code int[]} array.
 * The source bytes are packed as {@code 0xAARRGGBB}; {@code flipY} reverses the row
 * order so the result matches the top-left origin expected by Skia / Compose images.
 */
public final class GpuReadback {

    public static void readArgb(
        GpuDevice device,
        GpuTexture texture,
        String label,
        int width,
        int height,
        boolean flipY,
        Runnable onClose,
        Consumer<int[]> callback
    ) {
        int bufferSize = width * height * 4;
        GpuBuffer readbackBuffer = device.createBuffer(
            () -> label,
            GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_MAP_READ,
            bufferSize
        );
        device.createCommandEncoder().copyTextureToBuffer(texture, readbackBuffer, 0, () -> {
            try (GpuBuffer.MappedView view = device.createCommandEncoder().mapBuffer(readbackBuffer, true, false)) {
                callback.accept(packArgb(view.data(), width, height, flipY));
            } finally {
                readbackBuffer.close();
                if (onClose != null) onClose.run();
            }
        }, 0);
    }

    private static int[] packArgb(ByteBuffer pixels, int width, int height, boolean flipY) {
        int[] argb = new int[width * height];
        for (int srcY = 0; srcY < height; srcY++) {
            int dstY = flipY ? height - 1 - srcY : srcY;
            int srcRow = srcY * width;
            int dstRow = dstY * width;
            for (int x = 0; x < width; x++) {
                int offset = (srcRow + x) * 4;
                int r = pixels.get(offset) & 0xFF;
                int g = pixels.get(offset + 1) & 0xFF;
                int b = pixels.get(offset + 2) & 0xFF;
                int a = pixels.get(offset + 3) & 0xFF;
                argb[dstRow + x] = (a << 24) | (r << 16) | (g << 8) | b;
            }
        }
        return argb;
    }

    private GpuReadback() {}
}
