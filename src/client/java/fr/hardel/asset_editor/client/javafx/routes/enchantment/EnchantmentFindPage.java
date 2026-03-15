package fr.hardel.asset_editor.client.javafx.routes.enchantment;

import fr.hardel.asset_editor.client.javafx.components.ui.Card;
import fr.hardel.asset_editor.client.javafx.components.ui.ResponsiveGrid;
import fr.hardel.asset_editor.client.javafx.components.ui.Section;
import fr.hardel.asset_editor.client.javafx.lib.RegistryPage;
import fr.hardel.asset_editor.client.javafx.lib.StudioContext;
import fr.hardel.asset_editor.client.javafx.lib.data.StudioBreakpoint;
import javafx.geometry.Insets;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.List;

public final class EnchantmentFindPage extends RegistryPage<Enchantment> {

    private record FindTag(Identifier tagId) {

        String titleKey() {
            return "enchantment_tag:" + tagId;
        }

        String descKey() {
            return "enchantment_tag:" + tagId + ".desc";
        }

        Identifier image() {
            return tagId.withPath("textures/studio/enchantment/" + tagId.getPath() + ".png");
        }
    }

    private static final List<FindTag> FIND_TAGS = List.of(
            new FindTag(Identifier.fromNamespaceAndPath("minecraft", "in_enchanting_table")),
            new FindTag(Identifier.fromNamespaceAndPath("minecraft", "on_mob_spawn_equipment")),
            new FindTag(Identifier.fromNamespaceAndPath("minecraft", "on_random_loot")),
            new FindTag(Identifier.fromNamespaceAndPath("minecraft", "tradeable")),
            new FindTag(Identifier.fromNamespaceAndPath("minecraft", "on_traded_equipment")),
            new FindTag(Identifier.fromNamespaceAndPath("minecraft", "curse")),
            new FindTag(Identifier.fromNamespaceAndPath("minecraft", "non_treasure")),
            new FindTag(Identifier.fromNamespaceAndPath("minecraft", "treasure"))
    );

    public EnchantmentFindPage(StudioContext context) {
        super(context, Registries.ENCHANTMENT, "enchantment-subpage-scroll", 64, new Insets(32));
    }

    @Override
    protected void buildContent() {
        Section section = new Section(I18n.get("enchantment:section.find"));
        ResponsiveGrid grid = new ResponsiveGrid(ResponsiveGrid.autoFit(368))
            .atMost(StudioBreakpoint.XL, ResponsiveGrid.fixed(1));

        for (FindTag ft : FIND_TAGS) {
            Card card = new Card(ft.image(), I18n.get(ft.titleKey()), I18n.get(ft.descKey()), false, false, null);
            card.setOnMouseClicked(e -> applyTagToggle(ft.tagId()));
            bindView(entry -> entry.tags().contains(ft.tagId()), card::setActive);
            grid.addItem(card);
        }

        section.addContent(grid);
        content().getChildren().setAll(section);
    }
}
