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

import java.util.Locale;

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
            int hue = stringToHue(text.toLowerCase(Locale.ROOT));
            Region accent = new Region();
            accent.getStyleClass().add("tree-row-accent");
            accent.setPrefWidth(4);
            accent.setMinWidth(4);
            accent.setMaxWidth(4);
            Color c = hueToColor(hue);
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

    private static int stringToHue(String text) {
        int hash = 0;
        for (int i = 0; i < text.length(); i++) {
            hash = text.charAt(i) + ((hash << 5) - hash);
        }
        return (int) (Math.abs((long) hash) % 360L);
    }

    private static Color hueToColor(int hue) {
        double h = ((hue % 360) + 360) % 360;
        double s = 0.70;
        double l = 0.60;
        double c = (1 - Math.abs(2 * l - 1)) * s;
        double x = c * (1 - Math.abs((h / 60.0) % 2 - 1));
        double m = l - c / 2.0;
        double r = 0;
        double g = 0;
        double b = 0;
        if (h < 60) {
            r = c;
            g = x;
        } else if (h < 120) {
            r = x;
            g = c;
        } else if (h < 180) {
            g = c;
            b = x;
        } else if (h < 240) {
            g = x;
            b = c;
        } else if (h < 300) {
            r = x;
            b = c;
        } else {
            r = c;
            b = x;
        }
        return Color.color(clamp(r + m), clamp(g + m), clamp(b + m));
    }

    private static double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
