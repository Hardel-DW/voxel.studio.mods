package fr.hardel.asset_editor.client.javafx.components.ui.tree;

import fr.hardel.asset_editor.AssetEditor;
import fr.hardel.asset_editor.client.javafx.lib.StudioContext;
import fr.hardel.asset_editor.client.javafx.components.ui.SvgIcon;
import fr.hardel.asset_editor.client.javafx.lib.FxSelectionBindings;
import fr.hardel.asset_editor.client.javafx.lib.utils.ColorUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.Identifier;

import java.util.Locale;

public final class TreeSidebarView extends VBox {

    private static final Identifier PENCIL_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/pencil.svg");
    private static final Identifier SEARCH_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/search.svg");

    private final StudioContext context;
    private final TreeController tree;
    private final FileTreeView fileTree;
    private final FxSelectionBindings bindings = new FxSelectionBindings();

    public TreeSidebarView(StudioContext context, TreeController tree) {
        this.context = context;
        this.tree = tree;
        this.fileTree = new FileTreeView(context, tree);
        getStyleClass().add("ui-tree-sidebar");
        setSpacing(4);
        setPadding(new Insets(16, 0, 0, 0));

        bindings.observe(context.selectFilterPath(), value -> refresh());
        bindings.observe(context.selectCurrentElementId(), value -> refresh());
        tree.treeProperty().addListener((obs, oldValue, newValue) -> refresh());
        refresh();
    }

    private void refresh() {
        getChildren().setAll(
            createRow(PENCIL_ICON, I18n.get("generic:updated"), "updated", tree.modifiedCount(), false, () -> {
                tree.clearSelection();
                context.router().navigate(tree.changesRoute());
            }),
            createRow(SEARCH_ICON, I18n.get("generic:all"), "all", tree.tree().count(), tree.isAllActive(),
                tree::selectAll),
            fileTree);
    }

    private HBox createRow(Identifier iconPath, String text, String colorKey, int count, boolean active,
        Runnable onClick) {
        HBox row = new HBox(8);
        row.getStyleClass().add("ui-tree-sidebar-row");
        row.getStyleClass().add(active ? "ui-tree-sidebar-row-active" : "ui-tree-sidebar-row-inactive");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setOnMouseClicked(event -> onClick.run());

        if (active) {
            Region accent = new Region();
            accent.getStyleClass().add("ui-tree-row-accent");
            accent.setPrefWidth(4);
            accent.setMinWidth(4);
            accent.setMaxWidth(4);
            Color c = ColorUtils.accentColor(colorKey.toLowerCase(Locale.ROOT));
            accent.setStyle("-fx-background-color: " + ColorUtils.toCssRgba(c) + ";");
            row.getChildren().add(accent);
        } else {
            Region spacer = new Region();
            spacer.setPrefWidth(4);
            spacer.setMinWidth(4);
            spacer.setMaxWidth(4);
            row.getChildren().add(spacer);
        }

        SvgIcon icon = new SvgIcon(iconPath, 20, Color.WHITE);
        icon.setOpacity(0.6);
        row.getChildren().add(icon);

        Label label = new Label(text);
        label.getStyleClass().add("ui-tree-sidebar-row-label");
        HBox.setHgrow(label, Priority.ALWAYS);
        row.getChildren().add(label);

        Label countLabel = new Label(String.valueOf(count));
        countLabel.getStyleClass().add("ui-tree-sidebar-row-count");
        row.getChildren().add(countLabel);

        return row;
    }
}
