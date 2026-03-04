package fr.hardel.asset_editor.client.javafx.components.page.enchantment;

import fr.hardel.asset_editor.client.javafx.components.ui.ResponsiveGrid;
import fr.hardel.asset_editor.client.javafx.components.ui.Category;
import fr.hardel.asset_editor.client.javafx.components.ui.InlineCard;
import fr.hardel.asset_editor.client.javafx.lib.data.StudioBreakpoint;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * ToolCategory wrapping a grid of ToolInline cards for enchantment identifiers.
 * Used in ExclusiveSingleSection.
 */
public final class EnchantmentCategory extends Category {

    public EnchantmentCategory(String titleKey, List<Identifier> identifiers) {
        super(titleKey);

        ResponsiveGrid grid = new ResponsiveGrid(ResponsiveGrid.autoFit(256))
            .atMost(StudioBreakpoint.XL, ResponsiveGrid.fixed(1));

        for (Identifier id : identifiers) {
            String name = id.getPath().contains("/") ? id.getPath().substring(id.getPath().lastIndexOf('/') + 1) : id.getPath();
            grid.addItem(new InlineCard(name, id.getNamespace()));
        }

        addContent(grid);
    }
}
