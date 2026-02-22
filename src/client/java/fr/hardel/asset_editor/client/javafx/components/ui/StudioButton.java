package fr.hardel.asset_editor.client.javafx.components.ui;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.animation.Interpolator;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

/**
 * Styled button matching web Button component variants.
 * Shimmer and Patreon variants include animated shimmer highlight.
 */
public final class StudioButton extends StackPane {

    public enum Variant {
        DEFAULT, BLACK, GHOST, GHOST_BORDER, AURORA, TRANSPARENT, LINK, SHIMMER, PATREON
    }

    public enum Size {
        NONE, SQUARE, DEFAULT, SM, LG, XL, ICON
    }

    private static final Color SHIMMER_BASE = Color.web("#f5f5f5");
    private static final Color PATREON_BASE = Color.web("#c2410c");

    private final HBox content = new HBox();
    private final Region background = new Region();
    private Runnable onClick;

    public StudioButton(Variant variant, Size size, String text, Node... icons) {
        getStyleClass().add("studio-button");
        setCursor(Cursor.HAND);

        content.setAlignment(Pos.CENTER);
        content.setSpacing(8);
        if (text != null && !text.isEmpty()) {
            javafx.scene.control.Label label = new javafx.scene.control.Label(text);
            label.getStyleClass().add("studio-button-label");
            // Shimmer uses dark text; patreon uses white — set programmatically since the
            // variant style class is on a sibling Region, not an ancestor of the label.
            if (variant == Variant.SHIMMER) label.setTextFill(Color.web("#080507"));
            else label.setTextFill(Color.WHITE);
            content.getChildren().add(label);
        }
        for (Node icon : icons) {
            content.getChildren().add(0, icon);
        }

        applySize(size);
        applyVariantStyle(variant);

        if (variant == Variant.SHIMMER || variant == Variant.PATREON) {
            setupShimmer(variant);
        } else {
            getChildren().addAll(background, content);
        }

        setOnMouseClicked(e -> { if (onClick != null) onClick.run(); });
        setupHoverEffects(variant);
    }

    public StudioButton(Variant variant, String text) {
        this(variant, Size.DEFAULT, text);
    }

    public void setOnAction(Runnable handler) {
        this.onClick = handler;
    }

    private void applySize(Size size) {
        double height = switch (size) {
            case SM -> 36;
            case LG -> 40;
            case XL -> 48;
            case ICON, SQUARE -> 40;
            default -> 40;
        };
        Insets padding = switch (size) {
            case SM -> new Insets(0, 12, 0, 12);
            case LG -> new Insets(0, 32, 0, 32);
            case XL -> new Insets(0, 40, 0, 40);
            case ICON -> new Insets(8);
            case SQUARE -> new Insets(8);
            default -> new Insets(0, 16, 0, 16);
        };
        if (size == Size.ICON) {
            setPrefSize(height, height);
            setMinSize(height, height);
        } else {
            setPrefHeight(height);
            setMinHeight(height);
        }
        content.setPadding(padding);
    }

    private void applyVariantStyle(Variant variant) {
        String styleClass = switch (variant) {
            case DEFAULT -> "studio-button-default";
            case BLACK -> "studio-button-black";
            case GHOST -> "studio-button-ghost";
            case GHOST_BORDER -> "studio-button-ghost-border";
            case AURORA -> "studio-button-aurora";
            case TRANSPARENT -> "studio-button-transparent";
            case LINK -> "studio-button-link";
            case SHIMMER -> "studio-button-shimmer";
            case PATREON -> "studio-button-patreon";
        };
        // Apply to background Region (for non-shimmer/patreon) AND to the outer StackPane
        // (so CSS descendant selectors like .studio-button-shimmer .studio-button-label work).
        background.getStyleClass().add(styleClass);
        background.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        if (variant == Variant.SHIMMER || variant == Variant.PATREON) {
            getStyleClass().add(styleClass);
        }

        Rectangle clip = new Rectangle();
        clip.setArcWidth(24);
        clip.setArcHeight(24);
        widthProperty().addListener((obs, o, w) -> clip.setWidth(w.doubleValue()));
        heightProperty().addListener((obs, o, h) -> clip.setHeight(h.doubleValue()));
        setClip(clip);
    }

    private void setupShimmer(Variant variant) {
        Color base = variant == Variant.PATREON ? PATREON_BASE : SHIMMER_BASE;

        Region baseLayer = new Region();
        baseLayer.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        baseLayer.setBackground(new javafx.scene.layout.Background(
            new javafx.scene.layout.BackgroundFill(base, new javafx.scene.layout.CornerRadii(12), Insets.EMPTY)));

        Region stripe = new Region();
        stripe.setMouseTransparent(true);
        stripe.prefHeightProperty().bind(heightProperty());
        stripe.setStyle("-fx-background-color: linear-gradient(from 0% 0% to 100% 0%, transparent, rgba(255,255,255,0.45), transparent);");

        getChildren().addAll(baseLayer, stripe, content);

        stripe.prefWidthProperty().bind(widthProperty().multiply(0.4));

        // Start the animation once (first non-zero width). Starting far left (-1.5x width)
        // ensures a smooth entry instead of snapping into view.
        Timeline[] animHolder = {null};
        widthProperty().addListener((obs, o, w) -> {
            if (w.doubleValue() <= 0 || animHolder[0] != null) return;
            double bw = w.doubleValue();
            Timeline anim = new Timeline(
                new KeyFrame(Duration.ZERO,
                    new KeyValue(stripe.translateXProperty(), -bw * 1.5)),
                new KeyFrame(Duration.seconds(2.5),
                    new KeyValue(stripe.translateXProperty(), bw * 1.5))
            );
            anim.setCycleCount(Timeline.INDEFINITE);
            anim.play();
            animHolder[0] = anim;
        });
    }

    private void setupHoverEffects(Variant variant) {
        if (variant == Variant.SHIMMER) {
            setOnMouseEntered(e -> animateOpacity(0.75));
            setOnMouseExited(e -> animateOpacity(1.0));
        } else if (variant == Variant.PATREON) {
            setOnMouseEntered(e -> animateScale(0.95));
            setOnMouseExited(e -> animateScale(1.0));
        }
    }

    private void animateOpacity(double target) {
        Timeline timeline = new Timeline(
            new KeyFrame(Duration.millis(150),
                new KeyValue(opacityProperty(), target, Interpolator.EASE_BOTH))
        );
        timeline.play();
    }

    private void animateScale(double target) {
        Timeline timeline = new Timeline(
            new KeyFrame(Duration.millis(150),
                new KeyValue(scaleXProperty(), target, Interpolator.EASE_BOTH),
                new KeyValue(scaleYProperty(), target, Interpolator.EASE_BOTH))
        );
        timeline.play();
    }
}
