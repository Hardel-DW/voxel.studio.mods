package fr.hardel.asset_editor.client.javafx.components.page.enchantment;

import fr.hardel.asset_editor.client.javafx.components.ui.ToolCategory;
import fr.hardel.asset_editor.client.javafx.components.ui.ToolInline;
import javafx.scene.layout.TilePane;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * ToolCategory wrapping a grid of ToolInline cards for enchantment identifiers.
 * Used in ExclusiveSingleSection.
 */
public final class EnchantmentCategory extends ToolCategory {

    public EnchantmentCategory(String titleKey, List<Identifier> identifiers) {
        super(titleKey);

        TilePane grid = new TilePane();
        grid.setHgap(16);
        grid.setVgap(16);
        grid.setPrefTileWidth(256);
        grid.setMaxWidth(Double.MAX_VALUE);

        for (Identifier id : identifiers) {
            String name = id.getPath().contains("/") ? id.getPath().substring(id.getPath().lastIndexOf('/') + 1) : id.getPath();
            grid.getChildren().add(new ToolInline(name, id.getNamespace()));
        }

        addContent(grid);
    }
}
