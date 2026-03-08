package fr.hardel.asset_editor.client.javafx.components.page.enchantment;

import fr.hardel.asset_editor.client.javafx.VoxelFonts;
import fr.hardel.asset_editor.client.javafx.components.ui.ResponsiveGrid;
import fr.hardel.asset_editor.client.javafx.components.ui.Category;
import fr.hardel.asset_editor.client.javafx.lib.StudioContext;
import fr.hardel.asset_editor.client.javafx.lib.data.ExclusiveSetGroup;
import fr.hardel.asset_editor.client.javafx.lib.data.StudioBreakpoint;
import fr.hardel.asset_editor.client.javafx.lib.store.StoreSelector;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class ExclusiveGroupSection extends VBox {

    public ExclusiveGroupSection(StudioContext context, Identifier elementId, StoreSelector<String> exclusiveSelector) {
        setSpacing(32);
        setMaxWidth(Double.MAX_VALUE);

        List<String> memberNames = resolveTagMembers(context, elementId);

        getChildren().addAll(
                buildVanillaCategory(context, elementId, exclusiveSelector, memberNames),
                buildCustomCategory());
    }

    private Category buildVanillaCategory(StudioContext context, Identifier elementId,
                                           StoreSelector<String> exclusiveSelector, List<String> memberNames) {
        Category category = new Category("enchantment:exclusive.vanilla.title");

        ResponsiveGrid grid = new ResponsiveGrid(ResponsiveGrid.autoFit(256))
            .atMost(StudioBreakpoint.XL, ResponsiveGrid.fixed(1));

        for (ExclusiveSetGroup group : ExclusiveSetGroup.ALL) {
            String tagId = group.value().startsWith("#") ? group.value().substring(1) : group.value();
            Identifier tagIdentifier = Identifier.tryParse(tagId);

            boolean isTarget = exclusiveSelector.get() != null && exclusiveSelector.get().equals(tagId);

            List<String> tagMembers = resolveExclusiveTagMembers(context, tagIdentifier);

            EnchantmentTags card = new EnchantmentTags(
                "enchantment:exclusive.set." + group.id() + ".title",
                "enchantment:exclusive.set." + group.id() + ".description",
                group.image(),
                tagMembers,
                isTarget,
                false,
                () -> context.gateway().toggleTag(Registries.ENCHANTMENT, elementId, tagIdentifier)
            );

            exclusiveSelector.subscribe(val -> {
                boolean target = val != null && val.equals(tagId);
                card.updateTarget(target);
            });

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

    private List<String> resolveTagMembers(StudioContext context, Identifier elementId) {
        var entry = context.elementStore().get(Registries.ENCHANTMENT, elementId);
        if (entry == null) return List.of();
        return entry.tags().stream().map(Identifier::toString).toList();
    }

    private List<String> resolveExclusiveTagMembers(StudioContext context, Identifier tagId) {
        if (tagId == null) return List.of();
        List<String> members = new ArrayList<>();
        for (var entry : context.allEntries(Registries.ENCHANTMENT)) {
            Set<Identifier> tags = entry.tags();
            if (tags.contains(tagId)) {
                members.add(entry.id().toString());
            }
        }
        return members;
    }
}
