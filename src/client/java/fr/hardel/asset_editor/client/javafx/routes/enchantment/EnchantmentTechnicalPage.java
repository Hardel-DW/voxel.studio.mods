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
import fr.hardel.asset_editor.client.javafx.lib.StudioText;
import fr.hardel.asset_editor.store.ElementEntry;
import fr.hardel.asset_editor.network.EditorAction;
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
import java.util.function.IntFunction;
import java.util.function.UnaryOperator;

public final class EnchantmentTechnicalPage extends RegistryPage<Enchantment> {

    private record BehaviourTag(Identifier tagId) {
        String titleKey() { return "enchantment_tag:" + tagId; }
        String descKey() { return "enchantment_tag:" + tagId + ".desc"; }
    }

    private record CostDef(String key,
            Function<ElementEntry<Enchantment>, Integer> selector,
            IntFunction<UnaryOperator<Enchantment>> mutation,
            IntFunction<EditorAction> actionFactory) {
    }

    private static final List<BehaviourTag> BEHAVIOUR_TAGS = List.of(
            new BehaviourTag(EnchantmentActions.CURSE_TAG),
            new BehaviourTag(EnchantmentActions.DOUBLE_TRADE_PRICE_TAG),
            new BehaviourTag(EnchantmentActions.PREVENTS_BEE_SPAWNS_WHEN_MINING_TAG),
            new BehaviourTag(EnchantmentActions.PREVENTS_DECORATED_POT_SHATTERING_TAG),
            new BehaviourTag(EnchantmentActions.PREVENTS_ICE_MELTING_TAG),
            new BehaviourTag(EnchantmentActions.PREVENTS_INFESTED_SPAWNS_TAG),
            new BehaviourTag(EnchantmentActions.SMELTS_LOOT_TAG));

    private static final List<CostDef> COST_FIELDS = List.of(
            new CostDef("minCostBase",
                    entry -> entry.data().definition().minCost().base(),
                    EnchantmentActions::minCostBase,
                    v -> new EditorAction.SetIntField("min_cost_base", v)),
            new CostDef("minCostPerLevelAboveFirst",
                    entry -> entry.data().definition().minCost().perLevelAboveFirst(),
                    EnchantmentActions::minCostPerLevel,
                    v -> new EditorAction.SetIntField("min_cost_per_level", v)),
            new CostDef("maxCostBase",
                    entry -> entry.data().definition().maxCost().base(),
                    EnchantmentActions::maxCostBase,
                    v -> new EditorAction.SetIntField("max_cost_base", v)),
            new CostDef("maxCostPerLevelAboveFirst",
                    entry -> entry.data().definition().maxCost().perLevelAboveFirst(),
                    EnchantmentActions::maxCostPerLevel,
                    v -> new EditorAction.SetIntField("max_cost_per_level", v)));

    public EnchantmentTechnicalPage(StudioContext context) {
        super(context, Registries.ENCHANTMENT, "enchantment-subpage-scroll", 64, new Insets(32));
    }

    @Override
    protected void buildContent() {
        Section behaviour = new Section(I18n.get("enchantment:section.technical.description"));
        ResponsiveGrid tagGrid = new ResponsiveGrid(ResponsiveGrid.fixed(1))
                .atLeast(StudioBreakpoint.LG, ResponsiveGrid.fixed(1, 1));

        for (BehaviourTag field : BEHAVIOUR_TAGS) {
            SwitchCard sw = SwitchCard.literal(I18n.get(field.titleKey()), I18n.get(field.descKey()));
            bindToggle(sw.valueProperty(), entry -> entry.tags().contains(field.tagId()),
                    () -> applyTagToggle(field.tagId()));
            tagGrid.addItem(sw);
        }
        behaviour.addContent(tagGrid);

        Section costs = new Section(I18n.get("enchantment:section.costs"));
        ResponsiveGrid costGrid = new ResponsiveGrid(ResponsiveGrid.fixed(1))
                .atLeast(StudioBreakpoint.LG, ResponsiveGrid.fixed(1, 1));

        for (CostDef field : COST_FIELDS) {
            Range range = new Range(I18n.get("enchantment:global." + field.key() + ".title"), 0, 100, 1, 0);
            bindInt(range.valueProperty(), field.selector(), field.mutation(), field.actionFactory());
            costGrid.addItem(range);
        }
        costs.addContent(costGrid);

        content().getChildren().setAll(behaviour, costs, buildEffectsSection());
    }

    private VBox buildEffectsSection() {
        Section section = new Section(I18n.get("enchantment:technical.effects.title"));
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
            Identifier effectKey = Identifier.tryParse(effectId);
            String effectLabel = effectKey != null
                    ? StudioText.resolve("effect", effectKey)
                    : effectId;
            String descKey = effectKey != null
                    ? "effect:" + effectKey + ".desc"
                    : "";
            String effectDesc = I18n.exists(descKey) ? I18n.get(descKey) : effectId;
            SwitchCard card = SwitchCard.literal(effectLabel, effectDesc);
            bindToggle(card.valueProperty(),
                    entry -> !EnchantmentActions.isEffectDisabled(entry, effectId),
                    () -> applyCustomAction(EnchantmentActions.toggleDisabledEffect(effectId),
                            new EditorAction.ToggleDisabledEffect(effectId)));
            grid.addItem(card);
        }

        section.addContent(grid);
        return section;
    }
}
