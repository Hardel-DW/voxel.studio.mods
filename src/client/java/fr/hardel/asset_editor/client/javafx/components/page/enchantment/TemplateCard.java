package fr.hardel.asset_editor.client.javafx.components.page.enchantment;

import fr.hardel.asset_editor.client.javafx.components.ui.SimpleCard;
import fr.hardel.asset_editor.client.javafx.components.ui.SvgIcon;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.Identifier;

/**
 * TemplateCard: SimpleCard with icon (h-16 = 64px, white) + child node (Counter),
 * and title/description below.
 * Used for maxLevel, weight, anvilCost tool cards.
 */
public final class TemplateCard extends SimpleCard {

    public TemplateCard(Identifier iconPath, String titleKey, String descriptionKey, Node child) {
        // Top row: flex items-center justify-between w-full gap-4
        SvgIcon icon = new SvgIcon(iconPath, 64, Color.WHITE);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox topRow = new HBox(16, icon, spacer, child);
        topRow.setAlignment(Pos.CENTER);
        topRow.setMaxWidth(Double.MAX_VALUE);

        // Bottom: title + description (mb-1 between title and desc = 4px spacing)
        Label titleLabel = new Label(I18n.get(titleKey));
        titleLabel.getStyleClass().add("template-card-title");

        VBox infoBox = new VBox(4, titleLabel);
        VBox.setMargin(infoBox, new Insets(12, 0, 0, 0)); // gap between topRow and infoBox

        if (descriptionKey != null) {
            Label descLabel = new Label(I18n.get(descriptionKey));
            descLabel.getStyleClass().add("template-card-desc");
            descLabel.setWrapText(true);
            infoBox.getChildren().add(descLabel);
        }

        contentBox.getChildren().addAll(topRow, infoBox);
    }
}
