package fr.hardel.asset_editor.client.javafx.components.page.enchantment;

import fr.hardel.asset_editor.client.javafx.components.layout.editor.ConceptLayout;
import fr.hardel.asset_editor.client.javafx.components.ui.ToggleGroup;
import fr.hardel.asset_editor.client.javafx.components.ui.tree.TreeController;
import fr.hardel.asset_editor.client.javafx.lib.StudioContext;
import fr.hardel.asset_editor.client.javafx.lib.data.StudioConcept;
import fr.hardel.asset_editor.client.javafx.lib.data.StudioSidebarView;
import fr.hardel.asset_editor.client.javafx.routes.EmptyPage;
import fr.hardel.asset_editor.client.javafx.routes.StudioRoute;
import fr.hardel.asset_editor.client.javafx.routes.enchantment.*;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.layout.VBox;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Map;

public final class EnchantmentLayout {

    private static final Identifier ICON = Identifier.fromNamespaceAndPath("minecraft", "textures/studio/concept/enchantment.png");

    public static ConceptLayout create(StudioContext context) {
        StudioSidebarView initialView = context.uiState().sidebarView();

        var treeConfig = new TreeController.Config(
            StudioRoute.ENCHANTMENT_OVERVIEW,
            StudioRoute.ENCHANTMENT_MAIN,
            StudioRoute.CHANGES_MAIN,
            StudioConcept.ENCHANTMENT.registry(),
            StudioConcept.ENCHANTMENT.tabRoutes(),
            EnchantmentTreeBuilder.build(context.allEntries(Registries.ENCHANTMENT), initialView),
            ICON,
            folderIcons(initialView),
            initialView == StudioSidebarView.SLOTS,
            null, null, null, () -> 0);

        ConceptLayout layout = new ConceptLayout(context, new ConceptLayout.Config(
            StudioConcept.ENCHANTMENT, ICON,
            "enchantment:overview.title",
            treeConfig,
            StudioRoute.ENCHANTMENT_SIMULATION,
            true,
            route -> createPage(context, route),
            List.of(buildToggle(context))));

        Runnable rebuildTree = () -> {
            StudioSidebarView v = context.uiState().sidebarView();
            layout.tree().setTree(EnchantmentTreeBuilder.build(context.allEntries(Registries.ENCHANTMENT), v));
            layout.tree().setFolderIcons(folderIcons(v));
            layout.tree().setDisableAutoExpand(v == StudioSidebarView.SLOTS);
        };

        context.uiState().sidebarViewProperty().addListener((obs, o, v) -> rebuildTree.run());
        context.elementStore().subscribeRegistry(Registries.ENCHANTMENT, rebuildTree);

        return layout;
    }

    private static Node createPage(StudioContext context, StudioRoute route) {
        return switch (route) {
            case ENCHANTMENT_OVERVIEW -> new EnchantmentOverviewPage(context);
            case ENCHANTMENT_MAIN -> new EnchantmentMainPage(context);
            case ENCHANTMENT_FIND -> new EnchantmentFindPage(context);
            case ENCHANTMENT_SLOTS -> new EnchantmentSlotsPage(context);
            case ENCHANTMENT_ITEMS -> new EnchantmentItemsPage(context);
            case ENCHANTMENT_EXCLUSIVE -> new EnchantmentExclusivePage(context);
            case ENCHANTMENT_TECHNICAL -> new EnchantmentTechnicalPage(context);
            default -> new EmptyPage();
        };
    }

    private static ToggleGroup buildToggle(StudioContext context) {
        ToggleGroup toggle = new ToggleGroup(
            () -> context.uiState().sidebarView().name().toLowerCase(),
            value -> context.uiState().setSidebarView(switch (value) {
                case "items" -> StudioSidebarView.ITEMS;
                case "exclusive" -> StudioSidebarView.EXCLUSIVE;
                default -> StudioSidebarView.SLOTS;
            }));
        toggle.setMaxWidth(Double.MAX_VALUE);
        VBox.setMargin(toggle, new Insets(16, 0, 0, 0));
        toggle.addOption("slots", I18n.get("enchantment:overview.sidebar.slots"));
        toggle.addOption("items", I18n.get("enchantment:overview.sidebar.items"));
        toggle.addOption("exclusive", I18n.get("enchantment:overview.sidebar.exclusive"));
        context.uiState().sidebarViewProperty().addListener((obs, o, v) -> toggle.refresh());
        return toggle;
    }

    private static Map<String, Identifier> folderIcons(StudioSidebarView view) {
        if (view == StudioSidebarView.SLOTS)
            return EnchantmentTreeBuilder.slotFolderIcons();
        if (view == StudioSidebarView.ITEMS)
            return EnchantmentTreeBuilder.itemFolderIcons();
        return Map.of();
    }

    private EnchantmentLayout() {}
}
