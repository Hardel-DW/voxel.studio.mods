package fr.hardel.asset_editor.workspace.action.loot_table;

import fr.hardel.asset_editor.workspace.action.EditorAction;
import fr.hardel.asset_editor.workspace.flush.ElementEntry;
import fr.hardel.asset_editor.workspace.io.RegistryMutationContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;

public record AddEntryAction(int poolIndex, Identifier itemId, int weight) implements EditorAction<LootTable> {

    public static final StreamCodec<ByteBuf, AddEntryAction> CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, AddEntryAction::poolIndex,
        Identifier.STREAM_CODEC, AddEntryAction::itemId,
        ByteBufCodecs.VAR_INT, AddEntryAction::weight,
        AddEntryAction::new);

    @Override
    public ElementEntry<LootTable> apply(ElementEntry<LootTable> entry, RegistryMutationContext ctx) {
        var holder = BuiltInRegistries.ITEM.get(itemId).orElse(null);
        if (holder == null)
            return entry;

        LootPoolEntryContainer newEntry = LootItem.lootTableItem(holder.value()).setWeight(weight).build();
        return entry.withData(LootTableMutator.addEntry(entry.data(), poolIndex, newEntry));
    }
}
