package fr.hardel.asset_editor.client.javafx.routes.enchantment;

import fr.hardel.asset_editor.client.javafx.VoxelColors;
import fr.hardel.asset_editor.client.javafx.VoxelFonts;
import fr.hardel.asset_editor.client.javafx.components.ui.ResponsiveGrid;
import fr.hardel.asset_editor.client.javafx.components.ui.ToolRange;
import fr.hardel.asset_editor.client.javafx.components.ui.ToolSection;
import fr.hardel.asset_editor.client.javafx.components.ui.ToolSwitch;
import fr.hardel.asset_editor.client.javafx.lib.StudioContext;
import fr.hardel.asset_editor.client.javafx.lib.data.StudioBreakpoint;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import net.minecraft.client.resources.language.I18n;

import java.util.List;

public final class EnchantmentTechnicalPage extends VBox {

    private static final List<String> BEHAVIOUR_FIELDS = List.of(
            "smelts_loot",
            "prevent_ice_melting",
            "prevent_infested_block_spawning",
            "prevent_bee_spawning",
            "prevent_pot_shattering",
            "price_doubled");

    private static final List<String> COST_FIELDS = List.of(
            "minCostBase",
            "minCostPerLevelAboveFirst",
            "maxCostBase",
            "maxCostPerLevelAboveFirst");

    private final VBox content = new VBox(64);

    public EnchantmentTechnicalPage(StudioContext context) {
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("enchantment-subpage-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        getChildren().add(scroll);
        content.setPadding(new Insets(32));
        context.tabsState().currentElementIdProperty().addListener((obs, o, v) -> refresh());
        refresh();
    }

    private void refresh() {
        content.getChildren().setAll(
                buildBehaviourSection(),
                buildCostsSection(),
                buildEffectsSection());
    }

    private ToolSection buildBehaviourSection() {
        ToolSection section = new ToolSection("enchantment:section.technical.description");
        ResponsiveGrid grid = buildTwoColGrid();

        for (String field : BEHAVIOUR_FIELDS) {
            ToolSwitch sw = new ToolSwitch(
                    "enchantment:technical." + field + ".title",
                    "enchantment:technical." + field + ".description");
            grid.addItem(sw);
        }

        section.addContent(grid);
        return section;
    }

    private ToolSection buildCostsSection() {
        ToolSection section = new ToolSection("enchantment:section.costs");
        ResponsiveGrid grid = buildTwoColGrid();

        for (String field : COST_FIELDS) {
            grid.addItem(new ToolRange("enchantment:global." + field + ".title", 0, 100, 1, 0));
        }

        section.addContent(grid);
        return section;
    }

    private VBox buildEffectsSection() {
        ToolSection section = new ToolSection("enchantment:technical.effects.title");

        Label empty = new Label(I18n.get("enchantment:technical.empty_effects"));
        empty.setFont(VoxelFonts.rubik(VoxelFonts.Rubik.REGULAR, 14));
        empty.setTextFill(VoxelColors.ZINC_400);
        empty.setPadding(new Insets(16, 0, 16, 0));
        empty.setMaxWidth(Double.MAX_VALUE);
        empty.setAlignment(javafx.geometry.Pos.CENTER);

        section.addContent(empty);
        return section;
    }

    private ResponsiveGrid buildTwoColGrid() {
        return new ResponsiveGrid(ResponsiveGrid.fixed(1))
            .atLeast(StudioBreakpoint.LG, ResponsiveGrid.fixed(1, 1));
    }

}
