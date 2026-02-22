package fr.hardel.asset_editor.client.javafx.components.page.recipe;

import fr.hardel.asset_editor.client.javafx.components.layout.editor.EditorHeader;
import fr.hardel.asset_editor.client.javafx.components.layout.editor.EditorSidebar;
import fr.hardel.asset_editor.client.javafx.components.ui.tree.TreeController;
import fr.hardel.asset_editor.client.javafx.lib.StudioContext;
import fr.hardel.asset_editor.client.javafx.lib.data.StudioConcept;
import fr.hardel.asset_editor.client.javafx.routes.EmptyPage;
import fr.hardel.asset_editor.client.javafx.routes.StudioRoute;
import fr.hardel.asset_editor.client.javafx.routes.recipe.RecipeMainPage;
import fr.hardel.asset_editor.client.javafx.routes.recipe.RecipeOverviewPage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import net.minecraft.resources.Identifier;

import java.util.List;

public final class RecipeLayout extends HBox {

    private static final Identifier RECIPE_ICON = Identifier.fromNamespaceAndPath(
            "asset_editor",
            "textures/features/block/crafting_table.png");

    private final StudioContext context;
    private final RecipeOverviewPage overviewPage;
    private final RecipeMainPage mainPage;
    private final EmptyPage emptyPage = new EmptyPage();
    private final StackPane outlet = new StackPane();

    public RecipeLayout(StudioContext context) {
        this.context = context;
        getStyleClass().add("enchantment-layout");
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        TreeController tree = new TreeController(context, new TreeController.Config(
                StudioRoute.RECIPE_OVERVIEW,
                StudioRoute.RECIPE_MAIN,
                StudioRoute.CHANGES_MAIN,
                StudioConcept.RECIPE.registry(),
                StudioConcept.RECIPE.tabRoutes(),
                RecipeTreeBuilder.build(List.of()),
                RECIPE_ICON,
                RecipeTreeBuilder.folderIcons(),
                false,
                null,
                null,
                null,
                () -> 0
        ));

        overviewPage = new RecipeOverviewPage(context);
        mainPage = new RecipeMainPage();

        EditorSidebar sidebar = new EditorSidebar(
                context,
                tree,
                "recipe:overview.title",
                RECIPE_ICON,
                List.of()
        );

        VBox main = new VBox(new EditorHeader(context, tree, StudioConcept.RECIPE, false, null), outlet);
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
        if (route == StudioRoute.RECIPE_OVERVIEW) {
            outlet.getChildren().setAll(overviewPage);
            return;
        }
        if (route == StudioRoute.RECIPE_MAIN) {
            outlet.getChildren().setAll(mainPage);
            return;
        }
        outlet.getChildren().setAll(emptyPage);
    }
}
