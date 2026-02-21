package fr.hardel.asset_editor.client.javafx.components.layout.editor;

import fr.hardel.asset_editor.client.javafx.context.StudioContext;
import fr.hardel.asset_editor.client.javafx.routes.EmptyPage;
import fr.hardel.asset_editor.client.javafx.routes.enchantment.EnchantmentMainPage;
import fr.hardel.asset_editor.client.javafx.routes.enchantment.EnchantmentOverviewPage;
import fr.hardel.asset_editor.client.javafx.routes.StudioRoute;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public final class EnchantmentLayout extends HBox {

    private final StudioContext context;
    private final EnchantmentOverviewPage overviewPage;
    private final EnchantmentMainPage mainPage;
    private final EmptyPage emptyPage = new EmptyPage();
    private final StackPane outlet = new StackPane();

    public EnchantmentLayout(StudioContext context) {
        this.context = context;
        getStyleClass().add("enchantment-layout");

        overviewPage = new EnchantmentOverviewPage(context);
        mainPage = new EnchantmentMainPage(context);

        EnchantmentSidebar sidebar = new EnchantmentSidebar(context);
        VBox main = new VBox(new EnchantmentHeader(context), outlet);
        main.getStyleClass().add("enchantment-main");
        VBox.setVgrow(outlet, Priority.ALWAYS);
        HBox.setHgrow(main, Priority.ALWAYS);

        getChildren().addAll(sidebar, main);

        context.router().routeProperty().addListener((obs, oldValue, newValue) -> refresh());
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
}



