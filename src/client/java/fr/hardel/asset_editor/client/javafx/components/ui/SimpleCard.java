package fr.hardel.asset_editor.client.javafx.components.ui;

import fr.hardel.asset_editor.client.javafx.VoxelResourceLoader;
import javafx.animation.TranslateTransition;
import javafx.scene.Cursor;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import net.minecraft.resources.Identifier;

/**
 * bg-black/35 border border-zinc-900 rounded-xl py-6 px-8
 * Shine image (top half, brightness-15 ≈ opacity 0.15) absolutely behind
 * content.
 * hover:-translate-y-1 (4px, 150ms ease-out).
 *
 * The outer StackPane is the fixed hit-area. Only the inner visualCard
 * translates,
 * so the hover zone never shifts (fixes the CSS-translate vs JavaFX-pickBounds
 * mismatch).
 */
public class SimpleCard extends StackPane {

    private static final Identifier SHINE = Identifier.fromNamespaceAndPath("asset_editor",
            "textures/studio/shine.png");
    private static volatile Image shineImage;

    protected final StackPane visualCard = new StackPane();
    protected final VBox contentBox = new VBox();

    protected SimpleCard(Insets padding) {
        setCursor(Cursor.HAND);
        visualCard.getStyleClass().add("ui-simple-card");
        visualCard.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        contentBox.setPadding(padding);
        contentBox.setMaxWidth(Double.MAX_VALUE);

        Pane shinePane = new Pane();
        shinePane.setMouseTransparent(true);
        shinePane.setManaged(false);
        shinePane.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        Image sharedShine = getShineImage();
        if (sharedShine != null) {
            ImageView shine = new ImageView(sharedShine);
            shine.setPreserveRatio(false);
            shine.setOpacity(0.15);
            shine.fitWidthProperty().bind(visualCard.widthProperty());
            shine.fitHeightProperty().bind(visualCard.heightProperty().multiply(0.5));
            shinePane.getChildren().add(shine);
        }

        visualCard.getChildren().addAll(shinePane, contentBox);
        StackPane.setAlignment(shinePane, Pos.TOP_LEFT);
        StackPane.setAlignment(contentBox, Pos.TOP_LEFT);

        Rectangle clip = new Rectangle();
        clip.setArcWidth(24);
        clip.setArcHeight(24);
        visualCard.widthProperty().addListener((obs, o, w) -> clip.setWidth(w.doubleValue()));
        visualCard.heightProperty().addListener((obs, o, h) -> clip.setHeight(h.doubleValue()));
        visualCard.setClip(clip);

        getChildren().add(visualCard);

        // Translate only the visual layer — outer StackPane bounds stay fixed
        TranslateTransition hoverIn = new TranslateTransition(Duration.millis(150), visualCard);
        hoverIn.setToY(-4);
        TranslateTransition hoverOut = new TranslateTransition(Duration.millis(150), visualCard);
        hoverOut.setToY(0);
        setOnMouseEntered(e -> hoverIn.playFromStart());
        setOnMouseExited(e -> hoverOut.playFromStart());
    }

    public SimpleCard() {
        this(new Insets(24, 32, 24, 32)); // py-6 px-8
    }

    private static Image getShineImage() {
        Image current = shineImage;
        if (current != null) {
            return current;
        }
        synchronized (SimpleCard.class) {
            if (shineImage != null) {
                return shineImage;
            }
            try (var stream = VoxelResourceLoader.open(SHINE)) {
                shineImage = new Image(stream);
            } catch (Exception ignored) {
                shineImage = null;
            }
            return shineImage;
        }
    }
}
