package fr.hardel.asset_editor.workspace.action.enchantment;

import fr.hardel.asset_editor.tag.TagSeed;
import fr.hardel.asset_editor.workspace.ElementEntry;
import fr.hardel.asset_editor.workspace.RegistryMutationContext;
import fr.hardel.asset_editor.workspace.action.EditorAction;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantment.EnchantmentDefinition;

public record SetSupportedItemsAction(String tagId, TagSeed seed) implements EditorAction<Enchantment> {

    public static final StreamCodec<ByteBuf, SetSupportedItemsAction> CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8, SetSupportedItemsAction::tagId,
        EditorAction.OPTIONAL_TAG_SEED_CODEC, SetSupportedItemsAction::seed,
        SetSupportedItemsAction::new);

    @Override
    public ElementEntry<Enchantment> apply(ElementEntry<Enchantment> entry, RegistryMutationContext ctx) {
        ensureItemTag(tagId, seed, ctx);
        HolderSet<Item> items = resolveItemTag(tagId, seed, ctx);
        if (items == null)
            return entry;

        Enchantment e = entry.data();
        EnchantmentDefinition d = e.definition();
        return entry.withData(new Enchantment(e.description(),
            new EnchantmentDefinition(items, d.primaryItems(), d.weight(), d.maxLevel(), d.minCost(), d.maxCost(), d.anvilCost(), d.slots()),
            e.exclusiveSet(), e.effects()));
    }

    static void ensureItemTag(String rawTagId, TagSeed seed, RegistryMutationContext ctx) {
        if (seed == null || rawTagId == null || rawTagId.isBlank())
            return;

        Identifier id = Identifier.tryParse(rawTagId);
        if (id == null)
            throw new IllegalArgumentException("Invalid item tag id: " + rawTagId);

        ctx.ensureTagResource(Registries.ITEM, id, seed);
    }

    static HolderSet<Item> resolveItemTag(String rawTagId, TagSeed seed, RegistryMutationContext ctx) {
        Identifier id = Identifier.tryParse(rawTagId);
        if (id == null)
            return null;

        return seed == null
            ? ctx.resolveTagReference(Registries.ITEM, id)
            : ctx.resolveTagReferenceOrPlaceholder(Registries.ITEM, id);
    }
}
