package fr.hardel.asset_editor.client.javafx.components.page.enchantment;

import fr.hardel.asset_editor.client.javafx.VoxelFonts;
import fr.hardel.asset_editor.client.javafx.components.ui.ResponsiveGrid;
import fr.hardel.asset_editor.client.javafx.components.ui.Category;
import fr.hardel.asset_editor.client.javafx.lib.StudioContext;
import fr.hardel.asset_editor.client.javafx.lib.action.EnchantmentMutations;
import fr.hardel.asset_editor.client.javafx.lib.data.ExclusiveSetGroup;
import fr.hardel.asset_editor.client.javafx.lib.data.StudioBreakpoint;
import fr.hardel.asset_editor.client.javafx.lib.store.StoreSelector;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.ArrayList;
import java.util.List;

public final class ExclusiveGroupSection extends VBox {

    public ExclusiveGroupSection(StudioContext context, Identifier elementId, StoreSelector<String> exclusiveSelector) {
        setSpacing(32);
        setMaxWidth(Double.MAX_VALUE);

        getChildren().addAll(
                buildVanillaCategory(context, elementId, exclusiveSelector),
                buildCustomCategory());
    }

    private Category buildVanillaCategory(StudioContext context, Identifier elementId,
                                           StoreSelector<String> exclusiveSelector) {
        Category category = new Category("enchantment:exclusive.vanilla.title");

        ResponsiveGrid grid = new ResponsiveGrid(ResponsiveGrid.autoFit(256))
            .atMost(StudioBreakpoint.XL, ResponsiveGrid.fixed(1));

        for (ExclusiveSetGroup group : ExclusiveSetGroup.ALL) {
            String rawTag = group.value().startsWith("#") ? group.value().substring(1) : group.value();
            Identifier tagIdentifier = Identifier.tryParse(rawTag);

            boolean isTarget = rawTag.equals(exclusiveSelector.get());
            List<String> tagMembers = resolveExclusiveTagMembers(context, tagIdentifier);

            EnchantmentTags card = new EnchantmentTags(
                "enchantment:exclusive.set." + group.id() + ".title",
                "enchantment:exclusive.set." + group.id() + ".description",
                group.image(),
                tagMembers,
                isTarget,
                false,
                () -> {
                    boolean currentlyTarget = rawTag.equals(exclusiveSelector.get());
                    if (currentlyTarget) {
                        context.gateway().apply(Registries.ENCHANTMENT, elementId,
                                EnchantmentMutations.exclusiveSet(HolderSet.empty()));
                    } else {
                        HolderSet<Enchantment> holderSet = EnchantmentMutations.resolveEnchantmentTag(rawTag);
                        if (holderSet != null) {
                            context.gateway().apply(Registries.ENCHANTMENT, elementId,
                                    EnchantmentMutations.exclusiveSet(holderSet));
                        }
                    }
                }
            );

            exclusiveSelector.subscribe(val -> card.updateTarget(rawTag.equals(val)));

            grid.addItem(card);
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

    private List<String> resolveExclusiveTagMembers(StudioContext context, Identifier tagId) {
        if (tagId == null) return List.of();
        List<String> members = new ArrayList<>();
        for (var entry : context.allTypedEntries(Registries.ENCHANTMENT)) {
            if (entry.tags().contains(tagId)) {
                members.add(entry.id().toString());
            }
        }
        return members;
    }

}
