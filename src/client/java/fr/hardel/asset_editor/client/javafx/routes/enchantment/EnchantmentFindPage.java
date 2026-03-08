package fr.hardel.asset_editor.client.javafx.routes.enchantment;

import fr.hardel.asset_editor.client.javafx.components.ui.Card;
import fr.hardel.asset_editor.client.javafx.components.ui.ResponsiveGrid;
import fr.hardel.asset_editor.client.javafx.components.ui.Section;
import fr.hardel.asset_editor.client.javafx.lib.RegistryPage;
import fr.hardel.asset_editor.client.javafx.lib.StudioContext;
import fr.hardel.asset_editor.client.javafx.lib.data.StudioBreakpoint;
import javafx.geometry.Insets;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.List;

public final class EnchantmentFindPage extends RegistryPage<Enchantment> {

    private record FindTag(String titleKey, String descKey, Identifier image, Identifier tagId) {}

    private static final List<FindTag> FIND_TAGS = List.of(
        new FindTag("enchantment:find.enchanting_table.title",    "enchantment:find.enchanting_table.description",    feature("block/enchanting_table"), minecraftTag("in_enchanting_table")),
        new FindTag("enchantment:find.mob_equipment.title",       "enchantment:find.mob_equipment.description",       feature("entity/zombie"),          minecraftTag("on_mob_spawn_equipment")),
        new FindTag("enchantment:find.loot_in_chests.title",      "enchantment:find.loot_in_chests.description",      feature("block/chest"),            minecraftTag("on_random_loot")),
        new FindTag("enchantment:find.tradeable.title",           "enchantment:find.tradeable.description",           feature("item/enchanted_book"),    minecraftTag("tradeable")),
        new FindTag("enchantment:find.tradeable_equipment.title", "enchantment:find.tradeable_equipment.description", feature("item/enchanted_item"),    minecraftTag("on_traded_equipment")),
        new FindTag("enchantment:find.curse.title",               "enchantment:find.curse.description",               feature("effect/curse"),           minecraftTag("curse")),
        new FindTag("enchantment:find.non_treasure.title",        "enchantment:find.non_treasure.description",        feature("effect/non_treasure"),    minecraftTag("non_treasure")),
        new FindTag("enchantment:find.treasure.title",            "enchantment:find.treasure.description",            feature("effect/treasure"),        minecraftTag("treasure"))
    );

    public EnchantmentFindPage(StudioContext context) {
        super(context, Registries.ENCHANTMENT, "enchantment-subpage-scroll", 64, new Insets(32));
    }

    @Override
    protected void buildContent() {
        Section section = new Section("enchantment:section.find");
        ResponsiveGrid grid = new ResponsiveGrid(ResponsiveGrid.autoFit(368))
            .atMost(StudioBreakpoint.XL, ResponsiveGrid.fixed(1));

        for (FindTag ft : FIND_TAGS) {
            Card card = new Card(ft.image(), ft.titleKey(), ft.descKey(), false, false, null);
            card.setOnMouseClicked(e -> applyTagToggle(ft.tagId()));

            var selector = select(entry -> entry.tags().contains(ft.tagId()));
            selector.subscribe(card::setActive);
            if (selector.get() != null) card.setActive(selector.get());

            grid.addItem(card);
        }

        section.addContent(grid);
        content().getChildren().setAll(section);
    }

    private static Identifier feature(String path) {
        return Identifier.fromNamespaceAndPath("asset_editor", "textures/features/" + path + ".png");
    }

    private static Identifier minecraftTag(String name) {
        return Identifier.fromNamespaceAndPath("minecraft", name);
    }
}
