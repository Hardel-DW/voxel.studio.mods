package fr.hardel.asset_editor.client.javafx.routes.enchantment;

import fr.hardel.asset_editor.client.javafx.VoxelColors;
import fr.hardel.asset_editor.client.javafx.VoxelFonts;
import fr.hardel.asset_editor.client.javafx.components.ui.Range;
import fr.hardel.asset_editor.client.javafx.components.ui.ResponsiveGrid;
import fr.hardel.asset_editor.client.javafx.components.ui.Section;
import fr.hardel.asset_editor.client.javafx.components.ui.SwitchCard;
import fr.hardel.asset_editor.client.javafx.lib.RegistryPage;
import fr.hardel.asset_editor.client.javafx.lib.StudioContext;
import fr.hardel.asset_editor.client.javafx.lib.action.EnchantmentActions;
import fr.hardel.asset_editor.client.javafx.lib.data.StudioBreakpoint;
import fr.hardel.asset_editor.client.javafx.lib.store.RegistryElementStore.ElementEntry;
import fr.hardel.asset_editor.client.javafx.lib.utils.TextUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.List;
import java.util.function.Function;
import java.util.function.UnaryOperator;

public final class EnchantmentTechnicalPage extends RegistryPage<Enchantment> {

    private record BehaviourTag(Identifier tagId, String title, String description) {
    }

    private record CostDef(String key,
            Function<ElementEntry<Enchantment>, Integer> selector,
            java.util.function.IntFunction<UnaryOperator<Enchantment>> mutation) {
    }

    private static final List<BehaviourTag> BEHAVIOUR_TAGS = List.of(
            new BehaviourTag(EnchantmentActions.CURSE_TAG, "enchantment:find.curse.title",
                    "enchantment:find.curse.description"),
            new BehaviourTag(EnchantmentActions.DOUBLE_TRADE_PRICE_TAG, "enchantment:technical.price_doubled.title",
                    "enchantment:technical.price_doubled.description"),
            new BehaviourTag(EnchantmentActions.PREVENTS_BEE_SPAWNS_WHEN_MINING_TAG,
                    "enchantment:technical.prevent_bee_spawning.title",
                    "enchantment:technical.prevent_bee_spawning.description"),
            new BehaviourTag(EnchantmentActions.PREVENTS_DECORATED_POT_SHATTERING_TAG,
                    "enchantment:technical.prevent_pot_shattering.title",
                    "enchantment:technical.prevent_pot_shattering.description"),
            new BehaviourTag(EnchantmentActions.PREVENTS_ICE_MELTING_TAG,
                    "enchantment:technical.prevent_ice_melting.title",
                    "enchantment:technical.prevent_ice_melting.description"),
            new BehaviourTag(EnchantmentActions.PREVENTS_INFESTED_SPAWNS_TAG,
                    "enchantment:technical.prevent_infested_block_spawning.title",
                    "enchantment:technical.prevent_infested_block_spawning.description"),
            new BehaviourTag(EnchantmentActions.SMELTS_LOOT_TAG, "enchantment:technical.smelts_loot.title",
                    "enchantment:technical.smelts_loot.description"));

    private static final List<CostDef> COST_FIELDS = List.of(
            new CostDef("minCostBase",
                    entry -> entry.data().definition().minCost().base(),
                    EnchantmentActions::minCostBase),
            new CostDef("minCostPerLevelAboveFirst",
                    entry -> entry.data().definition().minCost().perLevelAboveFirst(),
                    EnchantmentActions::minCostPerLevel),
            new CostDef("maxCostBase",
                    entry -> entry.data().definition().maxCost().base(),
                    EnchantmentActions::maxCostBase),
            new CostDef("maxCostPerLevelAboveFirst",
                    entry -> entry.data().definition().maxCost().perLevelAboveFirst(),
                    EnchantmentActions::maxCostPerLevel));

    public EnchantmentTechnicalPage(StudioContext context) {
        super(context, Registries.ENCHANTMENT, "enchantment-subpage-scroll", 64, new Insets(32));
    }

    @Override
    protected void buildContent() {
        Section behaviour = new Section("enchantment:section.technical.description");
        ResponsiveGrid tagGrid = new ResponsiveGrid(ResponsiveGrid.fixed(1))
                .atLeast(StudioBreakpoint.LG, ResponsiveGrid.fixed(1, 1));

        for (BehaviourTag field : BEHAVIOUR_TAGS) {
            SwitchCard sw = SwitchCard.literal(I18n.get(field.title()), I18n.get(field.description()));
            bindToggle(sw.valueProperty(), entry -> entry.tags().contains(field.tagId()),
                    () -> applyTagToggle(field.tagId()));
            tagGrid.addItem(sw);
        }
        behaviour.addContent(tagGrid);

        Section costs = new Section("enchantment:section.costs");
        ResponsiveGrid costGrid = new ResponsiveGrid(ResponsiveGrid.fixed(1))
                .atLeast(StudioBreakpoint.LG, ResponsiveGrid.fixed(1, 1));

        for (CostDef field : COST_FIELDS) {
            Range range = new Range("enchantment:global." + field.key() + ".title", 0, 100, 1, 0);
            bindInt(range.valueProperty(), field.selector(), field.mutation());
            costGrid.addItem(range);
        }
        costs.addContent(costGrid);

        content().getChildren().setAll(behaviour, costs, buildEffectsSection());
    }

    private VBox buildEffectsSection() {
        Section section = new Section("enchantment:technical.effects.title");
        var effectsSelector = select(entry -> EnchantmentActions.availableEffects(entry.data()));
        List<String> effects = effectsSelector.get() == null ? List.of() : effectsSelector.get();

        if (effects.isEmpty()) {
            Label empty = new Label(I18n.get("enchantment:technical.empty_effects"));
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
            String effectKey = "effects:" + effectId;
            String effectLabel = I18n.exists(effectKey) ? I18n.get(effectKey) : TextUtils.humanize(effectId);
            SwitchCard card = SwitchCard.literal(effectLabel, effectId);
            bindToggle(card.valueProperty(),
                    entry -> !EnchantmentActions.isEffectDisabled(entry, effectId),
                    () -> applyCustomAction(EnchantmentActions.toggleDisabledEffect(effectId)));
            grid.addItem(card);
        }

        section.addContent(grid);
        return section;
    }

}
