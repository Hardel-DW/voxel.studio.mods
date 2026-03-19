package fr.hardel.asset_editor.client.javafx.components.ui;

import javafx.animation.TranslateTransition;
import javafx.scene.Cursor;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

public class SimpleCard extends StackPane {

    protected final StackPane visualCard = new StackPane();
    protected final VBox contentBox = new VBox();

    protected SimpleCard(Insets padding) {
        setCursor(Cursor.HAND);
        visualCard.getStyleClass().add("ui-simple-card");
        visualCard.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        contentBox.setPadding(padding);
        contentBox.setMaxWidth(Double.MAX_VALUE);
        visualCard.getChildren().add(contentBox);

        var shine = new ShineOverlay(visualCard.widthProperty(), visualCard.heightProperty().multiply(0.5))
            .withOpacity(0.15);
        if (shine.isLoaded())
            visualCard.getChildren().addFirst(shine);

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
