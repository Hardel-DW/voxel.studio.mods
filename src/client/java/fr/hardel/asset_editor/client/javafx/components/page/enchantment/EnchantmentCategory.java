package fr.hardel.asset_editor.client.javafx.components.page.enchantment;

import fr.hardel.asset_editor.client.javafx.components.ui.ResponsiveGrid;
import fr.hardel.asset_editor.client.javafx.components.ui.Category;
import fr.hardel.asset_editor.client.javafx.components.ui.InlineCard;
import fr.hardel.asset_editor.client.javafx.lib.StudioContext;
import fr.hardel.asset_editor.client.javafx.lib.action.EnchantmentMutations;
import fr.hardel.asset_editor.client.javafx.lib.data.StudioBreakpoint;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;

import java.util.List;

public final class EnchantmentCategory extends Category {

    public EnchantmentCategory(String titleKey, List<Identifier> identifiers, StudioContext context, Identifier currentElementId) {
        super(titleKey);

        ResponsiveGrid grid = new ResponsiveGrid(ResponsiveGrid.autoFit(256))
            .atMost(StudioBreakpoint.XL, ResponsiveGrid.fixed(1));

        for (Identifier id : identifiers) {
            String name = id.getPath().contains("/")
                    ? id.getPath().substring(id.getPath().lastIndexOf('/') + 1)
                    : id.getPath();

            InlineCard card = new InlineCard(name, id.getNamespace());

            card.setOnMouseClicked(e -> {
                var result = context.gateway().apply(Registries.ENCHANTMENT, currentElementId,
                        EnchantmentMutations.toggleExclusive(id));
                if (result.isApplied()) card.activeProperty().set(!card.isActive());
            });

            grid.addItem(card);
        }

        addContent(grid);
    }
}
