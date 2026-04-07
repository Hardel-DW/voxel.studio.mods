package fr.hardel.asset_editor.workspace.action.enchantment;

import fr.hardel.asset_editor.workspace.flush.ElementEntry;
import fr.hardel.asset_editor.workspace.io.RegistryMutationContext;
import fr.hardel.asset_editor.workspace.action.EditorAction;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.enchantment.Enchantment;

public record ToggleTagAction(Identifier tagId) implements EditorAction<Enchantment> {

    public static final StreamCodec<ByteBuf, ToggleTagAction> CODEC = StreamCodec.composite(Identifier.STREAM_CODEC, ToggleTagAction::tagId, ToggleTagAction::new);

    @Override
    public ElementEntry<Enchantment> apply(ElementEntry<Enchantment> entry, RegistryMutationContext ctx) {
        return entry.toggleTag(tagId);
    }
}
