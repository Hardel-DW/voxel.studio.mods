package fr.hardel.asset_editor.workspace.action.loot_table;

import com.mojang.serialization.JsonOps;
import fr.hardel.asset_editor.workspace.action.EditorAction;
import fr.hardel.asset_editor.workspace.flush.ElementEntry;
import fr.hardel.asset_editor.workspace.io.RegistryMutationContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;

public record ReplaceEntryItemAction(EntryPath path, Identifier itemId) implements EditorAction<LootTable> {

    public static final StreamCodec<ByteBuf, ReplaceEntryItemAction> CODEC = StreamCodec.composite(
        EntryPath.STREAM_CODEC, ReplaceEntryItemAction::path,
        Identifier.STREAM_CODEC, ReplaceEntryItemAction::itemId,
        ReplaceEntryItemAction::new);

    @Override
    public ElementEntry<LootTable> apply(ElementEntry<LootTable> entry, RegistryMutationContext ctx) {
        LootPoolEntryContainer target = path.resolve(entry.data());
        if (!(target instanceof LootItem))
            return entry;

        var ops = ctx.registries().createSerializationContext(JsonOps.INSTANCE);
        LootPoolEntryContainer updated = LootTableMutator.withItem(target, itemId, ops);
        return entry.withData(LootTableMutator.replaceEntry(entry.data(), path, updated));
    }
}
