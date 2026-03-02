package fr.hardel.asset_editor.client.javafx.components.page.enchantment;

import fr.hardel.asset_editor.client.javafx.components.ui.AutoFitGrid;
import fr.hardel.asset_editor.client.javafx.components.ui.ToolCategory;
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

    private ToolCategory buildVanillaCategory() {
        ToolCategory category = new ToolCategory("enchantment:exclusive.vanilla.title");

        AutoFitGrid grid = new AutoFitGrid(256, true);

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

    private ToolCategory buildCustomCategory() {
        ToolCategory category = new ToolCategory("enchantment:exclusive.custom.title");

        Label fallback = new Label(I18n.get("enchantment:exclusive.custom.fallback"));
        fallback.setFont(fr.hardel.asset_editor.client.javafx.VoxelFonts.rubik(
            fr.hardel.asset_editor.client.javafx.VoxelFonts.Rubik.REGULAR, 13));
        fallback.setTextFill(fr.hardel.asset_editor.client.javafx.VoxelColors.ZINC_400);
        fallback.setPadding(new Insets(0, 16, 0, 16));

        category.addContent(fallback);
        return category;
    }
}
