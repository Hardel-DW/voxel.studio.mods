package fr.hardel.asset_editor.client.javafx.components.page.enchantment;

import fr.hardel.asset_editor.client.javafx.components.ui.ResponsiveGrid;
import fr.hardel.asset_editor.client.javafx.components.ui.Category;
import fr.hardel.asset_editor.client.javafx.components.ui.InlineCard;
import fr.hardel.asset_editor.client.javafx.lib.FxSelectionBindings;
import fr.hardel.asset_editor.client.javafx.lib.StudioContext;
import fr.hardel.asset_editor.client.javafx.lib.data.StudioBreakpoint;
import fr.hardel.asset_editor.client.StudioText;
import fr.hardel.asset_editor.client.selector.StoreSelection;
import fr.hardel.asset_editor.store.ElementEntry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

public final class EnchantmentCategory extends Category {

    public EnchantmentCategory(String title, List<Identifier> identifiers,
        StoreSelection<ElementEntry<?>, Set<String>> directExclusiveSelection,
        FxSelectionBindings bindings,
        Function<Identifier, Boolean> toggleExclusive,
        StudioContext context) {
        super(title);

        ResponsiveGrid grid = new ResponsiveGrid(ResponsiveGrid.autoFit(256))
            .atMost(StudioBreakpoint.XL, ResponsiveGrid.fixed(1));

        for (Identifier id : identifiers) {
            String name = resolveEnchantmentName(context, id);

            boolean active = directExclusiveSelection.get() != null
                && directExclusiveSelection.get().contains(id.toString());
            InlineCard card = new InlineCard(name, id.getNamespace(), active, false, null);

            card.setOnMouseClicked(e -> toggleExclusive.apply(id));
            bindings.observe(directExclusiveSelection,
                ids -> card.activeProperty().set(ids != null && ids.contains(id.toString())));

            grid.addItem(card);
        }

        addContent(grid);
    }

    private static String resolveEnchantmentName(StudioContext context, Identifier id) {
        var entry = context.elementStore().get(Registries.ENCHANTMENT, id);
        if (entry != null)
            return entry.data().description().getString();
        return StudioText.resolve(Registries.ENCHANTMENT, id);
    }
}
