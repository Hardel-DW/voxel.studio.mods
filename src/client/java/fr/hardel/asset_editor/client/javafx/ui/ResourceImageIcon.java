package fr.hardel.asset_editor.client.javafx.ui;

import fr.hardel.asset_editor.client.javafx.ResourceLoader;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import net.minecraft.resources.Identifier;

public final class ResourceImageIcon extends ImageView {

    public ResourceImageIcon(Identifier location, double size) {
        setFitWidth(size);
        setFitHeight(size);
        setPreserveRatio(true);
        try (var stream = ResourceLoader.open(location)) {
            setImage(new Image(stream));
        } catch (Exception ignored) {
            setVisible(false);
            setManaged(false);
        }
    }
}
