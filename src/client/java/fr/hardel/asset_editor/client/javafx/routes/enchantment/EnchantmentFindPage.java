package fr.hardel.asset_editor.client.javafx.routes.enchantment;

import fr.hardel.asset_editor.client.javafx.components.ui.ResponsiveGrid;
import fr.hardel.asset_editor.client.javafx.components.ui.Section;
import fr.hardel.asset_editor.client.javafx.components.ui.Card;
import fr.hardel.asset_editor.client.javafx.lib.StudioContext;
import fr.hardel.asset_editor.client.javafx.lib.data.StudioBreakpoint;
import fr.hardel.asset_editor.client.javafx.lib.store.RegistryElementStore.ElementEntry;
import javafx.geometry.Insets;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.List;
import java.util.Set;

public final class EnchantmentFindPage extends VBox {

    private record FindTag(String titleKey, String descKey, Identifier image, Identifier tagId) {}

    private static final List<FindTag> FIND_TAGS = List.of(
        new FindTag("enchantment:find.enchanting_table.title",    "enchantment:find.enchanting_table.description",    feature("block/enchanting_table"), mcTag("in_enchanting_table")),
        new FindTag("enchantment:find.mob_equipment.title",       "enchantment:find.mob_equipment.description",       feature("entity/zombie"),          mcTag("on_mob_spawn_equipment")),
        new FindTag("enchantment:find.loot_in_chests.title",      "enchantment:find.loot_in_chests.description",      feature("block/chest"),            mcTag("on_random_loot")),
        new FindTag("enchantment:find.tradeable.title",           "enchantment:find.tradeable.description",           feature("item/enchanted_book"),    mcTag("tradeable")),
        new FindTag("enchantment:find.tradeable_equipment.title", "enchantment:find.tradeable_equipment.description", feature("item/enchanted_item"),    mcTag("on_traded_equipment")),
        new FindTag("enchantment:find.curse.title",               "enchantment:find.curse.description",               feature("effect/curse"),           mcTag("curse")),
        new FindTag("enchantment:find.non_treasure.title",        "enchantment:find.non_treasure.description",        feature("effect/non_treasure"),    mcTag("non_treasure")),
        new FindTag("enchantment:find.treasure.title",            "enchantment:find.treasure.description",            feature("effect/treasure"),        mcTag("treasure"))
    );

    private final StudioContext context;
    private final VBox content = new VBox(64);

    public EnchantmentFindPage(StudioContext context) {
        this.context = context;

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("enchantment-subpage-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        getChildren().add(scroll);

        content.setPadding(new Insets(32));

        context.tabsState().currentElementIdProperty().addListener((obs, o, v) -> refresh());
        context.elementStore().versionProperty().addListener((obs, o, v) -> refresh());
        refresh();
    }

    private void refresh() {
        var holder = context.findElement(Registries.ENCHANTMENT);
        if (holder == null) {
            content.getChildren().clear();
            return;
        }

        Identifier id = holder.key().identifier();
        ElementEntry<Enchantment> entry = context.elementStore().get("enchantment", id);
        Set<Identifier> tags = entry != null ? entry.tags() : Set.of();

        Section section = new Section("enchantment:section.find");
        ResponsiveGrid grid = new ResponsiveGrid(ResponsiveGrid.autoFit(368))
            .atMost(StudioBreakpoint.XL, ResponsiveGrid.fixed(1));

        for (FindTag ft : FIND_TAGS) {
            boolean active = tags.contains(ft.tagId());
            Card card = new Card(ft.image(), ft.titleKey(), ft.descKey(), active, false, null);
            card.setOnMouseClicked(e -> context.gateway().toggleTag("enchantment", id, ft.tagId()));
            grid.addItem(card);
        }

        section.addContent(grid);
        content.getChildren().setAll(section);
    }

    private static Identifier feature(String path) {
        return Identifier.fromNamespaceAndPath("asset_editor", "textures/features/" + path + ".png");
    }

    private static Identifier mcTag(String name) {
        return Identifier.fromNamespaceAndPath("minecraft", name);
    }
}
