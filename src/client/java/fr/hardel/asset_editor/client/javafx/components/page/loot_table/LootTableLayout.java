package fr.hardel.asset_editor.client.javafx.components.page.loot_table;

import fr.hardel.asset_editor.client.javafx.components.layout.editor.EditorHeader;
import fr.hardel.asset_editor.client.javafx.components.layout.editor.EditorSidebar;
import fr.hardel.asset_editor.client.javafx.components.ui.tree.TreeController;
import fr.hardel.asset_editor.client.javafx.lib.StudioContext;
import fr.hardel.asset_editor.client.javafx.lib.data.StudioConcept;
import fr.hardel.asset_editor.client.javafx.routes.EmptyPage;
import fr.hardel.asset_editor.client.javafx.routes.StudioRoute;
import fr.hardel.asset_editor.client.javafx.routes.loot.LootTableMainPage;
import fr.hardel.asset_editor.client.javafx.routes.loot.LootTableOverviewPage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Map;

public final class LootTableLayout extends HBox {

    private static final Identifier LOOT_ICON = Identifier.fromNamespaceAndPath(
            "asset_editor",
            "textures/features/item/bundle_close.png");

    private final StudioContext context;
    private final LootTableOverviewPage overviewPage;
    private final LootTableMainPage mainPage;
    private final EmptyPage emptyPage = new EmptyPage();
    private final StackPane outlet = new StackPane();

    public LootTableLayout(StudioContext context) {
        this.context = context;
        getStyleClass().add("enchantment-layout");
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        TreeController tree = new TreeController(context, new TreeController.Config(
                StudioRoute.LOOT_TABLE_OVERVIEW,
                StudioRoute.LOOT_TABLE_MAIN,
                StudioRoute.CHANGES_MAIN,
                StudioConcept.LOOT_TABLE.registry(),
                StudioConcept.LOOT_TABLE.tabRoutes(),
                LootTableTreeBuilder.build(List.of()),
                LOOT_ICON,
                Map.of(),
                false,
                null,
                null,
                null,
                () -> 0
        ));

        overviewPage = new LootTableOverviewPage(context);
        mainPage = new LootTableMainPage();

        EditorSidebar sidebar = new EditorSidebar(
                context,
                tree,
                "loot:overview.title",
                LOOT_ICON,
                List.of()
        );

        VBox main = new VBox(new EditorHeader(context, tree, StudioConcept.LOOT_TABLE, true, null), outlet);
        main.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        main.getStyleClass().add("enchantment-main");
        VBox.setVgrow(outlet, Priority.ALWAYS);
        HBox.setHgrow(main, Priority.ALWAYS);

        getChildren().addAll(sidebar, main);
        context.router().routeProperty().addListener((obs, oldValue, newValue) -> refresh());
        refresh();
    }

    private void refresh() {
        StudioRoute route = context.router().currentRoute();
        if (route == StudioRoute.LOOT_TABLE_OVERVIEW) {
            outlet.getChildren().setAll(overviewPage);
            return;
        }
        if (route == StudioRoute.LOOT_TABLE_MAIN || route == StudioRoute.LOOT_TABLE_POOLS) {
            outlet.getChildren().setAll(mainPage);
            return;
        }
        outlet.getChildren().setAll(emptyPage);
    }
}
