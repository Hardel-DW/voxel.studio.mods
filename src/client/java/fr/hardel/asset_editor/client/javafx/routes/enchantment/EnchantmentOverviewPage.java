package fr.hardel.asset_editor.client.javafx.routes.enchantment;

import fr.hardel.asset_editor.client.javafx.components.page.enchantment.EnchantmentOverviewCard;
import fr.hardel.asset_editor.client.javafx.components.page.enchantment.EnchantmentOverviewRow;
import fr.hardel.asset_editor.client.javafx.lib.StudioContext;
import fr.hardel.asset_editor.client.javafx.lib.data.mock.StudioMockEnchantment;
import fr.hardel.asset_editor.client.javafx.routes.StudioRoute;
import fr.hardel.asset_editor.client.javafx.lib.data.StudioViewMode;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import net.minecraft.client.resources.language.I18n;

import java.util.List;

public final class EnchantmentOverviewPage extends VBox {

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
        List<StudioMockEnchantment> enchantments = context.repository().filter(
                context.uiState().search(),
                context.uiState().filterPath(),
                context.uiState().sidebarView());
        if (enchantments.isEmpty()) {
            content.getChildren().setAll(emptyState());
            return;
        }
        if (context.uiState().viewMode() == StudioViewMode.LIST) {
            VBox list = new VBox(8);
            list.getStyleClass().add("enchantment-overview-list");
            list.setPadding(new Insets(20, 32, 24, 32));
            for (StudioMockEnchantment enchantment : enchantments)
                list.getChildren().add(new EnchantmentOverviewRow(enchantment, () -> open(enchantment)));
            content.getChildren().setAll(list);
            return;
        }
        FlowPane grid = new FlowPane();
        grid.getStyleClass().add("enchantment-overview-grid");
        grid.setHgap(14);
        grid.setVgap(14);
        grid.setPadding(new Insets(20, 32, 24, 32));
        grid.prefWrapLengthProperty().bind(widthProperty().subtract(96));
        for (StudioMockEnchantment enchantment : enchantments)
            grid.getChildren().add(new EnchantmentOverviewCard(enchantment, () -> open(enchantment)));
        content.getChildren().setAll(grid);
    }

    private VBox emptyState() {
        Label title = new Label(I18n.get("enchantment:items.no_results.title"));
        title.getStyleClass().add("enchantment-overview-empty-title");
        Label body = new Label(I18n.get("enchantment:items.no_results.description"));
        body.getStyleClass().add("enchantment-overview-empty-body");
        body.setWrapText(true);
        VBox box = new VBox(6, title, body);
        box.getStyleClass().add("enchantment-overview-empty");
        return box;
    }

    private void open(StudioMockEnchantment enchantment) {
        context.tabsState().openElement(enchantment.uniqueKey(), StudioRoute.ENCHANTMENT_MAIN);
        context.router().navigate(StudioRoute.ENCHANTMENT_MAIN);
    }
}


