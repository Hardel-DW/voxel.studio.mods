package fr.hardel.asset_editor.workspace.action.enchantment;

import fr.hardel.asset_editor.tag.TagSeed;
import fr.hardel.asset_editor.workspace.ElementEntry;
import fr.hardel.asset_editor.workspace.RegistryMutationContext;
import fr.hardel.asset_editor.workspace.action.EditorAction;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.HolderSet;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantment.EnchantmentDefinition;

import java.util.Optional;

public record SetPrimaryItemsAction(String tagId, TagSeed seed) implements EditorAction<Enchantment> {

    public static final StreamCodec<ByteBuf, SetPrimaryItemsAction> CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8, SetPrimaryItemsAction::tagId,
        EditorAction.OPTIONAL_TAG_SEED_CODEC, SetPrimaryItemsAction::seed,
        SetPrimaryItemsAction::new);

    @Override
    public ElementEntry<Enchantment> apply(ElementEntry<Enchantment> entry, RegistryMutationContext ctx) {
        SetSupportedItemsAction.ensureItemTag(tagId, seed, ctx);
        Optional<HolderSet<Item>> items = tagId == null || tagId.isEmpty()
            ? Optional.empty()
            : Optional.ofNullable(SetSupportedItemsAction.resolveItemTag(tagId, seed, ctx));

        if (tagId != null && !tagId.isEmpty() && items.isEmpty())
            return entry;

        Enchantment e = entry.data();
        EnchantmentDefinition d = e.definition();
        return entry.withData(new Enchantment(e.description(),
            new EnchantmentDefinition(d.supportedItems(), items, d.weight(), d.maxLevel(), d.minCost(), d.maxCost(), d.anvilCost(), d.slots()),
            e.exclusiveSet(), e.effects()));
    }
}
