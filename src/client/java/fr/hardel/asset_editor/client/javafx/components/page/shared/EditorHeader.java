package fr.hardel.asset_editor.client.javafx.components.page.shared;

import fr.hardel.asset_editor.client.javafx.VoxelColors;
import fr.hardel.asset_editor.client.javafx.VoxelFonts;
import fr.hardel.asset_editor.client.javafx.context.StudioContext;
import fr.hardel.asset_editor.client.javafx.data.StudioConcept;
import fr.hardel.asset_editor.client.javafx.data.StudioTabDefinition;
import fr.hardel.asset_editor.client.javafx.data.StudioViewMode;
import fr.hardel.asset_editor.client.javafx.routes.StudioRoute;
import fr.hardel.asset_editor.client.javafx.components.ui.SvgIcon;
import fr.hardel.asset_editor.client.javafx.components.ui.tree.TreeController;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class EditorHeader extends VBox {

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
        int hue = stringToHue(resolveColorKey());
        Color lineColor = hueToColor(hue);
        colorLine.setStyle("-fx-background-color: linear-gradient(from 0% 0% to 100% 0%, "
                + cssColor(lineColor)
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
            Label root = new Label(I18n.get(concept.titleKey()).toUpperCase());
            root.getStyleClass().add("editor-breadcrumb-root");
            breadcrumb.getChildren().add(root);
        } else {
            SvgIcon back = new SvgIcon("/icons/back.svg", 14, VoxelColors.ZINC_400);
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

            Label root = new Label(I18n.get(concept.titleKey()).toUpperCase());
            root.getStyleClass().add("editor-breadcrumb-root");
            breadcrumb.getChildren().add(root);
        }

        List<String> segments = buildSegments();
        for (int i = 0; i < segments.size(); i++) {
            Label sep = new Label("/");
            sep.getStyleClass().add("editor-breadcrumb-separator");
            Label segment = new Label(toDisplay(segments.get(i)).toUpperCase(Locale.ROOT));
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
                    viewButton(StudioViewMode.GRID, "/icons/tools/overview/grid.svg"),
                    viewButton(StudioViewMode.LIST, "/icons/tools/overview/list.svg"));
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

    private StackPane viewButton(StudioViewMode mode, String iconPath) {
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
            return toDisplay(parts[parts.length - 1]);
        }
        String id = tree.currentElementId();
        if (id == null || id.isBlank()) return I18n.get(concept.titleKey());
        String clean = id.contains("$") ? id.substring(0, id.indexOf('$')) : id;
        int sep = clean.indexOf(':');
        return sep >= 0 ? toDisplay(clean.substring(sep + 1)) : toDisplay(clean);
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

    private static String cssColor(Color color) {
        return "rgba(%d,%d,%d,%.3f)".formatted(
                (int) Math.round(color.getRed() * 255),
                (int) Math.round(color.getGreen() * 255),
                (int) Math.round(color.getBlue() * 255),
                color.getOpacity());
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

    private static String toDisplay(String input) {
        if (input == null || input.isBlank()) return "";
        String clean = input.startsWith("#") ? input.substring(1) : input;
        int namespaceSep = clean.indexOf(':');
        String resource = namespaceSep >= 0 ? clean.substring(namespaceSep + 1) : clean;
        String[] path = resource.split("/");
        String leaf = path[path.length - 1];
        String[] words = leaf.replace('_', ' ').trim().split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            if (builder.length() > 0) builder.append(' ');
            builder.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) builder.append(word.substring(1).toLowerCase(Locale.ROOT));
        }
        return builder.toString();
    }
}
