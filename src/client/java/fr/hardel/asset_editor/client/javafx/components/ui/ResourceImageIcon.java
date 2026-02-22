package fr.hardel.asset_editor.client.javafx.components.ui;

import fr.hardel.asset_editor.client.javafx.ResourceLoader;
import fr.hardel.asset_editor.client.javafx.WebAssetPath;
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

    public ResourceImageIcon(String webPath, double size) {
        setFitWidth(size);
        setFitHeight(size);
        setPreserveRatio(true);
        for (Identifier id : WebAssetPath.imageCandidates(webPath)) {
            try (var stream = ResourceLoader.open(id)) {
                setImage(new Image(stream));
                return;
            } catch (Exception ignored) {
            }
        }
        setVisible(false);
        setManaged(false);
    }
}

