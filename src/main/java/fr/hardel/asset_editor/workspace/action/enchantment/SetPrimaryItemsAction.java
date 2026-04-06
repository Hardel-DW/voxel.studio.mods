package fr.hardel.asset_editor.workspace.action.enchantment;

import fr.hardel.asset_editor.AssetEditor;
import fr.hardel.asset_editor.tag.TagSeed;
import fr.hardel.asset_editor.workspace.action.EditorAction;
import fr.hardel.asset_editor.workspace.action.EditorActionType;
import net.minecraft.core.HolderSet;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantment.EnchantmentDefinition;

import java.util.Optional;

public record SetPrimaryItemsAction(String tagId, TagSeed seed) implements EditorAction {

    public static final EditorActionType<Enchantment, SetPrimaryItemsAction> TYPE = new EditorActionType<>(
        Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "enchantment/set_primary_items"),
        SetPrimaryItemsAction.class,
        StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, SetPrimaryItemsAction::tagId,
            EditorAction.OPTIONAL_TAG_SEED_CODEC, SetPrimaryItemsAction::seed,
            SetPrimaryItemsAction::new),
        (entry, action, ctx) -> {
            SetSupportedItemsAction.ensureItemTag(action.tagId(), action.seed(), ctx);
            Optional<HolderSet<Item>> items = action.tagId() == null || action.tagId().isEmpty()
                ? Optional.empty()
                : Optional.ofNullable(SetSupportedItemsAction.resolveItemTag(action.tagId(), action.seed(), ctx));

            if (action.tagId() != null && !action.tagId().isEmpty() && items.isEmpty())
                return entry;

            Enchantment e = entry.data();
            EnchantmentDefinition d = e.definition();
            return entry.withData(new Enchantment(e.description(),
                new EnchantmentDefinition(d.supportedItems(), items, d.weight(), d.maxLevel(), d.minCost(), d.maxCost(), d.anvilCost(), d.slots()),
                e.exclusiveSet(), e.effects()));
        });

    @Override
    public EditorActionType<Enchantment, SetPrimaryItemsAction> type() {
        return TYPE;
    }
}
