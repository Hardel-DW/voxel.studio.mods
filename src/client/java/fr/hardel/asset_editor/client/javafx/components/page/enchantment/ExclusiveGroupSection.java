package fr.hardel.asset_editor.client.javafx.components.page.enchantment;

import fr.hardel.asset_editor.AssetEditor;
import fr.hardel.asset_editor.client.javafx.VoxelColors;
import fr.hardel.asset_editor.client.javafx.VoxelFonts;
import fr.hardel.asset_editor.client.javafx.components.ui.ResponsiveGrid;
import fr.hardel.asset_editor.client.javafx.components.ui.Category;
import fr.hardel.asset_editor.client.javafx.lib.StudioContext;
import fr.hardel.asset_editor.store.EnchantmentFlushAdapter;
import fr.hardel.asset_editor.client.javafx.lib.data.ExclusiveSetGroup;
import fr.hardel.asset_editor.client.javafx.lib.data.StudioBreakpoint;
import fr.hardel.asset_editor.client.javafx.lib.StudioText;
import fr.hardel.asset_editor.workspace.action.EditorAction;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

public final class ExclusiveGroupSection extends VBox {

    private final StudioContext context;
    private final Identifier elementId;
    private final Function<EditorAction, Boolean> dispatchAction;
    private final Function<Identifier, Boolean> applyTag;
    private EnchantmentTags currentTargetCard;

    public ExclusiveGroupSection(StudioContext context, Identifier elementId,
        Function<EditorAction, Boolean> dispatchAction,
        Function<Identifier, Boolean> applyTag) {
        this.context = context;
        this.elementId = elementId;
        this.dispatchAction = dispatchAction;
        this.applyTag = applyTag;
        setSpacing(32);
        setMaxWidth(Double.MAX_VALUE);

        var entry = context.currentEntry(Registries.ENCHANTMENT);
        String currentExclusiveTag = "";
        Set<Identifier> currentTags = Set.of();
        if (entry != null) {
            currentExclusiveTag = entry.data().exclusiveSet().unwrapKey()
                .map(k -> k.location().toString()).orElse("");
            currentTags = entry.tags();
        }

        Function<String, String> labelResolver = value -> {
            if (value == null || value.isBlank())
                return "";
            Identifier id = Identifier.tryParse(value.startsWith("#") ? value.substring(1) : value);
            if (id == null)
                return value;
            return context.resolveHolder(Registries.ENCHANTMENT, id)
                .map(holder -> holder.value().description().getString())
                .orElseGet(() -> StudioText.resolve(Registries.ENCHANTMENT, id));
        };

        getChildren().addAll(
            buildVanillaCategory(currentExclusiveTag, currentTags, labelResolver),
            buildCustomCategory(currentExclusiveTag, currentTags, labelResolver));
    }

    private Category buildVanillaCategory(String currentExclusiveTag, Set<Identifier> currentTags,
        Function<String, String> labelResolver) {
        Category category = new Category(I18n.get("enchantment.exclusive:vanilla"));

        ResponsiveGrid grid = new ResponsiveGrid(ResponsiveGrid.autoFit(256))
            .atMost(StudioBreakpoint.XL, ResponsiveGrid.fixed(1));

        for (ExclusiveSetGroup group : ExclusiveSetGroup.ALL) {
            Identifier tagId = group.tagId();
            String rawTag = tagId.toString();
            EnchantmentTags card = buildCard(
                StudioText.resolve("enchantment_tag", tagId),
                I18n.get("enchantment_tag:" + tagId + ".desc"),
                group.image(), tagId, rawTag,
                currentExclusiveTag, currentTags, labelResolver);
            if (rawTag.equals(currentExclusiveTag))
                currentTargetCard = card;
            grid.addItem(card);
        }

        category.addContent(grid);
        return category;
    }

    private Category buildCustomCategory(String currentExclusiveTag, Set<Identifier> currentTags,
        Function<String, String> labelResolver) {
        Category category = new Category(I18n.get("enchantment.exclusive:custom"));

        List<Identifier> customTags = EnchantmentFlushAdapter
            .customExclusiveTags(context.allTypedEntries(Registries.ENCHANTMENT));
        if (customTags.isEmpty()) {
            Label fallback = new Label(I18n.get("enchantment.exclusive:custom.fallback"));
            fallback.setFont(VoxelFonts.of(VoxelFonts.Variant.REGULAR, 13));
            fallback.setTextFill(VoxelColors.ZINC_400);
            fallback.setPadding(new Insets(0, 16, 0, 16));
            category.addContent(fallback);
            return category;
        }

        ResponsiveGrid grid = new ResponsiveGrid(ResponsiveGrid.autoFit(256))
            .atMost(StudioBreakpoint.XL, ResponsiveGrid.fixed(1));

        for (Identifier tagIdent : customTags) {
            String rawTag = tagIdent.toString();
            String title = StudioText.resolve("enchantment_tag", tagIdent);

            EnchantmentTags card = buildCard(
                title, rawTag,
                Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/logo.svg"),
                tagIdent, rawTag,
                currentExclusiveTag, currentTags, labelResolver);
            if (rawTag.equals(currentExclusiveTag))
                currentTargetCard = card;
            grid.addItem(card);
        }

        category.addContent(grid);
        return category;
    }

    private EnchantmentTags buildCard(String title, String description, Identifier imageId,
        Identifier tagId, String rawTag, String currentExclusiveTag,
        Set<Identifier> currentTags, Function<String, String> labelResolver) {

        List<String> members = resolveTagMembers(tagId);
        boolean isTarget = rawTag.equals(currentExclusiveTag);
        boolean isMember = currentTags.contains(tagId);

        EnchantmentTags[] holder = { null };
        EnchantmentTags card = new EnchantmentTags(
            title, description, imageId, members, isTarget, isMember, false,
            checked -> handleTargetToggle(holder[0], tagId, checked),
            checked -> handleMembershipToggle(holder[0], tagId, checked),
            labelResolver);
        holder[0] = card;
        return card;
    }

    private void handleTargetToggle(EnchantmentTags card, Identifier tagId, boolean checked) {
        EnchantmentTags previousTarget = currentTargetCard;

        if (checked && previousTarget != null && previousTarget != card) {
            previousTarget.updateTarget(false);
        }
        card.updateTarget(checked);
        currentTargetCard = checked ? card : null;

        boolean ok = dispatchAction.apply(new EditorAction.SetExclusiveSet(checked ? tagId.toString() : ""));
        if (!ok) {
            card.updateTarget(!checked);
            if (previousTarget != null && previousTarget != card) {
                previousTarget.updateTarget(true);
            }

            currentTargetCard = previousTarget;
        }
    }

    private void handleMembershipToggle(EnchantmentTags card, Identifier tagId, boolean checked) {
        List<String> before = card.getValues();

        card.updateMembership(checked);
        List<String> after = new ArrayList<>(before);
        String enchId = elementId.toString();
        if (checked && !after.contains(enchId))
            after.add(enchId);
        if (!checked)
            after.remove(enchId);
        card.updateValues(after);

        boolean ok = applyTag.apply(tagId);

        if (!ok) {
            card.updateMembership(!checked);
            card.updateValues(before);
        }
    }

    private List<String> resolveTagMembers(Identifier tagId) {
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
