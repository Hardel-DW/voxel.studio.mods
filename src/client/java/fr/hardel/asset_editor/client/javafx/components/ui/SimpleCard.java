package fr.hardel.asset_editor.client.javafx.components.ui;

import fr.hardel.asset_editor.client.javafx.ResourceLoader;
import javafx.animation.TranslateTransition;
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
 * Shine image (top half, brightness-15 ≈ opacity 0.15) absolutely behind content.
 * hover:-translate-y-1 (4px, 150ms ease-out).
 *
 * The outer StackPane is the fixed hit-area. Only the inner visualCard translates,
 * so the hover zone never shifts (fixes the CSS-translate vs JavaFX-pickBounds mismatch).
 */
public class SimpleCard extends StackPane {

    private static final Identifier SHINE = Identifier.fromNamespaceAndPath("asset_editor", "textures/studio/shine.png");

    protected final VBox contentBox = new VBox();

    protected SimpleCard(Insets padding) {
        setCursor(javafx.scene.Cursor.HAND);

        // Visual card: has border/background styling, translates on hover.
        // The outer StackPane (this) acts as the stable hit area since it never translates.
        // JavaFX picks on layout bounds (not transformed), so the outer StackPane's hover
        // zone stays fixed even when visualCard shifts up.
        StackPane visualCard = new StackPane();
        visualCard.getStyleClass().add("simple-card");
        visualCard.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        contentBox.setPadding(padding);
        contentBox.setMaxWidth(Double.MAX_VALUE);

        Pane shinePane = new Pane();
        shinePane.setMouseTransparent(true);
        shinePane.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        try (var stream = ResourceLoader.open(SHINE)) {
            ImageView shine = new ImageView(new Image(stream));
            shine.setPreserveRatio(false);
            shine.setOpacity(0.15);
            shine.fitWidthProperty().bind(visualCard.widthProperty());
            shine.fitHeightProperty().bind(visualCard.heightProperty().multiply(0.5));
            shinePane.getChildren().add(shine);
        } catch (Exception ignored) {}

        visualCard.getChildren().addAll(shinePane, contentBox);
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
}
