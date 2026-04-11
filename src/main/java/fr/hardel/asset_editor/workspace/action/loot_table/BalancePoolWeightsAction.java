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

import java.util.List;

public record BalancePoolWeightsAction(int poolIndex) implements EditorAction<LootTable> {

    public static final StreamCodec<ByteBuf, BalancePoolWeightsAction> CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, BalancePoolWeightsAction::poolIndex,
        BalancePoolWeightsAction::new);

    @Override
    public ElementEntry<LootTable> apply(ElementEntry<LootTable> entry, RegistryMutationContext ctx) {
        var ops = ctx.registries().createSerializationContext(JsonOps.INSTANCE);
        return entry.withData(LootTableMutator.mapPoolEntries(entry.data(), poolIndex, entries -> {
            if (entries.isEmpty())
                return entries;
            int weightPerEntry = Math.max(1, 100 / entries.size());
            return entries.stream()
                .map(e -> LootTableMutator.withWeight(e, weightPerEntry, ops))
                .toList();
        }));
    }
}
