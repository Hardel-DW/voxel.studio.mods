package fr.hardel.asset_editor.workspace.action.loot_table;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.CompositeEntryBase;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;

import java.util.List;

public record EntryPath(int poolIndex, List<Integer> childIndices) {

    public static final StreamCodec<ByteBuf, EntryPath> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, EntryPath::poolIndex,
        ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), EntryPath::childIndices,
        EntryPath::new);

    public LootPoolEntryContainer resolve(LootTable table) {
        if (childIndices.isEmpty() || poolIndex < 0 || poolIndex >= table.pools.size())
            return null;

        List<LootPoolEntryContainer> entries = table.pools.get(poolIndex).entries;
        for (int depth = 0; depth < childIndices.size(); depth++) {
            int idx = childIndices.get(depth);
            if (idx < 0 || idx >= entries.size())
                return null;

            LootPoolEntryContainer entry = entries.get(idx);
            if (depth == childIndices.size() - 1)
                return entry;

            if (!(entry instanceof CompositeEntryBase composite))
                return null;
            entries = composite.children;
        }
        return null;
    }

    public static EntryPath ofTopLevel(int poolIndex, int entryIndex) {
        return new EntryPath(poolIndex, List.of(entryIndex));
    }
}
