package fr.hardel.asset_editor.client.javafx.components.ui;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

/**
 * Animated counter: w-20 h-10, hover:w-32 (500ms ease-in-out).
 * Border: white/20% → white on hover. Chevron arrows appear on hover.
 * Click value to edit inline.
 */
public final class Counter extends StackPane {

    private static final double WIDTH_DEFAULT = 80;
    private static final double WIDTH_HOVER = 128;
    private static final double HEIGHT = 40;
    private static final String BORDER_NORMAL = "-fx-border-color: rgba(255,255,255,0.2); -fx-border-width: 1.5; -fx-border-radius: 24; -fx-background-color: transparent; -fx-background-radius: 24;";
    private static final String BORDER_HOVER  = "-fx-border-color: white; -fx-border-width: 1.5; -fx-border-radius: 24; -fx-background-color: transparent; -fx-background-radius: 24;";

    private final IntegerProperty value = new SimpleIntegerProperty();
    private final int min, max, step;

    private final Region border = new Region();
    private final Label valueLabel = new Label();
    private final TextField valueField = new TextField();
    private final StackPane leftArrow;
    private final StackPane rightArrow;
    private final StackPane visualRoot = new StackPane();

    private final Timeline expandAnim;
    private final Timeline collapseAnim;

    public Counter(int min, int max, int step, int initialValue) {
        this.min = min;
        this.max = max;
        this.step = step;
        this.value.set(initialValue);

        setPrefSize(WIDTH_DEFAULT, HEIGHT);
        setMinSize(WIDTH_DEFAULT, HEIGHT);
        setMaxSize(WIDTH_HOVER, HEIGHT);

        border.setStyle(BORDER_NORMAL);
        border.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        border.setMouseTransparent(true);

        valueLabel.setText(String.valueOf(initialValue));
        valueLabel.setTextFill(Color.WHITE);
        valueLabel.setFont(Font.font("Rubik", FontWeight.BOLD, 20));
        valueLabel.setCursor(javafx.scene.Cursor.TEXT);

        valueField.setText(String.valueOf(initialValue));
        valueField.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 20; -fx-font-weight: bold; -fx-alignment: center; -fx-border-color: transparent;");
        valueField.setAlignment(Pos.CENTER);
        valueField.setMaxWidth(60);
        valueField.setVisible(false);
        valueField.setManaged(false);

        leftArrow = buildArrowPane(false);
        rightArrow = buildArrowPane(true);
        leftArrow.setOpacity(0);
        rightArrow.setOpacity(0);
        StackPane.setAlignment(leftArrow, Pos.CENTER_LEFT);
        StackPane.setAlignment(rightArrow, Pos.CENTER_RIGHT);
        StackPane.setMargin(leftArrow, new Insets(0, 0, 0, 16));
        StackPane.setMargin(rightArrow, new Insets(0, 16, 0, 0));

        StackPane valueContainer = new StackPane(valueLabel, valueField);
        valueContainer.setAlignment(Pos.CENTER);
        valueContainer.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        visualRoot.setAlignment(Pos.CENTER);
        visualRoot.setPrefSize(WIDTH_DEFAULT, HEIGHT);
        visualRoot.setMinSize(WIDTH_DEFAULT, HEIGHT);
        visualRoot.setMaxSize(WIDTH_HOVER, HEIGHT);
        visualRoot.getChildren().addAll(border, valueContainer, leftArrow, rightArrow);
        getChildren().add(visualRoot);
        setAlignment(Pos.CENTER_RIGHT);

        Rectangle clip = new Rectangle(WIDTH_DEFAULT, HEIGHT);
        clip.setArcWidth(48);
        clip.setArcHeight(48);
        visualRoot.prefWidthProperty().addListener((obs, o, w) -> clip.setWidth(w.doubleValue()));
        clip.heightProperty().bind(visualRoot.heightProperty());
        setClip(clip);

        Interpolator easeInOut = Interpolator.EASE_BOTH;
        expandAnim = new Timeline(
            new KeyFrame(Duration.millis(500),
                new KeyValue(visualRoot.prefWidthProperty(), WIDTH_HOVER, easeInOut),
                new KeyValue(prefWidthProperty(), WIDTH_HOVER, easeInOut))
        );
        collapseAnim = new Timeline(
            new KeyFrame(Duration.millis(500),
                new KeyValue(visualRoot.prefWidthProperty(), WIDTH_DEFAULT, easeInOut),
                new KeyValue(prefWidthProperty(), WIDTH_DEFAULT, easeInOut))
        );

        setupInteractions();
        this.value.addListener((obs, o, v) -> valueLabel.setText(String.valueOf(v.intValue())));
    }

