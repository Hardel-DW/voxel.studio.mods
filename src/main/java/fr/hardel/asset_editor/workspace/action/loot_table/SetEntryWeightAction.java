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

public record SetEntryWeightAction(EntryPath path, int weight) implements EditorAction<LootTable> {

    public static final StreamCodec<ByteBuf, SetEntryWeightAction> CODEC = StreamCodec.composite(
        EntryPath.STREAM_CODEC, SetEntryWeightAction::path,
        ByteBufCodecs.VAR_INT, SetEntryWeightAction::weight,
        SetEntryWeightAction::new);

    @Override
    public ElementEntry<LootTable> apply(ElementEntry<LootTable> entry, RegistryMutationContext ctx) {
        LootPoolEntryContainer target = path.resolve(entry.data());
        if (!(target instanceof LootPoolSingletonContainer))
            return entry;

        var ops = ctx.registries().createSerializationContext(JsonOps.INSTANCE);
        LootPoolEntryContainer updated = LootTableMutator.withWeight(target, Math.max(1, weight), ops);
        return entry.withData(LootTableMutator.replaceEntry(entry.data(), path, updated));
    }
}
