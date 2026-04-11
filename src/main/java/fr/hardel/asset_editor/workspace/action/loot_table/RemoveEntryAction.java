package fr.hardel.asset_editor.workspace.action.loot_table;

import fr.hardel.asset_editor.workspace.action.EditorAction;
import fr.hardel.asset_editor.workspace.flush.ElementEntry;
import fr.hardel.asset_editor.workspace.io.RegistryMutationContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.storage.loot.LootTable;

public record RemoveEntryAction(EntryPath path) implements EditorAction<LootTable> {

    public static final StreamCodec<ByteBuf, RemoveEntryAction> CODEC = StreamCodec.composite(
        EntryPath.STREAM_CODEC, RemoveEntryAction::path,
        RemoveEntryAction::new);

    @Override
    public ElementEntry<LootTable> apply(ElementEntry<LootTable> entry, RegistryMutationContext ctx) {
        if (path.resolve(entry.data()) == null)
            return entry;
        return entry.withData(LootTableMutator.removeEntry(entry.data(), path));
    }
}
