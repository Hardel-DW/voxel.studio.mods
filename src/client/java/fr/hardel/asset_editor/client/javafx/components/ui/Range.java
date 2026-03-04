package fr.hardel.asset_editor.client.javafx.components.ui;

import fr.hardel.asset_editor.client.javafx.VoxelColors;
import fr.hardel.asset_editor.client.javafx.VoxelFonts;
import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import net.minecraft.client.resources.language.I18n;

/**
 * Label row: label left, value right.
 * Styled range slider (h-6 track rounded, 24px thumb).
 */
public final class Range extends VBox {

    private final IntegerProperty value;

    public Range(String labelKey, int min, int max, int step, int initialValue) {
        this(labelKey, min, max, step, initialValue, false, null);
    }

    public Range(String labelKey, int min, int max, int step, int initialValue, boolean locked, String lockKey) {
        setSpacing(4);
        setMaxWidth(Double.MAX_VALUE);

        String labelText = locked && lockKey != null ? I18n.get(lockKey) : I18n.get(labelKey);

        Label labelNode = new Label(labelText);
        labelNode.setFont(VoxelFonts.rubik(VoxelFonts.Rubik.MEDIUM, 13));
        labelNode.setTextFill(VoxelColors.ZINC_400);

        Label valueLabel = new Label(String.valueOf(initialValue));
        valueLabel.setFont(VoxelFonts.rubik(VoxelFonts.Rubik.MEDIUM, 13));
        valueLabel.setTextFill(VoxelColors.ZINC_400);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(labelNode, spacer, valueLabel);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 2, 0));

        Slider slider = new Slider(min, max, initialValue);
        slider.setBlockIncrement(step);
        slider.setMajorTickUnit(Math.max(1, (max - min) / 10.0));
        slider.setShowTickMarks(false);
        slider.setShowTickLabels(false);
        slider.setDisable(locked);
        slider.getStyleClass().add("tool-range-slider");
        slider.setMaxWidth(Double.MAX_VALUE);

        value = new SimpleIntegerProperty(initialValue);
        slider.valueProperty().addListener((obs, o, v) -> {
            int rounded = (int) Math.round(v.doubleValue() / step) * step;
            value.set(rounded);
            valueLabel.setText(String.valueOf(rounded));
            updateTrackFill(slider);
        });
        slider.widthProperty().addListener((obs, o, v) -> updateTrackFill(slider));
        slider.skinProperty().addListener((obs, o, v) -> Platform.runLater(() -> updateTrackFill(slider)));
        Platform.runLater(() -> updateTrackFill(slider));

        if (locked) setOpacity(0.5);
        getChildren().addAll(header, slider);
    }

    private static void updateTrackFill(Slider slider) {
        Node track = slider.lookup(".track");
        if (track == null) {
            return;
        }
        double range = slider.getMax() - slider.getMin();
        double progress = range <= 0 ? 0 : (slider.getValue() - slider.getMin()) / range;
        progress = Math.max(0, Math.min(1, progress));
        int pct = (int) Math.round(progress * 100);
        track.setStyle("-fx-background-color: linear-gradient(to right, "
                + toCss(Color.WHITE) + " 0%, "
                + toCss(Color.WHITE) + " " + pct + "%, "
                + toCss(VoxelColors.ZINC_800) + " " + pct + "%, "
                + toCss(VoxelColors.ZINC_800) + " 100%);");
    }

    private static String toCss(Color color) {
        return "rgba(" + (int) Math.round(color.getRed() * 255) + ","
                + (int) Math.round(color.getGreen() * 255) + ","
                + (int) Math.round(color.getBlue() * 255) + ","
                + color.getOpacity() + ")";
    }

    public IntegerProperty valueProperty() { return value; }
    public int getValue() { return value.get(); }
}
