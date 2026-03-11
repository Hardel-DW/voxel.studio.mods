package fr.hardel.asset_editor.client.javafx.lib;

import fr.hardel.asset_editor.client.rendering.ItemAtlasRenderer;
import fr.hardel.asset_editor.client.rendering.ItemAtlasRenderer.AtlasEntry;
import javafx.application.Platform;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import net.minecraft.resources.Identifier;

public final class ItemAtlasGenerator {
    private static volatile WritableImage atlasImage;
    private static long lastGeneration = -1;

    public static WritableImage getAtlasImage() {
        refreshIfNeeded();
        return atlasImage;
    }

    public static AtlasEntry getEntry(Identifier itemId) {
        return ItemAtlasRenderer.getEntry(itemId);
    }

    public static boolean isReady() {
        return ItemAtlasRenderer.isReady();
    }

    private static void refreshIfNeeded() {
        long current = ItemAtlasRenderer.getGeneration();
        if (current == lastGeneration) return;

        int[] pixels = ItemAtlasRenderer.getArgbPixels();
        if (pixels == null) return;

        int w = ItemAtlasRenderer.getWidth();
        int h = ItemAtlasRenderer.getHeight();

        if (Platform.isFxApplicationThread()) {
            atlasImage = createImage(pixels, w, h);
            lastGeneration = current;
        } else {
            Platform.runLater(() -> {
                atlasImage = createImage(pixels, w, h);
                lastGeneration = current;
            });
        }
    }

    private static WritableImage createImage(int[] argbPixels, int width, int height) {
        WritableImage image = new WritableImage(width, height);
        image.getPixelWriter().setPixels(0, 0, width, height, PixelFormat.getIntArgbInstance(), argbPixels, 0, width);
        return image;
    }

    private ItemAtlasGenerator() {}
}
