package fr.hardel.asset_editor.workspace.action.enchantment;

import fr.hardel.asset_editor.workspace.ElementEntry;
import fr.hardel.asset_editor.workspace.RegistryMutationContext;
import fr.hardel.asset_editor.workspace.action.EditorAction;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.enchantment.Enchantment;

public record SetExclusiveSetAction(String tagId) implements EditorAction<Enchantment> {

    public static final StreamCodec<ByteBuf, SetExclusiveSetAction> CODEC = StreamCodec.composite(ByteBufCodecs.STRING_UTF8, SetExclusiveSetAction::tagId, SetExclusiveSetAction::new);

    @Override
    public ElementEntry<Enchantment> apply(ElementEntry<Enchantment> entry, RegistryMutationContext ctx) {
        Enchantment e = entry.data();
        if (tagId.isEmpty())
            return entry.withData(new Enchantment(e.description(), e.definition(), HolderSet.empty(), e.effects()));

        Identifier id = Identifier.tryParse(tagId);
        if (id == null)
            return entry;

        HolderSet<Enchantment> resolved = ctx.resolveTagReference(Registries.ENCHANTMENT, id);
        return entry.withData(new Enchantment(e.description(), e.definition(), resolved == null ? HolderSet.empty() : resolved, e.effects()));
    }
}
