package fr.hardel.asset_editor.client.javafx.components.layout.editor;

import fr.hardel.asset_editor.client.javafx.VoxelColors;
import fr.hardel.asset_editor.client.javafx.VoxelFonts;
import fr.hardel.asset_editor.client.javafx.lib.data.StudioConcept;
import fr.hardel.asset_editor.client.javafx.lib.data.StudioTabDefinition;
import fr.hardel.asset_editor.client.javafx.lib.data.StudioViewMode;
import fr.hardel.asset_editor.client.javafx.lib.StudioContext;
import fr.hardel.asset_editor.client.javafx.routes.StudioRoute;
import fr.hardel.asset_editor.client.javafx.components.ui.SvgIcon;
import fr.hardel.asset_editor.client.javafx.components.ui.tree.TreeController;
import fr.hardel.asset_editor.client.javafx.lib.utils.ColorUtils;
import fr.hardel.asset_editor.client.javafx.lib.utils.TextUtils;
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
import javafx.scene.paint.Color;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class EditorHeader extends VBox {

    private static final Identifier BACK_ICON = Identifier.fromNamespaceAndPath("asset_editor", "icons/back.svg");
    private static final Identifier GRID_ICON = Identifier.fromNamespaceAndPath("asset_editor", "icons/tools/overview/grid.svg");
    private static final Identifier LIST_ICON = Identifier.fromNamespaceAndPath("asset_editor", "icons/tools/overview/list.svg");

    private final StudioContext context;
    private final TreeController tree;
    private final StudioConcept concept;
    private final boolean showViewToggle;
    private final StudioRoute simulationRoute;

    public EditorHeader(StudioContext context, TreeController tree, StudioConcept concept, boolean showViewToggle, StudioRoute simulationRoute) {
        this.context = context;
        this.tree = tree;
        this.concept = concept;
        this.showViewToggle = showViewToggle;
        this.simulationRoute = simulationRoute;

        getStyleClass().add("editor-header");
        setPadding(new Insets(28, 32, 12, 32));

        context.router().routeProperty().addListener((obs, oldValue, newValue) -> refresh());
        context.uiState().filterPathProperty().addListener((obs, oldValue, newValue) -> refresh());
        context.tabsState().currentElementIdProperty().addListener((obs, oldValue, newValue) -> refresh());
        context.uiState().viewModeProperty().addListener((obs, oldValue, newValue) -> refresh());
        refresh();
    }

    private void refresh() {
        getChildren().clear();

        HBox row = new HBox(16);
        row.setAlignment(Pos.BOTTOM_LEFT);
        Label title = new Label(resolveTitle());
        title.getStyleClass().add("editor-header-title");
        title.setFont(VoxelFonts.minecraft(VoxelFonts.Minecraft.TEN, 36));

        Region colorLine = new Region();
        colorLine.getStyleClass().add("editor-header-color-line");
        int hue = ColorUtils.stringToHue(resolveColorKey());
        Color lineColor = ColorUtils.hueToColor(hue);
        colorLine.setStyle("-fx-background-color: linear-gradient(from 0% 0% to 100% 0%, "
                + ColorUtils.toCssRgba(lineColor)
                + " 0%, transparent 100%);");

        VBox left = new VBox(8, breadcrumb(), title, colorLine);
        HBox.setHgrow(left, Priority.ALWAYS);
        row.getChildren().addAll(left, actions());

        getChildren().add(row);
        if (showTabs()) {
            getChildren().add(buildTabs());
        }
    }

    private HBox breadcrumb() {
        HBox breadcrumb = new HBox(8);
        breadcrumb.getStyleClass().add("editor-breadcrumb");
        breadcrumb.setAlignment(Pos.CENTER_LEFT);

        if (isOverview()) {
            Label root = new Label(I18n.get(concept.titleKey()).toUpperCase(Locale.ROOT));
            root.getStyleClass().add("editor-breadcrumb-root");
            breadcrumb.getChildren().add(root);
        } else {
            SvgIcon back = new SvgIcon(BACK_ICON, 14, VoxelColors.ZINC_400);
            back.setOpacity(0.5);
            Label backLabel = new Label(I18n.get("back"));
            backLabel.getStyleClass().add("editor-breadcrumb-back-label");
            HBox backBtn = new HBox(6, back, backLabel);
            backBtn.getStyleClass().add("editor-breadcrumb-back");
            backBtn.setAlignment(Pos.CENTER_LEFT);
            backBtn.setCursor(Cursor.HAND);
            backBtn.setOnMouseEntered(event -> {
                back.setOpacity(1.0);
                backLabel.setStyle("-fx-text-fill: white;");
            });
            backBtn.setOnMouseExited(event -> {
                back.setOpacity(0.5);
                backLabel.setStyle("");
            });
            backBtn.setOnMouseClicked(event -> context.router().navigate(concept.overviewRoute()));
            breadcrumb.getChildren().add(backBtn);

            Label root = new Label(I18n.get(concept.titleKey()).toUpperCase(Locale.ROOT));
            root.getStyleClass().add("editor-breadcrumb-root");
            breadcrumb.getChildren().add(root);
        }

        List<String> segments = buildSegments();
        for (int i = 0; i < segments.size(); i++) {
            Label sep = new Label("/");
            sep.getStyleClass().add("editor-breadcrumb-separator");
            Label segment = new Label(TextUtils.toDisplay(segments.get(i)).toUpperCase(Locale.ROOT));
            segment.getStyleClass().add(i == segments.size() - 1
                    ? "editor-breadcrumb-leaf"
                    : "editor-breadcrumb-segment");
            breadcrumb.getChildren().addAll(sep, segment);
        }
        return breadcrumb;
    }

    private HBox actions() {
        HBox actions = new HBox(8);
        actions.getStyleClass().add("editor-header-actions");
        actions.setAlignment(Pos.CENTER_RIGHT);
        if (!isOverview()) return actions;

        if (simulationRoute != null) {
            Button simulation = new Button(I18n.get("enchantment:simulation"));
            simulation.getStyleClass().add("editor-header-simulation");
            simulation.setOnAction(event -> context.router().navigate(simulationRoute));
            actions.getChildren().add(simulation);
        }

        if (showViewToggle) {
            HBox toggle = new HBox(4,
                    viewButton(StudioViewMode.GRID, GRID_ICON),
                    viewButton(StudioViewMode.LIST, LIST_ICON));
            toggle.getStyleClass().add("editor-header-view-toggle");
            toggle.setAlignment(Pos.CENTER);
            actions.getChildren().add(toggle);
        }
        return actions;
    }

    private HBox buildTabs() {
        HBox tabs = new HBox(4);
        tabs.getStyleClass().add("editor-header-tabs");
        tabs.setAlignment(Pos.CENTER_LEFT);
        tabs.setPadding(new Insets(8, 0, 0, 0));

        StudioRoute current = context.router().currentRoute();
        for (StudioTabDefinition tab : concept.tabs()) {
            Button button = new Button(I18n.get(tab.translationKey()));
            button.getStyleClass().add("editor-header-tab-button");
            if (current == tab.route()) button.getStyleClass().add("editor-header-tab-button-active");
            button.setOnAction(event -> context.router().navigate(tab.route()));
            tabs.getChildren().add(button);
        }
        return tabs;
    }

    private StackPane viewButton(StudioViewMode mode, Identifier iconPath) {
        boolean active = context.uiState().viewMode() == mode;
        SvgIcon icon = new SvgIcon(iconPath, 14, active ? VoxelColors.ZINC_200 : VoxelColors.ZINC_500);
        StackPane pane = new StackPane(icon);
        pane.getStyleClass().add("editor-header-view-button");
        if (active) pane.getStyleClass().add("editor-header-view-button-active");
        pane.setPrefSize(28, 28);
        pane.setMinSize(28, 28);
        pane.setMaxSize(28, 28);
        pane.setCursor(Cursor.HAND);
        pane.setOnMouseClicked(event -> context.uiState().setViewMode(mode));
        return pane;
    }

    private boolean showTabs() {
        if (concept.tabs().size() <= 1) return false;
        String current = tree.currentElementId();
        if (current == null || current.isBlank()) return false;
        StudioRoute route = context.router().currentRoute();
        return route.concept().equals(concept.registry()) && concept.tabRoutes().contains(route);
    }

    private String resolveTitle() {
        if (isOverview()) {
            String filterPath = tree.filterPath();
            if (filterPath == null || filterPath.isBlank()) return I18n.get("editor.all");
            String[] parts = filterPath.split("/");
            return TextUtils.toDisplay(parts[parts.length - 1]);
        }
        String id = tree.currentElementId();
        if (id == null || id.isBlank()) return I18n.get(concept.titleKey());
        String clean = id.contains("$") ? id.substring(0, id.indexOf('$')) : id;
        int sep = clean.indexOf(':');
        return sep >= 0 ? TextUtils.toDisplay(clean.substring(sep + 1)) : TextUtils.toDisplay(clean);
    }

    private String resolveColorKey() {
        if (isOverview()) {
            String filterPath = tree.filterPath();
            return filterPath == null || filterPath.isBlank() ? "all" : filterPath;
        }
        String id = tree.currentElementId();
        return (id == null || id.isBlank()) ? concept.registry() : id;
    }

    private boolean isOverview() {
        return context.router().currentRoute().isOverview();
    }

    private List<String> buildSegments() {
        ArrayList<String> segments = new ArrayList<>();
        if (isOverview()) {
            String filterPath = tree.filterPath();
            if (filterPath == null || filterPath.isBlank()) return segments;
            String[] parts = filterPath.split("/");
            for (String part : parts) segments.add(part);
            return segments;
        }
        String id = tree.currentElementId();
        if (id == null || id.isBlank()) return segments;
        String clean = id.contains("$") ? id.substring(0, id.indexOf('$')) : id;
        int sep = clean.indexOf(':');
        if (sep < 0) {
            segments.add(clean);
            return segments;
        }
        segments.add(clean.substring(0, sep));
        String[] parts = clean.substring(sep + 1).split("/");
        for (String part : parts) segments.add(part);
        return segments;
    }

}
