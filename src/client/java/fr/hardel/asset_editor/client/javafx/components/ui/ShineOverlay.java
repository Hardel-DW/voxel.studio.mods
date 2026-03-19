package fr.hardel.asset_editor.client.javafx.components.ui;

import fr.hardel.asset_editor.client.javafx.VoxelResourceLoader;
import javafx.beans.value.ObservableValue;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ShineOverlay extends ImageView {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShineOverlay.class);
    private static final Identifier SHINE = Identifier.fromNamespaceAndPath("asset_editor", "textures/shine.png");
    private static volatile Image cachedImage;

    public ShineOverlay(ObservableValue<? extends Number> widthSource, ObservableValue<? extends Number> heightSource) {
        Image image = loadImage();
        if (image == null)
            return;

        setImage(image);
        setPreserveRatio(false);
        setManaged(false);
        setMouseTransparent(true);
        fitWidthProperty().bind(widthSource);
        fitHeightProperty().bind(heightSource);
    }

    public ShineOverlay withOpacity(double opacity) {
        setOpacity(opacity);
        return this;
    }

    public ShineOverlay withHue(double hue) {
        colorAdjust().setHue(hue);
        return this;
    }

    public ShineOverlay withBrightness(double brightness) {
        colorAdjust().setBrightness(brightness);
        return this;
    }

    private ColorAdjust colorAdjust() {
        if (getEffect() instanceof ColorAdjust ca)
            return ca;
        var ca = new ColorAdjust();
        setEffect(ca);
        return ca;
    }

    public boolean isLoaded() {
        return getImage() != null;
    }

    private static Image loadImage() {
        Image current = cachedImage;
        if (current != null)
            return current;

        synchronized (ShineOverlay.class) {
            if (cachedImage != null)
                return cachedImage;

            try (var stream = VoxelResourceLoader.open(SHINE)) {
                cachedImage = new Image(stream);
            } catch (Exception e) {
                LOGGER.warn("Failed to load shine texture: {}", e.getMessage());
            }
            return cachedImage;
        }
    }
}
