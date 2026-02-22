package fr.hardel.asset_editor.client.javafx.components.page.enchantment;

import fr.hardel.asset_editor.client.javafx.components.layout.editor.EditorHeader;
import fr.hardel.asset_editor.client.javafx.components.layout.editor.EditorSidebar;
import fr.hardel.asset_editor.client.javafx.components.ui.ToggleGroup;
import fr.hardel.asset_editor.client.javafx.components.ui.tree.TreeController;
import fr.hardel.asset_editor.client.javafx.components.ui.tree.TreeNodeModel;
import fr.hardel.asset_editor.client.javafx.lib.StudioContext;
import fr.hardel.asset_editor.client.javafx.lib.data.StudioConcept;
import fr.hardel.asset_editor.client.javafx.lib.data.StudioSidebarView;
import fr.hardel.asset_editor.client.javafx.routes.StudioRoute;
import fr.hardel.asset_editor.client.javafx.routes.EmptyPage;
import fr.hardel.asset_editor.client.javafx.routes.enchantment.EnchantmentMainPage;
import fr.hardel.asset_editor.client.javafx.routes.enchantment.EnchantmentOverviewPage;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Map;

public final class EnchantmentLayout extends HBox {

    private static final Identifier ENCHANTED_BOOK_ICON = Identifier.fromNamespaceAndPath(
            "asset_editor",
            "textures/features/item/enchanted_book.png");

    private final StudioContext context;
    private final TreeController tree;
    private final EnchantmentOverviewPage overviewPage;
    private final EnchantmentMainPage mainPage;
    private final EmptyPage emptyPage = new EmptyPage();
    private final StackPane outlet = new StackPane();

    public EnchantmentLayout(StudioContext context) {
        this.context = context;
        getStyleClass().add("enchantment-layout");
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        overviewPage = new EnchantmentOverviewPage(context);
        mainPage = new EnchantmentMainPage(context);

        StudioSidebarView initialView = context.uiState().sidebarView();
        tree = new TreeController(context, new TreeController.Config(
                StudioRoute.ENCHANTMENT_OVERVIEW,
                StudioRoute.ENCHANTMENT_MAIN,
                StudioRoute.CHANGES_MAIN,
                StudioConcept.ENCHANTMENT.registry(),
                StudioConcept.ENCHANTMENT.tabRoutes(),
                buildTree(initialView),
                ENCHANTED_BOOK_ICON,
                folderIcons(initialView),
                initialView == StudioSidebarView.SLOTS,
                null,
                null,
                null,
                () -> 0
        ));

        ToggleGroup toggleGroup = buildSidebarToggle();
        Node sidebar = new EditorSidebar(
                context,
                tree,
                "enchantment:overview.title",
                ENCHANTED_BOOK_ICON,
                List.of(toggleGroup));

        VBox main = new VBox(new EditorHeader(context, tree, StudioConcept.ENCHANTMENT, true, StudioRoute.ENCHANTMENT_SIMULATION), outlet);
        main.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        main.getStyleClass().add("enchantment-main");
        VBox.setVgrow(outlet, Priority.ALWAYS);
        HBox.setHgrow(main, Priority.ALWAYS);

        getChildren().addAll(sidebar, main);

        context.router().routeProperty().addListener((obs, oldValue, newValue) -> refresh());
        context.uiState().sidebarViewProperty().addListener((obs, oldValue, newValue) -> {
            tree.setTree(buildTree(newValue));
            tree.setFolderIcons(folderIcons(newValue));
            tree.setDisableAutoExpand(newValue == StudioSidebarView.SLOTS);
        });
        refresh();
    }

    private void refresh() {
        StudioRoute route = context.router().currentRoute();
        if (route == StudioRoute.ENCHANTMENT_OVERVIEW) {
            outlet.getChildren().setAll(overviewPage);
            return;
        }
        if (route == StudioRoute.ENCHANTMENT_MAIN) {
            outlet.getChildren().setAll(mainPage);
            return;
        }
        outlet.getChildren().setAll(emptyPage);
    }

    private ToggleGroup buildSidebarToggle() {
        ToggleGroup toggleGroup = new ToggleGroup(
                () -> context.uiState().sidebarView().name().toLowerCase(),
                value -> {
                    StudioSidebarView next = switch (value) {
                        case "items" -> StudioSidebarView.ITEMS;
                        case "exclusive" -> StudioSidebarView.EXCLUSIVE;
                        default -> StudioSidebarView.SLOTS;
                    };
                    context.uiState().setSidebarView(next);
                });
        toggleGroup.getStyleClass().add("enchantment-sidebar-toggle");
        javafx.scene.layout.VBox.setMargin(toggleGroup, new javafx.geometry.Insets(16, 0, 0, 0));
        toggleGroup.addOption("slots", net.minecraft.client.resources.language.I18n.get("enchantment:overview.sidebar.slots"));
        toggleGroup.addOption("items", net.minecraft.client.resources.language.I18n.get("enchantment:overview.sidebar.items"));
        toggleGroup.addOption("exclusive", net.minecraft.client.resources.language.I18n.get("enchantment:overview.sidebar.exclusive"));
        context.uiState().sidebarViewProperty().addListener((obs, oldValue, newValue) -> toggleGroup.refresh());
        return toggleGroup;
    }

    private TreeNodeModel buildTree(StudioSidebarView view) {
        return EnchantmentTreeBuilder.build(context.repository().enchantments(), view, 61);
    }

    private Map<String, Identifier> folderIcons(StudioSidebarView view) {
        if (view == StudioSidebarView.SLOTS) return EnchantmentTreeBuilder.slotFolderIcons();
        if (view == StudioSidebarView.ITEMS) return EnchantmentTreeBuilder.itemFolderIcons(61);
        return Map.of();
    }
}



