package fr.hardel.asset_editor.client.javafx.routes.enchantment;

import fr.hardel.asset_editor.client.javafx.VoxelColors;
import fr.hardel.asset_editor.client.javafx.VoxelFonts;
import fr.hardel.asset_editor.client.javafx.components.ui.Range;
import fr.hardel.asset_editor.client.javafx.components.ui.ResponsiveGrid;
import fr.hardel.asset_editor.client.javafx.components.ui.Section;
import fr.hardel.asset_editor.client.javafx.components.ui.SwitchCard;
import fr.hardel.asset_editor.client.javafx.lib.RegistryPage;
import fr.hardel.asset_editor.client.javafx.lib.StudioContext;
import fr.hardel.asset_editor.client.javafx.lib.action.EnchantmentMutations;
import fr.hardel.asset_editor.client.javafx.lib.data.StudioBreakpoint;
import fr.hardel.asset_editor.client.javafx.lib.store.RegistryElementStore.ElementEntry;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantment.EnchantmentDefinition;
import net.minecraft.world.item.enchantment.Enchantment.Cost;

import java.util.List;
import java.util.function.Function;


public final class EnchantmentTechnicalPage extends RegistryPage<Enchantment> {

    private static final List<String> BEHAVIOUR_TAGS = List.of(
            "smelts_loot",
            "prevent_ice_melting",
            "prevent_infested_block_spawning",
            "prevent_bee_spawning",
            "prevent_pot_shattering",
            "price_doubled");

    private record CostDef(String key,
                            Function<ElementEntry<Enchantment>, Integer> selector,
                            CostMutator mutator) {}

    @FunctionalInterface
    private interface CostMutator {
        Enchantment apply(Enchantment e, int value);
    }

    private static final List<CostDef> COST_FIELDS = List.of(
            new CostDef("minCostBase",
                    entry -> entry.data().definition().minCost().base(),
                    (e, v) -> EnchantmentMutations.withDefinition(e, d -> new EnchantmentDefinition(
                            d.supportedItems(), d.primaryItems(), d.weight(), d.maxLevel(),
                            new Cost(v, d.minCost().perLevelAboveFirst()), d.maxCost(), d.anvilCost(), d.slots()))),
            new CostDef("minCostPerLevelAboveFirst",
                    entry -> entry.data().definition().minCost().perLevelAboveFirst(),
                    (e, v) -> EnchantmentMutations.withDefinition(e, d -> new EnchantmentDefinition(
                            d.supportedItems(), d.primaryItems(), d.weight(), d.maxLevel(),
                            new Cost(d.minCost().base(), v), d.maxCost(), d.anvilCost(), d.slots()))),
            new CostDef("maxCostBase",
                    entry -> entry.data().definition().maxCost().base(),
                    (e, v) -> EnchantmentMutations.withDefinition(e, d -> new EnchantmentDefinition(
                            d.supportedItems(), d.primaryItems(), d.weight(), d.maxLevel(),
                            d.minCost(), new Cost(v, d.maxCost().perLevelAboveFirst()), d.anvilCost(), d.slots()))),
            new CostDef("maxCostPerLevelAboveFirst",
                    entry -> entry.data().definition().maxCost().perLevelAboveFirst(),
                    (e, v) -> EnchantmentMutations.withDefinition(e, d -> new EnchantmentDefinition(
                            d.supportedItems(), d.primaryItems(), d.weight(), d.maxLevel(),
                            d.minCost(), new Cost(d.maxCost().base(), v), d.anvilCost(), d.slots()))));

    public EnchantmentTechnicalPage(StudioContext context) {
        super(context, Registries.ENCHANTMENT, "enchantment-subpage-scroll", 64, new Insets(32));
    }

    @Override
    protected void buildContent() {
        Section behaviour = new Section("enchantment:section.technical.description");
        ResponsiveGrid tagGrid = new ResponsiveGrid(ResponsiveGrid.fixed(1))
                .atLeast(StudioBreakpoint.LG, ResponsiveGrid.fixed(1, 1));

        for (String field : BEHAVIOUR_TAGS) {
            Identifier tagId = Identifier.fromNamespaceAndPath("minecraft", field);
            SwitchCard sw = new SwitchCard(
                    "enchantment:technical." + field + ".title",
                    "enchantment:technical." + field + ".description");

            var selector = select(entry -> entry.tags().contains(tagId));
            selector.subscribe(sw::setValue);
            if (selector.get() != null) sw.setValue(selector.get());

            sw.valueProperty().addListener((obs, o, v) -> {
                if (o == null || v == null || o.equals(v)) return;
                if (v.equals(selector.get())) return;
                context().gateway().toggleTag(Registries.ENCHANTMENT, currentId(), tagId);
            });

            tagGrid.addItem(sw);
        }
        behaviour.addContent(tagGrid);

        Section costs = new Section("enchantment:section.costs");
        ResponsiveGrid costGrid = new ResponsiveGrid(ResponsiveGrid.fixed(1))
                .atLeast(StudioBreakpoint.LG, ResponsiveGrid.fixed(1, 1));

        for (CostDef field : COST_FIELDS) {
            Range range = new Range("enchantment:global." + field.key() + ".title", 0, 100, 1, 0);

            var selector = select(field.selector());
            selector.subscribe(val -> { if (val != null) range.setValue(val); });
            if (selector.get() != null) range.setValue(selector.get());

            range.valueProperty().addListener((obs, o, v) -> {
                if (o == null || v == null || o.intValue() == v.intValue()) return;
                if (Integer.valueOf(v.intValue()).equals(selector.get())) return;
                context().gateway().apply(Registries.ENCHANTMENT, currentId(),
                        e -> field.mutator().apply(e, v.intValue()));
            });

            costGrid.addItem(range);
        }
        costs.addContent(costGrid);

        content().getChildren().setAll(behaviour, costs, buildEffectsSection());
    }

    private VBox buildEffectsSection() {
        Section section = new Section("enchantment:technical.effects.title");
        Label empty = new Label(I18n.get("enchantment:technical.empty_effects"));
        empty.setFont(VoxelFonts.of(VoxelFonts.Variant.REGULAR, 14));
        empty.setTextFill(VoxelColors.ZINC_400);
        empty.setPadding(new Insets(16, 0, 16, 0));
        empty.setMaxWidth(Double.MAX_VALUE);
        empty.setAlignment(Pos.CENTER);
        section.addContent(empty);
        return section;
    }
}
