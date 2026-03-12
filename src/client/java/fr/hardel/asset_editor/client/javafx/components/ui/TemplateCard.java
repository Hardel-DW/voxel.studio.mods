package fr.hardel.asset_editor.client.javafx.components.ui;

import fr.hardel.asset_editor.client.javafx.VoxelFonts;
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

public final class TemplateCard extends SimpleCard {

    public TemplateCard(Identifier iconPath, String titleKey, String descriptionKey, Node child) {
        SvgIcon icon = new SvgIcon(iconPath, 64, Color.WHITE);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox topRow = new HBox(16, icon, spacer, child);
        topRow.setAlignment(Pos.CENTER);
        topRow.setMaxWidth(Double.MAX_VALUE);

        Label titleLabel = new Label(I18n.get(titleKey));
        titleLabel.getStyleClass().add("ui-template-card-title");
        titleLabel.setFont(VoxelFonts.of(VoxelFonts.Variant.SEMI_BOLD, 18));

        VBox infoBox = new VBox(4, titleLabel);
        VBox.setMargin(infoBox, new Insets(12, 0, 0, 0));

        if (descriptionKey != null) {
            Label descLabel = new Label(I18n.get(descriptionKey));
            descLabel.getStyleClass().add("ui-template-card-desc");
            descLabel.setFont(VoxelFonts.of(VoxelFonts.Variant.REGULAR, 14));
            descLabel.setWrapText(true);
            infoBox.getChildren().add(descLabel);
        }

        contentBox.getChildren().addAll(topRow, infoBox);
    }
}
