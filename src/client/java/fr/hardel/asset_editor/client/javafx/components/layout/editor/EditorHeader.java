package fr.hardel.asset_editor.client.javafx.components.layout.editor;

import fr.hardel.asset_editor.client.javafx.VoxelFonts;
import fr.hardel.asset_editor.client.javafx.lib.data.StudioConcept;
import fr.hardel.asset_editor.client.javafx.lib.data.StudioTabDefinition;
import fr.hardel.asset_editor.client.javafx.lib.data.StudioViewMode;
import fr.hardel.asset_editor.client.javafx.lib.StudioContext;
import fr.hardel.asset_editor.client.javafx.routes.StudioRoute;
import fr.hardel.asset_editor.client.javafx.components.ui.ToggleGroup;
import fr.hardel.asset_editor.client.javafx.components.ui.tree.TreeController;
import fr.hardel.asset_editor.client.javafx.lib.utils.ColorUtils;
import fr.hardel.asset_editor.client.javafx.lib.utils.TextUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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

import java.util.Locale;

public final class EditorHeader extends VBox {

    private static final Identifier GRID_ICON = Identifier.fromNamespaceAndPath("asset_editor", "icons/tools/overview/grid.svg");
    private static final Identifier LIST_ICON = Identifier.fromNamespaceAndPath("asset_editor", "icons/tools/overview/list.svg");

    private final StudioContext context;
    private final TreeController tree;
    private final StudioConcept concept;
    private final boolean showViewToggle;
    private final StudioRoute simulationRoute;
    private final StackPane surface = new StackPane();
    private final Region tintLayer = new Region();
    private final Region gradientLayer = new Region();
    private final VBox content = new VBox();

    public EditorHeader(StudioContext context, TreeController tree, StudioConcept concept, boolean showViewToggle, StudioRoute simulationRoute) {
        this.context = context;
        this.tree = tree;
        this.concept = concept;
        this.showViewToggle = showViewToggle;
        this.simulationRoute = simulationRoute;

        getStyleClass().add("editor-header");

        surface.getStyleClass().add("editor-header-surface");
        surface.setAlignment(Pos.TOP_LEFT);
        surface.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(surface, Priority.NEVER);

        tintLayer.getStyleClass().add("editor-header-tint");
        tintLayer.setOpacity(0.4);
        tintLayer.setMouseTransparent(true);

        gradientLayer.getStyleClass().add("editor-header-gradient");
        gradientLayer.setMouseTransparent(true);

        content.getStyleClass().add("editor-header-content");
        content.setPadding(new Insets(32, 32, 24, 32));
        content.setSpacing(0);
        content.setMaxWidth(Double.MAX_VALUE);

        surface.getChildren().addAll(tintLayer, gradientLayer, content);
        getChildren().add(surface);

        context.router().routeProperty().addListener((obs, oldValue, newValue) -> refresh());
        context.uiState().filterPathProperty().addListener((obs, oldValue, newValue) -> refresh());
        context.tabsState().currentElementIdProperty().addListener((obs, oldValue, newValue) -> refresh());
        context.uiState().viewModeProperty().addListener((obs, oldValue, newValue) -> refresh());
        refresh();
    }

    private void refresh() {
        content.getChildren().clear();

        HBox row = new HBox(16);
        row.setAlignment(Pos.BOTTOM_LEFT);
        Label title = new Label(resolveTitle());
        title.getStyleClass().add("editor-header-title");
        title.setFont(VoxelFonts.minecraft(VoxelFonts.Minecraft.TEN, 36));

        Region colorLine = new Region();
        colorLine.getStyleClass().add("editor-header-color-line");
        Color lineColor = ColorUtils.accentColor(resolveColorKey());
        tintLayer.setStyle("-fx-background-color: " + ColorUtils.toCssRgba(lineColor) + ";");
        colorLine.setStyle("-fx-background-color: linear-gradient(from 0% 0% to 100% 0%, "
                + ColorUtils.toCssRgba(lineColor)
                + " 0%, transparent 100%);");

        EditorBreadcrumb breadcrumb = new EditorBreadcrumb(
                I18n.get(concept.titleKey()),
                tree.filterPath(),
                tree.currentElementId(),
                isOverview(),
                () -> context.router().navigate(concept.overviewRoute()));
        VBox left = new VBox(8, breadcrumb, title, colorLine);
        HBox.setHgrow(left, Priority.ALWAYS);
        row.getChildren().addAll(left, actions());

        content.getChildren().add(row);
        if (showTabs()) {
            content.getChildren().add(buildTabs());
        }
    }

    private HBox actions() {
        HBox actions = new HBox(8);
        actions.getStyleClass().add("editor-header-actions");
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.setFillHeight(false);
        actions.setMaxHeight(Region.USE_PREF_SIZE);
        if (!isOverview()) return actions;

        if (simulationRoute != null) {
            Button simulation = new Button(I18n.get("enchantment:simulation"));
            simulation.getStyleClass().add("editor-header-simulation");
            simulation.setMaxHeight(Region.USE_PREF_SIZE);
            simulation.setOnAction(event -> context.router().navigate(simulationRoute));
            actions.getChildren().add(simulation);
        }

        if (showViewToggle) {
            ToggleGroup toggle = new ToggleGroup(
                    () -> context.uiState().viewMode().name().toLowerCase(Locale.ROOT),
                    value -> context.uiState().setViewMode("list".equals(value) ? StudioViewMode.LIST : StudioViewMode.GRID));
            toggle.addIconOption("grid", GRID_ICON);
            toggle.addIconOption("list", LIST_ICON);
            toggle.setMinHeight(Region.USE_PREF_SIZE);
            toggle.setMaxHeight(Region.USE_PREF_SIZE);
            toggle.setMaxWidth(Region.USE_PREF_SIZE);
            toggle.refresh();
            actions.getChildren().add(toggle);
        }
        return actions;
    }

    private HBox buildTabs() {
        HBox tabs = new HBox(4);
        tabs.getStyleClass().add("editor-header-tabs");
        tabs.setAlignment(Pos.CENTER_LEFT);
        tabs.setPadding(new Insets(24, 0, 0, 0));

        StudioRoute current = context.router().currentRoute();
        for (StudioTabDefinition tab : concept.tabs()) {
            Button button = new EditorHeaderTabItem(
                    I18n.get(tab.translationKey()),
                    current == tab.route(),
                    () -> context.router().navigate(tab.route()));
            tabs.getChildren().add(button);
        }
        return tabs;
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
        String resource = sep >= 0 ? clean.substring(sep + 1) : clean;
        int slash = resource.lastIndexOf('/');
        String leaf = slash >= 0 ? resource.substring(slash + 1) : resource;
        return TextUtils.toDisplay(leaf);
    }

    private String resolveColorKey() {
        if (isOverview()) {
            String filterPath = tree.filterPath();
            return filterPath == null || filterPath.isBlank() ? "all" : filterPath;
        }
        String id = ColorUtils.normalizeColorKey(tree.currentElementId());
        return (id == null || id.isBlank()) ? concept.registry() : id;
    }

    private boolean isOverview() {
        return context.router().currentRoute().isOverview();
    }
}
