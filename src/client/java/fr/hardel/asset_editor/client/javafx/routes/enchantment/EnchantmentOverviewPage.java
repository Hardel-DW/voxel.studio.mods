package fr.hardel.asset_editor.client.javafx.routes.enchantment;

import fr.hardel.asset_editor.client.javafx.components.ui.ResponsiveGrid;
import fr.hardel.asset_editor.client.javafx.components.page.enchantment.EnchantmentOverviewCard;
import fr.hardel.asset_editor.client.javafx.components.page.enchantment.EnchantmentOverviewRow;
import fr.hardel.asset_editor.client.javafx.lib.StudioContext;
import fr.hardel.asset_editor.client.javafx.lib.data.SlotConfigs;
import fr.hardel.asset_editor.client.javafx.lib.Page;
import fr.hardel.asset_editor.client.javafx.lib.data.SlotConfigs.SlotConfig;
import fr.hardel.asset_editor.client.javafx.lib.data.StudioSidebarView;
import fr.hardel.asset_editor.client.javafx.routes.StudioRoute;
import fr.hardel.asset_editor.client.javafx.lib.data.StudioViewMode;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.Identifier;
import net.minecraft.core.Holder;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.List;
import java.util.Locale;

public final class EnchantmentOverviewPage extends VBox implements Page {

    private final StudioContext context;
    private final StackPane content = new StackPane();

    public EnchantmentOverviewPage(StudioContext context) {
        this.context = context;
        getStyleClass().add("enchantment-overview-page");

        TextField search = new TextField();
        search.getStyleClass().add("enchantment-overview-search");
        search.setPromptText(I18n.get("enchantment:overview.search"));
        search.textProperty().bindBidirectional(context.uiState().searchProperty());

        VBox toolbar = new VBox(search);
        toolbar.getStyleClass().add("enchantment-overview-toolbar");
        toolbar.setPadding(new Insets(16, 32, 16, 32));

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.getStyleClass().add("enchantment-overview-scroll");
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        getChildren().addAll(toolbar, scrollPane);

        context.uiState().searchProperty().addListener((obs, oldValue, newValue) -> refresh());
        context.uiState().filterPathProperty().addListener((obs, oldValue, newValue) -> refresh());
        context.uiState().viewModeProperty().addListener((obs, oldValue, newValue) -> refresh());
        context.uiState().sidebarViewProperty().addListener((obs, oldValue, newValue) -> refresh());
        refresh();
    }

    private void refresh() {
        String search = context.uiState().search() == null ? "" : context.uiState().search().trim().toLowerCase(Locale.ROOT);
        String filterPath = context.uiState().filterPath() == null ? "" : context.uiState().filterPath().trim().toLowerCase(Locale.ROOT);
        StudioSidebarView sidebarView = context.uiState().sidebarView();

        List<Holder.Reference<Enchantment>> enchantments = context.registryElements(net.minecraft.core.registries.Registries.ENCHANTMENT).stream()
                .filter(h -> search.isEmpty() || h.key().identifier().getPath().contains(search))
                .filter(h -> matchesFilter(h, filterPath, sidebarView))
                .toList();

        if (enchantments.isEmpty()) {
            content.getChildren().setAll(emptyState());
            return;
        }
        if (context.uiState().viewMode() == StudioViewMode.LIST) {
            VBox list = new VBox(0);
            list.getStyleClass().add("enchantment-overview-list");
            list.setPadding(new Insets(24, 32, 24, 32));
            for (int i = 0; i < enchantments.size(); i++) {
                var holder = enchantments.get(i);
                var row = new EnchantmentOverviewRow(holder, () -> open(holder));
                if (i == 0) row.getStyleClass().add("enchantment-row-first");
                list.getChildren().add(row);
            }
            content.getChildren().setAll(list);
            return;
        }
        ResponsiveGrid grid = new ResponsiveGrid(ResponsiveGrid.autoFit(280));
        grid.getStyleClass().add("enchantment-overview-grid");
        grid.setPadding(new Insets(24, 32, 24, 32));
        for (var holder : enchantments) {
            grid.addItem(new EnchantmentOverviewCard(holder, () -> open(holder)));
        }
        content.getChildren().setAll(grid);
    }

    private static boolean matchesFilter(Holder.Reference<Enchantment> holder, String filterPath, StudioSidebarView sidebarView) {
        if (filterPath.isEmpty()) return true;
        String[] parts = filterPath.split("/", 2);
        String category = parts[0];
        String leaf = parts.length == 2 ? parts[1] : "";
        if (!leaf.isEmpty() && !holder.key().identifier().getPath().equals(leaf)) return false;

        Enchantment enchantment = holder.value();
        if (sidebarView == StudioSidebarView.SLOTS) {
            SlotConfig config = SlotConfigs.BY_ID.get(category);
            if (config == null) return false;
            return enchantment.definition().slots().stream()
                    .anyMatch(g -> config.slots().contains(g.getSerializedName()));
        }
        if (sidebarView == StudioSidebarView.ITEMS) {
            return enchantment.definition().supportedItems().unwrapKey()
                    .map(tag -> tag.location().getPath().endsWith("/" + category))
                    .orElse(false);
        }
        return enchantment.exclusiveSet().unwrapKey()
                .map(tag -> tag.location().getPath().toLowerCase(Locale.ROOT).equals(category))
                .orElse(false);
    }

    private static final Identifier SEARCH_ICON = Identifier.fromNamespaceAndPath("asset_editor", "icons/search.svg");

    private VBox emptyState() {
        var icon = new fr.hardel.asset_editor.client.javafx.components.ui.SvgIcon(SEARCH_ICON, 40, Color.WHITE);
        icon.setOpacity(0.2);
        StackPane circle = new StackPane(icon);
        circle.getStyleClass().add("enchantment-overview-empty-circle");
        circle.setPrefSize(96, 96);
        circle.setMaxSize(96, 96);
        circle.setAlignment(Pos.CENTER);

        Label title = new Label(I18n.get("enchantment:items.no_results.title"));
        title.getStyleClass().add("enchantment-overview-empty-title");
        Label body = new Label(I18n.get("enchantment:items.no_results.description"));
        body.getStyleClass().add("enchantment-overview-empty-body");
        body.setWrapText(true);

        VBox box = new VBox(6, circle, title, body);
        VBox.setMargin(circle, new Insets(0, 0, 18, 0));
        box.getStyleClass().add("enchantment-overview-empty");
        box.setOpacity(0.6);
        return box;
    }

    private void open(Holder.Reference<Enchantment> holder) {
        context.tabsState().openElement(holder.key().identifier().toString(), StudioRoute.ENCHANTMENT_MAIN);
        context.router().navigate(StudioRoute.ENCHANTMENT_MAIN);
    }
}
