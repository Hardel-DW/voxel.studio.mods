package fr.hardel.asset_editor.workspace.action.loot_table;

import com.mojang.serialization.JsonOps;
import fr.hardel.asset_editor.workspace.action.EditorAction;
import fr.hardel.asset_editor.workspace.flush.ElementEntry;
import fr.hardel.asset_editor.workspace.io.RegistryMutationContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer;

public record SetEntryCountMinAction(EntryPath path, int min) implements EditorAction<LootTable> {

    public static final StreamCodec<ByteBuf, SetEntryCountMinAction> CODEC = StreamCodec.composite(
        EntryPath.STREAM_CODEC, SetEntryCountMinAction::path,
        ByteBufCodecs.VAR_INT, SetEntryCountMinAction::min,
        SetEntryCountMinAction::new);

    @Override
    public ElementEntry<LootTable> apply(ElementEntry<LootTable> entry, RegistryMutationContext ctx) {
        LootPoolEntryContainer target = path.resolve(entry.data());
        if (!(target instanceof LootPoolSingletonContainer singleton))
            return entry;

        int[] range = LootTableMutator.currentCountRange(singleton);
        int newMin = Math.max(1, min);
        var ops = ctx.registries().createSerializationContext(JsonOps.INSTANCE);
        LootPoolEntryContainer updated = LootTableMutator.withCount(target, newMin, range[1], ops);
        return entry.withData(LootTableMutator.replaceEntry(entry.data(), path, updated));
    }
}
