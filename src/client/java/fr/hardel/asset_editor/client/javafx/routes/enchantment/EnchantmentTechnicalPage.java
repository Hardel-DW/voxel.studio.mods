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
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.UnaryOperator;

public final class EnchantmentTechnicalPage extends RegistryPage<Enchantment> {

    private record BehaviourTag(Identifier tagId, String title, String description) {}

    private record CostDef(String key,
                            Function<ElementEntry<Enchantment>, Integer> selector,
                            java.util.function.IntFunction<UnaryOperator<Enchantment>> mutation) {}

    private static final List<BehaviourTag> BEHAVIOUR_TAGS = List.of(
            behaviour(EnchantmentMutations.CURSE_TAG),
            behaviour(EnchantmentMutations.DOUBLE_TRADE_PRICE_TAG),
            behaviour(EnchantmentMutations.PREVENTS_BEE_SPAWNS_WHEN_MINING_TAG),
            behaviour(EnchantmentMutations.PREVENTS_DECORATED_POT_SHATTERING_TAG),
            behaviour(EnchantmentMutations.PREVENTS_ICE_MELTING_TAG),
            behaviour(EnchantmentMutations.PREVENTS_INFESTED_SPAWNS_TAG),
            behaviour(EnchantmentMutations.SMELTS_LOOT_TAG));

    private static final List<CostDef> COST_FIELDS = List.of(
            new CostDef("minCostBase",
                    entry -> entry.data().definition().minCost().base(),
                    EnchantmentMutations::minCostBase),
            new CostDef("minCostPerLevelAboveFirst",
                    entry -> entry.data().definition().minCost().perLevelAboveFirst(),
                    EnchantmentMutations::minCostPerLevel),
            new CostDef("maxCostBase",
                    entry -> entry.data().definition().maxCost().base(),
                    EnchantmentMutations::maxCostBase),
            new CostDef("maxCostPerLevelAboveFirst",
                    entry -> entry.data().definition().maxCost().perLevelAboveFirst(),
                    EnchantmentMutations::maxCostPerLevel));

    public EnchantmentTechnicalPage(StudioContext context) {
        super(context, Registries.ENCHANTMENT, "enchantment-subpage-scroll", 64, new Insets(32));
    }

    @Override
    protected void buildContent() {
        Section behaviour = new Section("enchantment:section.technical.description");
        ResponsiveGrid tagGrid = new ResponsiveGrid(ResponsiveGrid.fixed(1))
                .atLeast(StudioBreakpoint.LG, ResponsiveGrid.fixed(1, 1));

        for (BehaviourTag field : BEHAVIOUR_TAGS) {
            SwitchCard sw = SwitchCard.literal(field.title(), field.description());

            var selector = select(entry -> entry.tags().contains(field.tagId()));
            selector.subscribe(sw::setValue);
            if (selector.get() != null) sw.setValue(selector.get());

            sw.valueProperty().addListener((obs, o, v) -> {
                if (o == null || v == null || o.equals(v)) return;
                if (v.equals(selector.get())) return;
                applyTagToggle(field.tagId());
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
                var result = applyAction(field.mutation().apply(v.intValue()));
                if (!result.isApplied()) range.setValue(selector.get());
            });

            costGrid.addItem(range);
        }
        costs.addContent(costGrid);

        content().getChildren().setAll(behaviour, costs, buildEffectsSection());
    }

    private VBox buildEffectsSection() {
        Section section = new Section("enchantment:technical.effects.title");
        var effectsSelector = select(entry -> EnchantmentMutations.availableEffects(entry.data()));
        List<String> effects = effectsSelector.get() == null ? List.of() : effectsSelector.get();

        if (effects.isEmpty()) {
            Label empty = new Label(net.minecraft.client.resources.language.I18n.get("enchantment:technical.empty_effects"));
            empty.setFont(VoxelFonts.of(VoxelFonts.Variant.REGULAR, 14));
            empty.setTextFill(VoxelColors.ZINC_400);
            empty.setPadding(new Insets(16, 0, 16, 0));
            empty.setMaxWidth(Double.MAX_VALUE);
            empty.setAlignment(Pos.CENTER);
            section.addContent(empty);
            return section;
        }

        ResponsiveGrid grid = new ResponsiveGrid(ResponsiveGrid.fixed(1))
                .atLeast(StudioBreakpoint.LG, ResponsiveGrid.fixed(1, 1));

        for (String effectId : effects) {
            SwitchCard card = SwitchCard.literal(labelOf(effectId), effectId);
            var selector = select(entry -> EnchantmentMutations.isEffectDisabled(entry, effectId));
            selector.subscribe(disabled -> {
                if (disabled != null) {
                    card.setValue(!disabled);
                }
            });
            if (selector.get() != null) {
                card.setValue(!selector.get());
            }

            card.valueProperty().addListener((obs, oldValue, newValue) -> {
                if (oldValue == null || newValue == null || oldValue.equals(newValue)) return;
                Boolean currentEnabled = selector.get() == null ? Boolean.TRUE : !selector.get();
                if (Boolean.valueOf(newValue).equals(currentEnabled)) return;

                var result = applyCustomAction(EnchantmentMutations.toggleDisabledEffect(effectId));
                if (!result.isApplied()) {
                    card.setValue(currentEnabled);
                }
            });
            grid.addItem(card);
        }

        section.addContent(grid);
        return section;
    }

    private static BehaviourTag behaviour(Identifier tagId) {
        String label = labelOf(tagId.getPath());
        return new BehaviourTag(tagId, label, tagId.toString());
    }

    private static String labelOf(String raw) {
        String clean = raw.contains(":") ? raw.substring(raw.indexOf(':') + 1) : raw;
        String[] parts = clean.split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) continue;
            if (!builder.isEmpty()) builder.append(' ');
            builder.append(part.substring(0, 1).toUpperCase(Locale.ROOT));
            builder.append(part.substring(1));
        }
        return builder.toString();
    }
}
