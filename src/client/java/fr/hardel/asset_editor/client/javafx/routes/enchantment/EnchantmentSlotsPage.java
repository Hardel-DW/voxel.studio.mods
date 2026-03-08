package fr.hardel.asset_editor.client.javafx.routes.enchantment;

import fr.hardel.asset_editor.client.javafx.VoxelColors;
import fr.hardel.asset_editor.client.javafx.VoxelFonts;
import fr.hardel.asset_editor.client.javafx.components.ui.ResponsiveGrid;
import fr.hardel.asset_editor.client.javafx.components.ui.Section;
import fr.hardel.asset_editor.client.javafx.components.ui.Card;
import fr.hardel.asset_editor.client.javafx.lib.StudioContext;
import fr.hardel.asset_editor.client.javafx.lib.action.EnchantmentActions;
import fr.hardel.asset_editor.client.javafx.lib.data.StudioBreakpoint;
import fr.hardel.asset_editor.client.javafx.lib.data.SlotConfigs;
import fr.hardel.asset_editor.client.javafx.lib.data.SlotConfigs.SlotConfig;
import fr.hardel.asset_editor.client.javafx.lib.store.RegistryElementStore.ElementEntry;
import fr.hardel.asset_editor.client.javafx.lib.store.StoreEvent;
import fr.hardel.asset_editor.client.javafx.lib.EditorPage;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.List;

public final class EnchantmentSlotsPage extends VBox implements EditorPage {

    private static final List<List<String>> GROUPS = List.of(
        List.of("mainhand", "offhand"),
        List.of("body", "saddle"),
        List.of("head", "chest", "legs", "feet")
    );

    private final StudioContext context;
    private final VBox content = new VBox(16);

    public EnchantmentSlotsPage(StudioContext context) {
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

        Section section = new Section("enchantment:section.slots.description");
        var enchantmentSlots = enchantment.definition().slots();

        for (List<String> group : GROUPS) {
            ResponsiveGrid row = new ResponsiveGrid(ResponsiveGrid.autoFit(256))
                .atMost(StudioBreakpoint.XL, ResponsiveGrid.fixed(1));

            for (String slotId : group) {
                SlotConfig cfg = SlotConfigs.BY_ID.get(slotId);
                if (cfg == null) continue;
                boolean active = enchantmentSlots.stream()
                        .anyMatch(g -> cfg.slots().contains(g.getSerializedName()));

                EquipmentSlotGroup slotGroup = EquipmentSlotGroup.valueOf(slotId.toUpperCase());
                Card slot = new Card(cfg.image(), cfg.nameKey(), active);
                slot.setOnMouseClicked(e -> context.gateway().apply(EnchantmentActions.toggleSlot(id, slotGroup)));
                row.addItem(slot);
            }
            section.addContent(row);
        }

        section.addContent(buildExplanation());
        content.getChildren().setAll(section);
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
        for (Label item : new Label[]{item1, item2}) {
            item.setFont(VoxelFonts.of(VoxelFonts.Variant.LIGHT, 13));
            item.setTextFill(VoxelColors.ZINC_400);
            item.setWrapText(true);
        }

        VBox list = new VBox(8, item1, item2);
        box.getChildren().addAll(title, list);
        return box;
    }
}
