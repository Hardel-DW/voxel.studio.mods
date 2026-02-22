package fr.hardel.asset_editor.client.javafx.components.page.enchantment;

import fr.hardel.asset_editor.client.javafx.components.ui.AnimatedTabs;
import fr.hardel.asset_editor.client.javafx.components.ui.SimpleCard;
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
public final class ToolSelector extends SimpleCard {

    public ToolSelector(String titleKey, String descKey, LinkedHashMap<String, String> options,
                        String defaultValue, Consumer<String> onChange) {
        super(new Insets(24, 48, 24, 48));
        contentBox.setSpacing(0);

        Label titleLabel = new Label(I18n.get(titleKey));
        titleLabel.getStyleClass().add("tool-selector-title");

        Label descLabel = new Label(I18n.get(descKey));
        descLabel.getStyleClass().add("tool-selector-desc");
        descLabel.setWrapText(true);

        VBox textBlock = new VBox(4, titleLabel, descLabel);
        textBlock.setMinWidth(0);
        HBox.setHgrow(textBlock, Priority.ALWAYS);

        AnimatedTabs tabs = new AnimatedTabs(options, defaultValue, onChange);
        tabs.setMinWidth(Region.USE_PREF_SIZE);
        tabs.setMaxWidth(Region.USE_PREF_SIZE);

        // flex justify-between items-center w-full gap-4
        HBox row = new HBox(16, textBlock, tabs);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setMaxWidth(Double.MAX_VALUE);

        contentBox.getChildren().add(row);
    }
}
