package fr.hardel.asset_editor.client.javafx.components.page.enchantment;

import fr.hardel.asset_editor.client.javafx.VoxelColors;
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

import java.util.ArrayList;
import java.util.List;

public final class ExclusiveGroupSection extends VBox {

    public ExclusiveGroupSection(StudioContext context, Identifier elementId, StoreSelector<String> exclusiveSelector) {
        setSpacing(32);
        setMaxWidth(Double.MAX_VALUE);

        getChildren().addAll(
                buildVanillaCategory(context, elementId, exclusiveSelector),
                buildCustomCategory(context, elementId, exclusiveSelector));
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
                    () -> toggleGroup(context, elementId, tagIdentifier, rawTag, exclusiveSelector));

            exclusiveSelector.subscribe(val -> card.updateTarget(rawTag.equals(val)));

            grid.addItem(card);
        }

        category.addContent(grid);
        return category;
    }

    private Category buildCustomCategory(StudioContext context, Identifier elementId,
            StoreSelector<String> exclusiveSelector) {
        Category category = new Category("enchantment:exclusive.custom.title");

        List<Identifier> customTags = EnchantmentMutations
                .customExclusiveTags(context.allTypedEntries(Registries.ENCHANTMENT));
        if (customTags.isEmpty()) {
            Label fallback = new Label(I18n.get("enchantment:exclusive.custom.fallback"));
            fallback.setFont(VoxelFonts.of(VoxelFonts.Variant.REGULAR, 13));
            fallback.setTextFill(VoxelColors.ZINC_400);
            fallback.setPadding(new Insets(0, 16, 0, 16));

            category.addContent(fallback);
            return category;
        }

        ResponsiveGrid grid = new ResponsiveGrid(ResponsiveGrid.autoFit(256))
                .atMost(StudioBreakpoint.XL, ResponsiveGrid.fixed(1));

        for (Identifier tagId : customTags) {
            String rawTag = tagId.toString();
            EnchantmentTags card = new EnchantmentTags(
                    tagId.getPath(),
                    rawTag,
                    Identifier.fromNamespaceAndPath("asset_editor", "icons/logo.svg"),
                    resolveExclusiveTagMembers(context, tagId),
                    rawTag.equals(exclusiveSelector.get()),
                    false,
                    () -> toggleGroup(context, elementId, tagId, rawTag, exclusiveSelector));

            exclusiveSelector.subscribe(value -> card.updateTarget(rawTag.equals(value)));
            grid.addItem(card);
        }

        category.addContent(grid);
        return category;
    }

    private void toggleGroup(StudioContext context, Identifier elementId, Identifier tagId, String rawTag,
            StoreSelector<String> exclusiveSelector) {
        boolean currentlyTarget = rawTag.equals(exclusiveSelector.get());
        boolean currentlyMember = context.elementStore().get(Registries.ENCHANTMENT, elementId).tags().contains(tagId);

        if (currentlyTarget) {
            context.gateway().apply(Registries.ENCHANTMENT, elementId,
                    EnchantmentMutations.exclusiveSet(HolderSet.empty()));
            if (currentlyMember) {
                context.gateway().toggleTag(Registries.ENCHANTMENT, elementId, tagId);
            }
            return;
        }

        context.gateway().apply(Registries.ENCHANTMENT, elementId, EnchantmentMutations.exclusiveSet(tagId));
        if (!currentlyMember) {
            context.gateway().toggleTag(Registries.ENCHANTMENT, elementId, tagId);
        }
    }

    private List<String> resolveExclusiveTagMembers(StudioContext context, Identifier tagId) {
        if (tagId == null)
            return List.of();
        List<String> members = new ArrayList<>();
        for (var entry : context.allTypedEntries(Registries.ENCHANTMENT)) {
            if (entry.tags().contains(tagId)) {
                members.add(entry.id().toString());
            }
        }
        return members;
    }

}
