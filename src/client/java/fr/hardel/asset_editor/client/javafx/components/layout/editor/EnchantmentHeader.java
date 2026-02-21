package fr.hardel.asset_editor.client.javafx.components.layout.editor;

import fr.hardel.asset_editor.client.javafx.VoxelColors;
import fr.hardel.asset_editor.client.javafx.VoxelFonts;
import fr.hardel.asset_editor.client.javafx.context.StudioContext;
import fr.hardel.asset_editor.client.javafx.routes.StudioRoute;
import fr.hardel.asset_editor.client.javafx.data.StudioViewMode;
import fr.hardel.asset_editor.client.javafx.components.ui.SvgIcon;
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
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class EnchantmentHeader extends VBox {

    private static final Identifier GRID_ICON = Identifier.fromNamespaceAndPath("asset_editor",
            "icons/tools/overview/grid.svg");
    private static final Identifier LIST_ICON = Identifier.fromNamespaceAndPath("asset_editor",
            "icons/tools/overview/list.svg");
    private static final Identifier BACK_ICON = Identifier.fromNamespaceAndPath("asset_editor", "icons/back.svg");

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
        breadcrumb.setAlignment(Pos.CENTER_LEFT);
        breadcrumb.getStyleClass().add("enchantment-breadcrumb");

        if (!context.router().currentRoute().isOverview()) {
            SvgIcon backIcon = new SvgIcon(BACK_ICON, 14, VoxelColors.ZINC_400);
            backIcon.setOpacity(0.5);
            Label backLabel = new Label(I18n.get("back"));
            backLabel.getStyleClass().add("enchantment-breadcrumb-back-label");
            HBox backBtn = new HBox(6, backIcon, backLabel);
            backBtn.setAlignment(Pos.CENTER_LEFT);
            backBtn.getStyleClass().add("enchantment-breadcrumb-back");
            backBtn.setCursor(Cursor.HAND);
            backBtn.setOnMouseEntered(e -> {
                backIcon.setOpacity(1.0);
                backLabel.setStyle("-fx-text-fill: white;");
            });
            backBtn.setOnMouseExited(e -> {
                backIcon.setOpacity(0.5);
                backLabel.setStyle("");
            });
            backBtn.setOnMouseClicked(e -> context.router().navigate(StudioRoute.ENCHANTMENT_OVERVIEW));
            breadcrumb.getChildren().add(backBtn);
        }

        Label root = new Label(I18n.get("studio.concept.enchantment").toUpperCase());
        root.getStyleClass().add("enchantment-breadcrumb-root");
        breadcrumb.getChildren().add(root);

        List<String> segments = buildSegments();
        for (int i = 0; i < segments.size(); i++) {
            Label sep = new Label("/");
            sep.getStyleClass().add("enchantment-breadcrumb-separator");
            String text = segments.get(i).replace("_", " ").toUpperCase();
            Label segment = new Label(text);
            segment.getStyleClass().add(i == segments.size() - 1
                    ? "enchantment-breadcrumb-leaf"
                    : "enchantment-breadcrumb-segment");
            breadcrumb.getChildren().addAll(sep, segment);
        }
        return breadcrumb;
    }

    private List<String> buildSegments() {
        if (context.router().currentRoute().isOverview()) {
            String filterPath = context.uiState().filterPath();
            return filterPath.isBlank() ? List.of() : Arrays.asList(filterPath.split("/"));
        }
        String id = context.tabsState().currentElementId();
        if (id.isBlank())
            return List.of();
        int sep = id.indexOf(':');
        if (sep < 0)
            return List.of(id);
        List<String> parts = new ArrayList<>();
        parts.add(id.substring(0, sep));
        parts.addAll(Arrays.asList(id.substring(sep + 1).split("/")));
        return parts;
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
        toggle.setAlignment(Pos.CENTER);

        actions.getChildren().addAll(simulation, toggle);
        return actions;
    }

    private StackPane viewButton(StudioViewMode mode, Identifier icon) {
        boolean active = context.uiState().viewMode() == mode;
        SvgIcon svgIcon = new SvgIcon(icon, 14, active ? VoxelColors.ZINC_200 : VoxelColors.ZINC_500);

        StackPane pane = new StackPane(svgIcon);
        pane.getStyleClass().add("enchantment-header-view-button");
        if (active)
            pane.getStyleClass().add("enchantment-header-view-button-active");
        pane.setPrefSize(28, 28);
        pane.setMinSize(28, 28);
        pane.setMaxSize(28, 28);
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
}



