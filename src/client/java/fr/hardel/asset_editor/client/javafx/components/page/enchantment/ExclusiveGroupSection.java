package fr.hardel.asset_editor.client.javafx.components.page.enchantment;

import fr.hardel.asset_editor.client.javafx.VoxelFonts;
import fr.hardel.asset_editor.client.javafx.components.ui.ResponsiveGrid;
import fr.hardel.asset_editor.client.javafx.components.ui.Category;
import fr.hardel.asset_editor.client.javafx.lib.data.StudioBreakpoint;
import fr.hardel.asset_editor.client.javafx.lib.data.ExclusiveSetGroup;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import net.minecraft.client.resources.language.I18n;

import java.util.List;

/**
 * Exclusive group mode: vanilla sets + custom sets placeholder.
 * Matches ExclusiveGroupSection.tsx.
 */
public final class ExclusiveGroupSection extends VBox {

    public ExclusiveGroupSection() {
        setSpacing(32);
        setMaxWidth(Double.MAX_VALUE);

        getChildren().addAll(buildVanillaCategory(), buildCustomCategory());
    }

    private Category buildVanillaCategory() {
        Category category = new Category("enchantment:exclusive.vanilla.title");

        ResponsiveGrid grid = new ResponsiveGrid(ResponsiveGrid.autoFit(256))
            .atMost(StudioBreakpoint.XL, ResponsiveGrid.fixed(1));

        for (ExclusiveSetGroup group : ExclusiveSetGroup.ALL) {
            grid.addItem(new EnchantmentTags(
                "enchantment:exclusive.set." + group.id() + ".title",
                "enchantment:exclusive.set." + group.id() + ".description",
                group.image(),
                List.of(),
                false,
                true
            ));
        }

        category.addContent(grid);
        return category;
    }

    private Category buildCustomCategory() {
        Category category = new Category("enchantment:exclusive.custom.title");

        Label fallback = new Label(I18n.get("enchantment:exclusive.custom.fallback"));
        fallback.setFont(VoxelFonts.of(VoxelFonts.Variant.REGULAR, 13));
        fallback.setTextFill(fr.hardel.asset_editor.client.javafx.VoxelColors.ZINC_400);
        fallback.setPadding(new Insets(0, 16, 0, 16));

        category.addContent(fallback);
        return category;
    }
}
