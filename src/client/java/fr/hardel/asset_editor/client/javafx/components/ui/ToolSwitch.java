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
import net.minecraft.client.resources.language.I18n;

/**
 * bg-black/35 border-zinc-900 rounded-xl p-6 hover:-translate-y-1
 * flex row: title+desc (flex-1) | ToggleSwitch
 * Locked: opacity-50, shows lock reason text instead of description.
 */
public final class ToolSwitch extends SimpleCard {

    private final ToggleSwitch toggle = new ToggleSwitch();

    public ToolSwitch(String titleKey, String descKey) {
        this(titleKey, descKey, false, null);
    }

    public ToolSwitch(String titleKey, String descKey, boolean locked, String lockKey) {
        super(new Insets(24));

        Label title = new Label(I18n.get(titleKey));
        title.setFont(VoxelFonts.rubik(VoxelFonts.Rubik.REGULAR, 14));
        title.setTextFill(VoxelColors.ZINC_100);

        String descText = locked && lockKey != null ? I18n.get(lockKey) : I18n.get(descKey);
        Label desc = new Label(descText);
        desc.setFont(VoxelFonts.rubik(VoxelFonts.Rubik.LIGHT, 12));
        desc.setTextFill(VoxelColors.ZINC_400);
        desc.setWrapText(true);

        VBox textBlock = new VBox(4, title, desc);
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
}
