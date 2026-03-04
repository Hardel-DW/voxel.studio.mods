package fr.hardel.asset_editor.client.javafx.routes.enchantment;

import fr.hardel.asset_editor.client.javafx.VoxelColors;
import fr.hardel.asset_editor.client.javafx.VoxelFonts;
import fr.hardel.asset_editor.client.javafx.components.ui.ResponsiveGrid;
import fr.hardel.asset_editor.client.javafx.components.ui.Section;
import fr.hardel.asset_editor.client.javafx.components.ui.Card;
import fr.hardel.asset_editor.client.javafx.lib.StudioContext;
import fr.hardel.asset_editor.client.javafx.lib.data.StudioBreakpoint;
import fr.hardel.asset_editor.client.javafx.lib.data.SlotConfig;
import fr.hardel.asset_editor.client.javafx.lib.data.SlotConfigs;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.Holder;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.List;

public final class EnchantmentSlotsPage extends VBox {

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

        context.tabsState().currentElementIdProperty().addListener((obs, o, v) -> refresh());
        Platform.runLater(this::refresh);
    }

    private void refresh() {
        Holder.Reference<Enchantment> holder = selectedEnchantment();
        if (holder == null) return;

        Section section = new Section("enchantment:section.slots.description");
        var enchantmentSlots = holder.value().definition().slots();

        for (List<String> group : GROUPS) {
            ResponsiveGrid row = new ResponsiveGrid(ResponsiveGrid.autoFit(256))
                .atMost(StudioBreakpoint.XL, ResponsiveGrid.fixed(1));

            for (int i = 0; i < group.size(); i++) {
                SlotConfig cfg = SlotConfigs.BY_ID.get(group.get(i));
                if (cfg == null) continue;
                boolean active = enchantmentSlots.stream()
                        .anyMatch(g -> cfg.slots().contains(g.getSerializedName()));
                Card slot = new Card(cfg.image(), cfg.nameKey(), active);
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
        title.setFont(VoxelFonts.rubik(VoxelFonts.Rubik.REGULAR, 13));
        title.setTextFill(VoxelColors.ZINC_300);
        title.setWrapText(true);

        Label item1 = new Label("- " + I18n.get("enchantment:slots.explanation.list.1"));
        Label item2 = new Label("- " + I18n.get("enchantment:slots.explanation.list.2"));
        for (Label item : new Label[]{item1, item2}) {
            item.setFont(VoxelFonts.rubik(VoxelFonts.Rubik.LIGHT, 13));
            item.setTextFill(VoxelColors.ZINC_400);
            item.setWrapText(true);
        }

        VBox list = new VBox(8, item1, item2);
        box.getChildren().addAll(title, list);
        return box;
    }

    private Holder.Reference<Enchantment> selectedEnchantment() {
        String id = context.tabsState().currentElementId();
        var enchantments = context.enchantments();
        if (enchantments.isEmpty()) return null;
        if (id != null && !id.isBlank()) {
            for (var h : enchantments) {
                if (h.key().identifier().toString().equals(id)) return h;
            }
        }
        return enchantments.getFirst();
    }
}
