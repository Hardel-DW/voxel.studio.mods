package fr.hardel.asset_editor.client.javafx.components.ui;

import fr.hardel.asset_editor.client.javafx.VoxelFonts;
import fr.hardel.asset_editor.client.javafx.lib.utils.ColorUtils;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;

public final class SelectableTextBlock extends TextArea {

    public SelectableTextBlock(String text, VoxelFonts.Variant fontVariant, double fontSize, Color color) {
        setEditable(false);
        setWrapText(true);
        setFocusTraversable(true);
        setMaxWidth(Double.MAX_VALUE);
        setMinHeight(Region.USE_PREF_SIZE);
        setPrefHeight(Region.USE_COMPUTED_SIZE);
        setFont(VoxelFonts.of(fontVariant, fontSize));
        setTextFill(color);
        textProperty().addListener((obs, oldText, newText) -> setPrefRowCount(preferredRows(newText)));
        setText(text);
    }

    public void setTextFill(Color color) {
        setStyle("-fx-control-inner-background: transparent;"
            + "-fx-background-color: transparent;"
            + "-fx-background-insets: 0;"
            + "-fx-padding: 0;"
            + "-fx-text-fill: " + ColorUtils.toCssRgb(color) + ";");
    }

    private static int preferredRows(String text) {
        if (text == null || text.isBlank())
            return 1;
        int lineCount = Math.max(1, text.split("\\R", -1).length);
        int estimatedWrap = Math.max(1, text.length() / 72);
        return Math.max(lineCount, Math.min(8, estimatedWrap + 1));
    }
}
