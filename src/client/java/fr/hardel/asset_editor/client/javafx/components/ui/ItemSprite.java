package fr.hardel.asset_editor.client.javafx.components.ui;

import fr.hardel.asset_editor.client.javafx.lib.ItemAtlasGenerator;
import fr.hardel.asset_editor.client.javafx.lib.ItemAtlasGenerator.AtlasEntry;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import net.minecraft.resources.Identifier;

public final class ItemSprite extends ImageView {

    public ItemSprite(Identifier itemId, double displaySize) {
        setFitWidth(displaySize);
        setFitHeight(displaySize);
        setPreserveRatio(true);
        setSmooth(false);

        WritableImage atlas = ItemAtlasGenerator.getAtlasImage();
        AtlasEntry entry = ItemAtlasGenerator.getEntry(itemId);
        if (atlas == null || entry == null) {
            setVisible(false);
            setManaged(false);
            return;
        }

        setImage(atlas);
        setViewport(new Rectangle2D(entry.x(), entry.y(), entry.size(), entry.size()));
    }
}
