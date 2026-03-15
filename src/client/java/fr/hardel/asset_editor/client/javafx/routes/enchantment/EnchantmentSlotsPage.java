package fr.hardel.asset_editor.client.javafx.routes.enchantment;

import fr.hardel.asset_editor.client.javafx.VoxelColors;
import fr.hardel.asset_editor.client.javafx.VoxelFonts;
import fr.hardel.asset_editor.client.javafx.components.ui.Card;
import fr.hardel.asset_editor.client.javafx.components.ui.ResponsiveGrid;
import fr.hardel.asset_editor.client.javafx.components.ui.Section;
import fr.hardel.asset_editor.client.javafx.lib.RegistryPage;
import fr.hardel.asset_editor.client.javafx.lib.SlotManager;
import fr.hardel.asset_editor.client.javafx.lib.StudioContext;
import fr.hardel.asset_editor.client.javafx.lib.action.EnchantmentActions;
import fr.hardel.asset_editor.client.javafx.lib.data.SlotConfigs;
import fr.hardel.asset_editor.client.javafx.lib.data.SlotConfigs.SlotConfig;
import fr.hardel.asset_editor.client.javafx.lib.data.StudioBreakpoint;
import fr.hardel.asset_editor.network.EditorAction;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.List;

public final class EnchantmentSlotsPage extends RegistryPage<Enchantment> {

    public EnchantmentSlotsPage(StudioContext context) {
        super(context, Registries.ENCHANTMENT, "enchantment-subpage-scroll", 16, new Insets(32));
    }

    @Override
    protected void buildContent() {
        Section section = new Section(I18n.get("enchantment:section.slots.description"));

        for (List<String> group : SlotConfigs.GROUPS) {
            ResponsiveGrid row = new ResponsiveGrid(ResponsiveGrid.autoFit(256))
                .atMost(StudioBreakpoint.XL, ResponsiveGrid.fixed(1));

            for (String slotId : group) {
                SlotConfig cfg = SlotConfigs.BY_ID.get(slotId);
                if (cfg == null)
                    continue;
                EquipmentSlotGroup slotGroup = EquipmentSlotGroup.valueOf(slotId.toUpperCase());
                Card card = new Card(cfg.image(), I18n.get("slot:" + cfg.id()), false);

                card.setOnMouseClicked(e -> applyAction(EnchantmentActions.toggleSlot(slotGroup),
                    new EditorAction.ToggleSlot(slotId)));
                bindView(entry -> new SlotManager(entry.data().definition().slots()).isActive(slotGroup),
                    card::setActive);

                row.addItem(card);
            }
            section.addContent(row);
        }

        section.addContent(buildExplanation());
        content().getChildren().setAll(section);
    }

    private VBox buildExplanation() {
        VBox box = new VBox(8);
        box.setPadding(new Insets(16));

        Label title = new Label(I18n.get("enchantment:slots.explanation.title"));
        title.setFont(VoxelFonts.of(VoxelFonts.Variant.REGULAR, 13));
        title.setTextFill(VoxelColors.ZINC_300);
        title.setWrapText(true);

        Label item1 = new Label("- " + I18n.get("enchantment:slots.explanation.list.1"));
        Label item2 = new Label("- " + I18n.get("enchantment:slots.explanation.list.2"));
        for (Label item : new Label[] { item1, item2 }) {
            item.setFont(VoxelFonts.of(VoxelFonts.Variant.LIGHT, 13));
            item.setTextFill(VoxelColors.ZINC_400);
            item.setWrapText(true);
        }

        VBox list = new VBox(8, item1, item2);
        box.getChildren().addAll(title, list);
        return box;
    }
}
