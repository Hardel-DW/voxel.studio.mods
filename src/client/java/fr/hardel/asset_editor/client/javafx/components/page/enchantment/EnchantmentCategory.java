package fr.hardel.asset_editor.client.javafx.components.page.enchantment;

import fr.hardel.asset_editor.client.javafx.components.ui.ResponsiveGrid;
import fr.hardel.asset_editor.client.javafx.components.ui.Category;
import fr.hardel.asset_editor.client.javafx.components.ui.InlineCard;
import fr.hardel.asset_editor.client.javafx.lib.StudioContext;
import fr.hardel.asset_editor.client.javafx.lib.action.EnchantmentActions;
import fr.hardel.asset_editor.client.javafx.lib.store.StoreSelector;
import fr.hardel.asset_editor.client.javafx.lib.data.StudioBreakpoint;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.UnaryOperator;

public final class EnchantmentCategory extends Category {

    public EnchantmentCategory(String titleKey, List<Identifier> identifiers,
                               StoreSelector<Set<String>> directExclusiveSelector,
                               Function<UnaryOperator<Enchantment>, Boolean> applyMutation,
                               StudioContext context) {
        super(titleKey);

        ResponsiveGrid grid = new ResponsiveGrid(ResponsiveGrid.autoFit(256))
            .atMost(StudioBreakpoint.XL, ResponsiveGrid.fixed(1));

        for (Identifier id : identifiers) {
            String name = resolveEnchantmentName(context, id);

            boolean active = directExclusiveSelector.get() != null
                    && directExclusiveSelector.get().contains(id.toString());
            InlineCard card = new InlineCard(name, id.getNamespace(), active, false, null);

            card.setOnMouseClicked(e ->
                context.resolveHolder(Registries.ENCHANTMENT, id).ifPresent(holder ->
                    applyMutation.apply(EnchantmentActions.toggleExclusive(holder, id))));
            directExclusiveSelector.subscribe(ids ->
                    card.activeProperty().set(ids != null && ids.contains(id.toString())));

            grid.addItem(card);
        }

        addContent(grid);
    }

    private static String resolveEnchantmentName(StudioContext context, Identifier id) {
        var entry = context.elementStore().get(Registries.ENCHANTMENT, id);
        if (entry != null) return entry.data().description().getString();
        String path = id.getPath();
        return path.contains("/") ? path.substring(path.lastIndexOf('/') + 1) : path;
    }
}