    private void setupInteractions() {
        setOnMouseEntered(e -> {
            border.setStyle(BORDER_HOVER);
            collapseAnim.stop();
            expandAnim.playFromStart();
            fadeArrows(true);
        });

        setOnMouseExited(e -> {
            border.setStyle(BORDER_NORMAL);
            expandAnim.stop();
            collapseAnim.playFromStart();
            fadeArrows(false);
        });

        valueLabel.setOnMouseClicked(e -> startEditing());
        valueField.setOnAction(e -> commitEdit());
        valueField.focusedProperty().addListener((obs, o, focused) -> { if (!focused) commitEdit(); });
        valueField.textProperty().addListener((obs, o, text) -> {
            if (!text.matches("[0-9]*")) valueField.setText(text.replaceAll("[^0-9]", ""));
        });

        leftArrow.setOnMouseClicked(e -> { e.consume(); decrease(); });
        rightArrow.setOnMouseClicked(e -> { e.consume(); increase(); });
    }

    private void fadeArrows(boolean visible) {
        FadeTransition left = new FadeTransition(Duration.millis(500), leftArrow);
        left.setToValue(visible ? 1.0 : 0.0);
        left.play();
        FadeTransition right = new FadeTransition(Duration.millis(500), rightArrow);
        right.setToValue(visible ? 1.0 : 0.0);
        right.play();
        Timeline slide = new Timeline(
            new KeyFrame(Duration.millis(500),
                new KeyValue(leftArrow.translateXProperty(), visible ? 4 : 0, Interpolator.EASE_BOTH),
                new KeyValue(rightArrow.translateXProperty(), visible ? -4 : 0, Interpolator.EASE_BOTH))
        );
        slide.play();
    }

    private StackPane buildArrowPane(boolean isRight) {
        Polyline arrow = isRight
            ? new Polyline(0, 0, 6, 6, 0, 12)
            : new Polyline(6, 0, 0, 6, 6, 12);
        arrow.setFill(Color.TRANSPARENT);
        arrow.setStroke(Color.WHITE);
        arrow.setStrokeWidth(2.0);
        arrow.setStrokeLineCap(StrokeLineCap.ROUND);
        arrow.setStrokeLineJoin(StrokeLineJoin.ROUND);
        arrow.setCursor(javafx.scene.Cursor.HAND);

        StackPane pane = new StackPane(arrow);
        pane.setPrefSize(12, 12);
        pane.setMaxSize(12, 12);
        pane.setCursor(javafx.scene.Cursor.HAND);
        return pane;
    }

    private void startEditing() {
        valueLabel.setVisible(false);
        valueLabel.setManaged(false);
        valueField.setText(String.valueOf(value.get()));
        valueField.setVisible(true);
        valueField.setManaged(true);
        Platform.runLater(() -> { valueField.requestFocus(); valueField.selectAll(); });
    }

    private void commitEdit() {
        String text = valueField.getText().trim();
        if (!text.isEmpty()) {
            try {
                value.set(Math.min(max, Math.max(min, Integer.parseInt(text))));
            } catch (NumberFormatException ignored) {}
        }
        valueField.setVisible(false);
        valueField.setManaged(false);
        valueLabel.setVisible(true);
        valueLabel.setManaged(true);
    }

    private void increase() { value.set(Math.min(max, value.get() + step)); }
    private void decrease() { value.set(Math.max(min, value.get() - step)); }

    public int getValue() { return value.get(); }
    public IntegerProperty valueProperty() { return value; }
}
