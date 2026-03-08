package fr.hardel.asset_editor.client.javafx.routes.enchantment;

import fr.hardel.asset_editor.client.javafx.VoxelColors;
import fr.hardel.asset_editor.client.javafx.VoxelFonts;
import fr.hardel.asset_editor.client.javafx.components.ui.ResponsiveGrid;
import fr.hardel.asset_editor.client.javafx.components.ui.Range;
import fr.hardel.asset_editor.client.javafx.components.ui.Section;
import fr.hardel.asset_editor.client.javafx.components.ui.SwitchCard;
import fr.hardel.asset_editor.client.javafx.lib.StudioContext;
import fr.hardel.asset_editor.client.javafx.lib.action.EnchantmentActions;
import fr.hardel.asset_editor.client.javafx.lib.data.StudioBreakpoint;
import fr.hardel.asset_editor.client.javafx.lib.store.RegistryElementStore.ElementEntry;
import fr.hardel.asset_editor.client.javafx.lib.store.StoreEvent;
import fr.hardel.asset_editor.client.javafx.lib.EditorPage;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.List;
import java.util.function.BiFunction;

public final class EnchantmentTechnicalPage extends VBox implements EditorPage {

    private static final List<String> BEHAVIOUR_TAGS = List.of(
            "smelts_loot",
            "prevent_ice_melting",
            "prevent_infested_block_spawning",
            "prevent_bee_spawning",
            "prevent_pot_shattering",
            "price_doubled");

    private record CostField(String key, BiFunction<Identifier, Integer, fr.hardel.asset_editor.client.javafx.lib.action.EditorAction<Enchantment>> actionFactory,
                              java.util.function.Function<Enchantment, Integer> getter) {}

    private static final List<CostField> COST_FIELDS = List.of(
            new CostField("minCostBase", EnchantmentActions::setMinCostBase, e -> e.definition().minCost().base()),
            new CostField("minCostPerLevelAboveFirst", EnchantmentActions::setMinCostPerLevel, e -> e.definition().minCost().perLevelAboveFirst()),
            new CostField("maxCostBase", EnchantmentActions::setMaxCostBase, e -> e.definition().maxCost().base()),
            new CostField("maxCostPerLevelAboveFirst", EnchantmentActions::setMaxCostPerLevel, e -> e.definition().maxCost().perLevelAboveFirst()));

    private final StudioContext context;
    private final VBox content = new VBox(64);

    public EnchantmentTechnicalPage(StudioContext context) {
        this.context = context;

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("enchantment-subpage-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        getChildren().add(scroll);
        content.setPadding(new Insets(32));
    }

    @Override
    public void onActivate() { refresh(); }

    @Override
    public void onStoreEvent(StoreEvent event) { refresh(); }

    private void refresh() {
        var holder = context.findElement(Registries.ENCHANTMENT);
        if (holder == null) {
            content.getChildren().clear();
            return;
        }

        Identifier id = holder.key().identifier();
        ElementEntry<Enchantment> entry = context.elementStore().get("enchantment", id);
        Enchantment enchantment = entry != null ? entry.data() : holder.value();
        var tags = entry != null ? entry.tags() : java.util.Set.<Identifier>of();

        content.getChildren().setAll(
                buildBehaviourSection(id, tags),
                buildCostsSection(id, enchantment),
                buildEffectsSection());
    }

    private Section buildBehaviourSection(Identifier id, java.util.Set<Identifier> tags) {
        Section section = new Section("enchantment:section.technical.description");
        ResponsiveGrid grid = new ResponsiveGrid(ResponsiveGrid.fixed(1))
                .atLeast(StudioBreakpoint.LG, ResponsiveGrid.fixed(1, 1));

        for (String field : BEHAVIOUR_TAGS) {
            Identifier tagId = Identifier.fromNamespaceAndPath("minecraft", field);
            boolean active = tags.contains(tagId);

            SwitchCard sw = new SwitchCard(
                    "enchantment:technical." + field + ".title",
                    "enchantment:technical." + field + ".description");
            sw.setValue(active);
            sw.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (oldVal == null || newVal == null || oldVal.equals(newVal)) return;
                context.gateway().toggleTag("enchantment", id, tagId);
            });
            grid.addItem(sw);
        }

        section.addContent(grid);
        return section;
    }

    private Section buildCostsSection(Identifier id, Enchantment enchantment) {
        Section section = new Section("enchantment:section.costs");
        ResponsiveGrid grid = new ResponsiveGrid(ResponsiveGrid.fixed(1))
                .atLeast(StudioBreakpoint.LG, ResponsiveGrid.fixed(1, 1));

        for (CostField field : COST_FIELDS) {
            int currentValue = field.getter().apply(enchantment);
            Range range = new Range("enchantment:global." + field.key() + ".title", 0, 100, 1, currentValue);
            range.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (oldVal == null || newVal == null || oldVal.intValue() == newVal.intValue()) return;
                context.gateway().apply(field.actionFactory().apply(id, newVal.intValue()));
            });
            grid.addItem(range);
        }

        section.addContent(grid);
        return section;
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
