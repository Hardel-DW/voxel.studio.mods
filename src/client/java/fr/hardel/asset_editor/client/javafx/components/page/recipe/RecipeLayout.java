package fr.hardel.asset_editor.client.javafx.components.page.recipe;

import fr.hardel.asset_editor.client.javafx.components.layout.editor.ConceptLayout;
import fr.hardel.asset_editor.client.javafx.components.ui.tree.TreeController;
import fr.hardel.asset_editor.client.javafx.lib.StudioContext;
import fr.hardel.asset_editor.client.javafx.lib.data.StudioConcept;
import fr.hardel.asset_editor.client.javafx.routes.EmptyPage;
import fr.hardel.asset_editor.client.javafx.routes.StudioRoute;
import fr.hardel.asset_editor.client.javafx.routes.recipe.RecipeMainPage;
import fr.hardel.asset_editor.client.javafx.routes.recipe.RecipeOverviewPage;
import javafx.scene.Node;
import net.minecraft.resources.Identifier;

import java.util.List;

public final class RecipeLayout {

    private static final Identifier ICON = Identifier.fromNamespaceAndPath("asset_editor", "textures/features/block/crafting_table.png");

    public static ConceptLayout create(StudioContext context) {
        var treeConfig = new TreeController.Config(
                StudioRoute.RECIPE_OVERVIEW,
                StudioRoute.RECIPE_MAIN,
                StudioRoute.CHANGES_MAIN,
                StudioConcept.RECIPE.registry(),
                StudioConcept.RECIPE.tabRoutes(),
                RecipeTreeBuilder.build(List.of()),
                ICON, RecipeTreeBuilder.folderIcons(), false,
                null, null, null, () -> 0);

        return new ConceptLayout(context, new ConceptLayout.Config(
                StudioConcept.RECIPE, ICON,
                "recipe:overview.title",
                treeConfig, null, false,
                route -> createPage(context, route),
                List.of()));
    }

    private static Node createPage(StudioContext context, StudioRoute route) {
        return switch (route) {
            case RECIPE_OVERVIEW -> new RecipeOverviewPage(context);
            case RECIPE_MAIN -> new RecipeMainPage();
            default -> new EmptyPage();
        };
    }

    private RecipeLayout() {}
}
