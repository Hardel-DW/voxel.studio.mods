package fr.hardel.asset_editor.client.javafx.editor.layout;

import fr.hardel.asset_editor.client.javafx.VoxelColors;
import fr.hardel.asset_editor.client.javafx.VoxelFonts;
import fr.hardel.asset_editor.client.javafx.editor.StudioContext;
import fr.hardel.asset_editor.client.javafx.editor.state.StudioRoute;
import fr.hardel.asset_editor.client.javafx.editor.state.StudioViewMode;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.scene.transform.Scale;
import net.minecraft.client.resources.language.I18n;

public final class EnchantmentHeader extends VBox {

    private static final String GRID_ICON = "M1 1h6v6H1zM9 1h6v6H9zM1 9h6v6H1zM9 9h6v6H9z";
    private static final String LIST_ICON = "M1 4h14v2H1zM1 8h14v2H1zM1 12h14v2H1z";

    private final StudioContext context;
    private final HBox row = new HBox();

    public EnchantmentHeader(StudioContext context) {
        this.context = context;
        getStyleClass().add("enchantment-header");
        setPadding(new Insets(28, 32, 12, 32));

        row.setAlignment(Pos.BOTTOM_LEFT);
        row.setSpacing(16);

        getChildren().addAll(row, new EnchantmentHeaderTabs(context));

        context.router().routeProperty().addListener((obs, o, n) -> refresh());
        context.uiState().filterPathProperty().addListener((obs, o, n) -> refresh());
        context.tabsState().currentElementIdProperty().addListener((obs, o, n) -> refresh());
        context.uiState().viewModeProperty().addListener((obs, o, n) -> refresh());
        refresh();
    }

    private void refresh() {
        row.getChildren().clear();

        Label title = new Label(resolveTitle());
        title.getStyleClass().add("enchantment-header-title");
        title.setFont(VoxelFonts.minecraft(VoxelFonts.Minecraft.TEN, 36));

        Region colorLine = new Region();
        colorLine.getStyleClass().add("enchantment-header-color-line");

        VBox left = new VBox(8, breadcrumb(), title, colorLine);
        HBox.setHgrow(left, Priority.ALWAYS);

        row.getChildren().addAll(left, actions());
    }

    private HBox breadcrumb() {
        HBox breadcrumb = new HBox(8);
        breadcrumb.getStyleClass().add("enchantment-breadcrumb");

        if (!context.router().currentRoute().isOverview()) {
            Button back = new Button(I18n.get("back"));
            back.getStyleClass().add("enchantment-breadcrumb-back");
            back.setOnAction(e -> context.router().navigate(StudioRoute.ENCHANTMENT_OVERVIEW));
            breadcrumb.getChildren().add(back);
        }

        Label root = new Label(I18n.get("studio.concept.enchantment").toUpperCase());
        root.getStyleClass().add("enchantment-breadcrumb-root");
        breadcrumb.getChildren().add(root);

        String tail = breadcrumbTail();
        if (!tail.isBlank()) {
            Label sep = new Label("/");
            sep.getStyleClass().add("enchantment-breadcrumb-separator");
            Label leaf = new Label(tail.toUpperCase());
            leaf.getStyleClass().add("enchantment-breadcrumb-leaf");
            breadcrumb.getChildren().addAll(sep, leaf);
        }
        return breadcrumb;
    }

    private HBox actions() {
        HBox actions = new HBox(8);
        actions.getStyleClass().add("enchantment-header-actions");
        actions.setAlignment(Pos.CENTER_RIGHT);

        if (!context.router().currentRoute().isOverview())
            return actions;

        Button simulation = new Button(I18n.get("enchantment:simulation"));
        simulation.getStyleClass().add("enchantment-header-simulation");
        simulation.setOnAction(e -> context.router().navigate(StudioRoute.ENCHANTMENT_SIMULATION));

        HBox toggle = new HBox(4,
                viewButton(StudioViewMode.GRID, GRID_ICON),
                viewButton(StudioViewMode.LIST, LIST_ICON));
        toggle.getStyleClass().add("enchantment-header-view-toggle");

        actions.getChildren().addAll(simulation, toggle);
        return actions;
    }

    private StackPane viewButton(StudioViewMode mode, String svgContent) {
        boolean active = context.uiState().viewMode() == mode;

        SVGPath icon = new SVGPath();
        icon.setContent(svgContent);
        icon.setFill(active ? VoxelColors.ZINC_200 : VoxelColors.ZINC_500);
        icon.getTransforms().add(new Scale(0.75, 0.75, 0, 0));

        StackPane pane = new StackPane(icon);
        pane.getStyleClass().add("enchantment-header-view-button");
        if (active) pane.getStyleClass().add("enchantment-header-view-button-active");
        pane.setCursor(Cursor.HAND);
        pane.setOnMouseClicked(e -> context.uiState().setViewMode(mode));
        return pane;
    }

    private String resolveTitle() {
        if (context.router().currentRoute().isOverview()) {
            String filterPath = context.uiState().filterPath();
            if (filterPath.isBlank())
                return I18n.get("editor.all");
            String[] parts = filterPath.split("/");
            return parts[parts.length - 1];
        }
        String id = context.tabsState().currentElementId();
        if (id.isBlank())
            return I18n.get("studio.concept.enchantment");
        int sep = id.indexOf(':');
        return (sep >= 0 && sep + 1 < id.length()) ? id.substring(sep + 1) : id;
    }

    private String breadcrumbTail() {
        if (context.router().currentRoute().isOverview()) {
            String filterPath = context.uiState().filterPath();
            if (filterPath.isBlank())
                return "";
            String[] parts = filterPath.split("/");
            return parts[parts.length - 1];
        }
        return context.tabsState().currentElementId();
    }
}
