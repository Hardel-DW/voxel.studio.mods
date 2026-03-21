package fr.hardel.asset_editor.client.javafx.components.ui;

import fr.hardel.asset_editor.client.javafx.VoxelFonts;
import fr.hardel.asset_editor.client.javafx.VoxelColors;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public final class Section extends VBox {

    private final VBox childrenBox = new VBox(16);

    public Section(String title) {
        setSpacing(0);
        setFillWidth(true);
        setMaxWidth(Double.MAX_VALUE);

        HBox titleRow = new HBox(16);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        titleRow.setPadding(new Insets(8, 8, 8, 8));

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("ui-tool-section-title");
        titleLabel.setFont(VoxelFonts.of(VoxelFonts.Variant.SEMI_BOLD, 24));
        titleLabel.setTextFill(VoxelColors.ZINC_100);

        Region hr = new Region();
        hr.getStyleClass().add("ui-tool-section-hr");
        hr.setMaxWidth(Double.MAX_VALUE);

        VBox titleBlock = new VBox(8, titleLabel, hr);
        titleBlock.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(titleBlock, Priority.ALWAYS);
        titleRow.getChildren().add(titleBlock);

        childrenBox.setPadding(new Insets(16, 0, 0, 0));
        childrenBox.setFillWidth(true);
        VBox.setVgrow(childrenBox, Priority.ALWAYS);
        getChildren().addAll(titleRow, childrenBox);
    }

    public void addContent(Node... nodes) {
        for (Node node : nodes) {
            if (node instanceof Region region)
                region.setMaxWidth(Double.MAX_VALUE);

            childrenBox.getChildren().add(node);
        }
    }
}
