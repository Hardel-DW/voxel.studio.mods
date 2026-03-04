package fr.hardel.asset_editor.client.javafx.routes.enchantment;

import fr.hardel.asset_editor.client.javafx.components.ui.ResponsiveGrid;
import fr.hardel.asset_editor.client.javafx.components.ui.Section;
import fr.hardel.asset_editor.client.javafx.components.ui.Card;
import fr.hardel.asset_editor.client.javafx.lib.StudioContext;
import fr.hardel.asset_editor.client.javafx.lib.data.StudioBreakpoint;
import javafx.geometry.Insets;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import net.minecraft.resources.Identifier;

import java.util.List;

public final class EnchantmentFindPage extends VBox {

    private record FindEntry(String titleKey, String descKey, Identifier image) {}

    private static final List<FindEntry> FIND_ENTRIES = List.of(
        new FindEntry("enchantment:find.enchanting_table.title",    "enchantment:find.enchanting_table.description",    feature("block/enchanting_table")),
        new FindEntry("enchantment:find.mob_equipment.title",       "enchantment:find.mob_equipment.description",       feature("entity/zombie")),
        new FindEntry("enchantment:find.loot_in_chests.title",      "enchantment:find.loot_in_chests.description",      feature("block/chest")),
        new FindEntry("enchantment:find.tradeable.title",           "enchantment:find.tradeable.description",           feature("item/enchanted_book")),
        new FindEntry("enchantment:find.tradeable_equipment.title", "enchantment:find.tradeable_equipment.description", feature("item/enchanted_item")),
        new FindEntry("enchantment:find.curse.title",               "enchantment:find.curse.description",               feature("effect/curse")),
        new FindEntry("enchantment:find.non_treasure.title",        "enchantment:find.non_treasure.description",        feature("effect/non_treasure")),
        new FindEntry("enchantment:find.treasure.title",            "enchantment:find.treasure.description",            feature("effect/treasure"))
    );

    private final VBox content = new VBox(64);

    public EnchantmentFindPage(StudioContext context) {
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("enchantment-subpage-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        getChildren().add(scroll);

        content.setPadding(new Insets(32));
        refresh();
    }

    private void refresh() {
        Section behaviour = new Section("enchantment:section.find");
        ResponsiveGrid grid = new ResponsiveGrid(ResponsiveGrid.autoFit(368))
            .atMost(StudioBreakpoint.XL, ResponsiveGrid.fixed(1));

        for (FindEntry entry : FIND_ENTRIES) {
            grid.addItem(new Card(entry.image(), entry.titleKey(), entry.descKey(), false, false, null));
        }
        behaviour.addContent(grid);
        content.getChildren().setAll(behaviour);
    }

    private static Identifier feature(String path) {
        return Identifier.fromNamespaceAndPath("asset_editor", "textures/features/" + path + ".png");
    }

}
