package fr.hardel.asset_editor.client.javafx.components.ui;

import fr.hardel.asset_editor.AssetEditor;
import fr.hardel.asset_editor.client.javafx.VoxelColors;
import fr.hardel.asset_editor.client.javafx.VoxelFonts;
import fr.hardel.asset_editor.client.javafx.lib.utils.ColorUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public final class Dropdown<T> extends HBox {

    private static final Identifier CHEVRON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/chevron-down.svg");
    private static final String HOVER_STYLE = "-fx-background-color: " + ColorUtils.toCssRgb(VoxelColors.ZINC_800) + "; -fx-background-radius: 6;";

    private final Function<T, String> labelExtractor;
    private final Consumer<T> onChange;
    private final Label triggerLabel = new Label();
    private final VBox itemsContainer = new VBox();
    private final Popover popover;

    private List<T> items;
    private T value;

    public Dropdown(List<T> items, T selected, Function<T, String> labelExtractor, Consumer<T> onChange) {
        this.items = items;
        this.value = selected;
        this.labelExtractor = labelExtractor;
        this.onChange = onChange;

        setAlignment(Pos.CENTER_LEFT);
        setSpacing(8);
        setPadding(new Insets(0, 12, 0, 12));
        setPrefHeight(32);
        setMinHeight(32);
        setMaxHeight(32);
        setCursor(Cursor.HAND);
        getStyleClass().add("studio-button-ghost-border");

        triggerLabel.setFont(VoxelFonts.of(VoxelFonts.Variant.MEDIUM, 12));
        triggerLabel.setTextFill(VoxelColors.ZINC_300);
        HBox.setHgrow(triggerLabel, Priority.ALWAYS);

        getChildren().addAll(triggerLabel, new SvgIcon(CHEVRON, 10, VoxelColors.ZINC_500));

        itemsContainer.setPadding(new Insets(4));
        itemsContainer.setMinWidth(160);
        this.popover = new Popover(this, itemsContainer);

        syncTriggerLabel();
        rebuildItems();
    }

    public void setItems(List<T> items) {
        this.items = items;
        rebuildItems();
    }

    public void setValue(T value) {
        this.value = value;
        syncTriggerLabel();
        rebuildItems();
    }

    public T getValue() {
        return value;
    }

    private void syncTriggerLabel() {
        triggerLabel.setText(value != null ? labelExtractor.apply(value) : "");
    }

    private void select(T item) {
        value = item;
        syncTriggerLabel();
        popover.hide();
        rebuildItems();
        onChange.accept(item);
    }

    private void rebuildItems() {
        itemsContainer.getChildren().clear();
        for (T item : items)
            itemsContainer.getChildren().add(buildOption(item));
    }

    private Label buildOption(T item) {
        Label label = new Label(labelExtractor.apply(item));
        label.setFont(VoxelFonts.of(VoxelFonts.Variant.MEDIUM, 12));
        label.setTextFill(item.equals(value) ? VoxelColors.ZINC_100 : VoxelColors.ZINC_400);
        label.setPadding(new Insets(6, 12, 6, 12));
        label.setMaxWidth(Double.MAX_VALUE);
        label.setCursor(Cursor.HAND);
        label.setOnMouseEntered(e -> label.setStyle(HOVER_STYLE));
        label.setOnMouseExited(e -> label.setStyle(null));
        label.setOnMouseClicked(e -> { e.consume(); select(item); });
        return label;
    }
}
