package fr.hardel.asset_editor.client.javafx.components.ui;

import fr.hardel.asset_editor.client.javafx.VoxelColors;
import fr.hardel.asset_editor.client.javafx.VoxelFonts;
import javafx.beans.property.BooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * bg-black/35 border-zinc-900 rounded-xl p-6 hover:-translate-y-1
 * flex row: title+desc (flex-1) | ToggleSwitch
 * Locked: opacity-50, shows lock reason text instead of description.
 */
public final class SwitchCard extends SimpleCard {

    private final ToggleSwitch toggle = new ToggleSwitch();

    public SwitchCard(String title, String description) {
        this(title, description, false, null);
    }

    public SwitchCard(String title, String description, boolean locked, String lockText) {
        super(new Insets(24));

        Label titleLabel = new Label(title);
        titleLabel.setFont(VoxelFonts.of(VoxelFonts.Variant.REGULAR, 14));
        titleLabel.setTextFill(VoxelColors.ZINC_100);

        String descriptionText = locked && lockText != null ? lockText : description;
        Label descriptionLabel = new Label(descriptionText);
        descriptionLabel.setFont(VoxelFonts.of(VoxelFonts.Variant.LIGHT, 12));
        descriptionLabel.setTextFill(VoxelColors.ZINC_400);
        descriptionLabel.setWrapText(true);

        VBox textBlock = new VBox(4, titleLabel, descriptionLabel);
        HBox.setHgrow(textBlock, Priority.ALWAYS);

        toggle.setDisable(locked);
        if (locked) setOpacity(0.5);

        HBox row = new HBox(16, textBlock, toggle);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setMaxWidth(Double.MAX_VALUE);
        contentBox.getChildren().add(row);
    }

    public BooleanProperty valueProperty() { return toggle.valueProperty(); }
    public boolean isOn() { return toggle.isOn(); }
    public void setValue(boolean v) { toggle.setValue(v); }

    public static SwitchCard literal(String title, String description) {
        return new SwitchCard(title, description, false, null);
    }
}
