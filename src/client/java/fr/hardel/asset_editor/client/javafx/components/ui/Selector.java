package fr.hardel.asset_editor.client.javafx.components.ui;

import fr.hardel.asset_editor.client.javafx.VoxelFonts;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import net.minecraft.client.resources.language.I18n;

import java.util.LinkedHashMap;
import java.util.function.Consumer;

/**
 * Card with title/description on the left and AnimatedTabs on the right.
 * p-6 outer + px-6 inner = Insets(24, 48, 24, 48).
 */
public final class Selector extends SimpleCard {

    private final AnimatedTabs tabs;

    public Selector(String titleKey, String descKey, LinkedHashMap<String, String> options,
                    String defaultValue, Consumer<String> onChange) {
        super(new Insets(24, 48, 24, 48));
        contentBox.setSpacing(0);

        Label titleLabel = new Label(I18n.get(titleKey));
        titleLabel.getStyleClass().add("tool-selector-title");
        titleLabel.setFont(VoxelFonts.of(VoxelFonts.Variant.REGULAR, 14));

        Label descLabel = new Label(I18n.get(descKey));
        descLabel.getStyleClass().add("tool-selector-desc");
        descLabel.setFont(VoxelFonts.of(VoxelFonts.Variant.LIGHT, 12));
        descLabel.setWrapText(true);

        VBox textBlock = new VBox(4, titleLabel, descLabel);
        textBlock.setMinWidth(0);
        HBox.setHgrow(textBlock, Priority.ALWAYS);

        tabs = new AnimatedTabs(options, defaultValue, onChange);
        tabs.setMinWidth(Region.USE_PREF_SIZE);
        tabs.setMaxWidth(Region.USE_PREF_SIZE);

        HBox row = new HBox(16, textBlock, tabs);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setMaxWidth(Double.MAX_VALUE);

        contentBox.getChildren().add(row);
    }

    public void setValue(String value) {
        tabs.setValue(value);
    }

    public String getValue() {
        return tabs.getValue();
    }
}
