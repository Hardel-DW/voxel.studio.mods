package fr.hardel.asset_editor.client.javafx.lib;

import fr.hardel.asset_editor.client.rendering.ItemAtlasRenderer;
import fr.hardel.asset_editor.client.rendering.ItemAtlasRenderer.AtlasEntry;
import javafx.application.Platform;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import net.minecraft.resources.Identifier;

import java.util.concurrent.CopyOnWriteArrayList;

public final class ItemAtlasGenerator {
    private static final CopyOnWriteArrayList<Runnable> listeners = new CopyOnWriteArrayList<>();
    private static volatile WritableImage atlasImage;

    static {
        ItemAtlasRenderer.subscribeGeneration(ItemAtlasGenerator::rebuild);
    }

    public static WritableImage getAtlasImage() {
        if (atlasImage == null && ItemAtlasRenderer.isReady())
            rebuild();
        return atlasImage;
    }

    public static AtlasEntry getEntry(Identifier itemId) {
        return ItemAtlasRenderer.getEntry(itemId);
    }

    public static boolean isReady() {
        return ItemAtlasRenderer.isReady();
    }

    public static Runnable subscribe(Runnable listener) {
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    public static void rebuild() {
        int[] pixels = ItemAtlasRenderer.getArgbPixels();
        if (pixels == null)
            return;

        int w = ItemAtlasRenderer.getWidth();
        int h = ItemAtlasRenderer.getHeight();
        Runnable update = () -> {
            atlasImage = createImage(pixels, w, h);
            listeners.forEach(Runnable::run);
        };
        if (Platform.isFxApplicationThread()) update.run();
        else Platform.runLater(update);
    }

    private static WritableImage createImage(int[] argbPixels, int width, int height) {
        WritableImage image = new WritableImage(width, height);
        image.getPixelWriter().setPixels(0, 0, width, height, PixelFormat.getIntArgbInstance(), argbPixels, 0, width);
        return image;
    }

    private ItemAtlasGenerator() {}
}
