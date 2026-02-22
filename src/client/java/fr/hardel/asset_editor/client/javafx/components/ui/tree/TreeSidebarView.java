package fr.hardel.asset_editor.client.javafx.components.ui.tree;

import fr.hardel.asset_editor.client.javafx.context.StudioContext;
import fr.hardel.asset_editor.client.javafx.components.ui.ResourceImageIcon;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import net.minecraft.client.resources.language.I18n;

public final class TreeSidebarView extends VBox {

    private final StudioContext context;
    private final TreeController tree;
    private final FileTreeView fileTree;

    public TreeSidebarView(StudioContext context, TreeController tree) {
        this.context = context;
        this.tree = tree;
        this.fileTree = new FileTreeView(context, tree);
        getStyleClass().add("tree-sidebar");
        setSpacing(4);
        setPadding(new Insets(16, 0, 0, 0));

        context.uiState().filterPathProperty().addListener((obs, oldValue, newValue) -> refresh());
        context.tabsState().currentElementIdProperty().addListener((obs, oldValue, newValue) -> refresh());
        tree.treeProperty().addListener((obs, oldValue, newValue) -> refresh());
        refresh();
    }

    private void refresh() {
        getChildren().setAll(
                createRow("/icons/pencil.svg", I18n.get("tree.updated"), tree.modifiedCount(), false, () -> {
                    tree.clearSelection();
                    context.router().navigate(tree.changesRoute());
                }),
                createRow("/icons/search.svg", I18n.get("tree.all"), tree.tree().count(), tree.isAllActive(), tree::selectAll),
                fileTree
        );
    }

    private HBox createRow(String iconPath, String text, int count, boolean active, Runnable onClick) {
        HBox row = new HBox(8);
        row.getStyleClass().add("tree-sidebar-row");
        row.getStyleClass().add(active ? "tree-sidebar-row-active" : "tree-sidebar-row-inactive");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setOnMouseClicked(event -> onClick.run());

        if (active) {
            int hue = TreeColorUtil.stringToHue(text.toLowerCase());
            Region accent = new Region();
            accent.getStyleClass().add("tree-row-accent");
            accent.setPrefWidth(4);
            accent.setMinWidth(4);
            accent.setMaxWidth(4);
            Color c = TreeColorUtil.hueToColor(hue);
            accent.setStyle("-fx-background-color: rgba(%d,%d,%d,1.0);".formatted(
                    (int) Math.round(c.getRed() * 255),
                    (int) Math.round(c.getGreen() * 255),
                    (int) Math.round(c.getBlue() * 255)));
            row.getChildren().add(accent);
        } else {
            Region spacer = new Region();
            spacer.setPrefWidth(4);
            spacer.setMinWidth(4);
            spacer.setMaxWidth(4);
            row.getChildren().add(spacer);
        }

        ResourceImageIcon icon = new ResourceImageIcon(iconPath, 20);
        icon.setOpacity(0.6);
        row.getChildren().add(icon);

        Label label = new Label(text);
        label.getStyleClass().add("tree-sidebar-row-label");
        HBox.setHgrow(label, Priority.ALWAYS);
        row.getChildren().add(label);

        Label countLabel = new Label(String.valueOf(count));
        countLabel.getStyleClass().add("tree-sidebar-row-count");
        row.getChildren().add(countLabel);

        return row;
    }
}
