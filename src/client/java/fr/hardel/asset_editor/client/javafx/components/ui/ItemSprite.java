package fr.hardel.asset_editor.client.javafx.components.ui;

import fr.hardel.asset_editor.client.javafx.lib.ItemAtlasGenerator;
import fr.hardel.asset_editor.client.rendering.ItemAtlasRenderer.AtlasEntry;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import net.minecraft.resources.Identifier;

public final class ItemSprite extends ImageView {
    private final Identifier itemId;
    private Runnable subscription;

    public ItemSprite(Identifier itemId, double displaySize) {
        this.itemId = itemId;
        setFitWidth(displaySize);
        setFitHeight(displaySize);
        setPreserveRatio(true);
        setSmooth(false);

        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) {
                if (subscription != null) {
                    subscription.run();
                    subscription = null;
                }
                return;
            }

            if (subscription == null)
                subscription = ItemAtlasGenerator.subscribe(this::refresh);
            refresh();
        });

        refresh();
    }

    private void refresh() {
        WritableImage atlas = ItemAtlasGenerator.getAtlasImage();
        AtlasEntry entry = ItemAtlasGenerator.getEntry(itemId);
        if (atlas == null || entry == null) {
            setVisible(false);
            setManaged(false);
            setImage(null);
            return;
        }

        setVisible(true);
        setManaged(true);
        setImage(atlas);
        setViewport(new Rectangle2D(entry.x(), entry.y(), entry.size(), entry.size()));
    }
}
