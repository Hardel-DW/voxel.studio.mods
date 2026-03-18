package fr.hardel.asset_editor.client.javafx.components.page.loot_table;

import fr.hardel.asset_editor.client.javafx.components.layout.editor.ConceptLayout;
import fr.hardel.asset_editor.client.javafx.components.ui.tree.TreeController;
import fr.hardel.asset_editor.client.javafx.lib.StudioContext;
import fr.hardel.asset_editor.client.javafx.lib.data.StudioConcept;
import fr.hardel.asset_editor.client.javafx.routes.EmptyPage;
import fr.hardel.asset_editor.client.javafx.routes.StudioRoute;
import fr.hardel.asset_editor.client.javafx.routes.loot.LootTableMainPage;
import fr.hardel.asset_editor.client.javafx.routes.loot.LootTableOverviewPage;
import javafx.scene.Node;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Map;

public final class LootTableLayout {

    private static final Identifier ICON = Identifier.fromNamespaceAndPath("minecraft", "textures/studio/concept/loot_table.png");

    public static ConceptLayout create(StudioContext context) {
        var treeConfig = new TreeController.Config(
            StudioRoute.LOOT_TABLE_OVERVIEW,
            StudioRoute.LOOT_TABLE_MAIN,
            StudioRoute.CHANGES_MAIN,
            StudioConcept.LOOT_TABLE.registry(),
            StudioConcept.LOOT_TABLE.tabRoutes(),
            LootTableTreeBuilder.build(List.of()),
            ICON, Map.of(), false,
            null, null, null, () -> 0);

        return new ConceptLayout(context, new ConceptLayout.Config(
            StudioConcept.LOOT_TABLE, ICON,
            "loot:overview.title",
            treeConfig, null, false,
            route -> createPage(context, route),
            List.of()));
    }

    private static Node createPage(StudioContext context, StudioRoute route) {
        return switch (route) {
            case LOOT_TABLE_OVERVIEW -> new LootTableOverviewPage(context);
            case LOOT_TABLE_MAIN, LOOT_TABLE_POOLS -> new LootTableMainPage();
            default -> new EmptyPage();
        };
    }

    private LootTableLayout() {}
}
