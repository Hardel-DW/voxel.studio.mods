package fr.hardel.asset_editor.workspace.action.loot_table;

import fr.hardel.asset_editor.workspace.action.EditorAction;
import fr.hardel.asset_editor.workspace.flush.ElementEntry;
import fr.hardel.asset_editor.workspace.flush.adapter.LootTableFlushAdapter;
import fr.hardel.asset_editor.workspace.io.RegistryMutationContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.storage.loot.LootTable;

public record ToggleDisabledAction() implements EditorAction<LootTable> {

    public static final StreamCodec<ByteBuf, ToggleDisabledAction> CODEC = StreamCodec.unit(new ToggleDisabledAction());

    @Override
    public ElementEntry<LootTable> apply(ElementEntry<LootTable> entry, RegistryMutationContext ctx) {
        boolean current = LootTableFlushAdapter.disabled(entry);
        return entry.withCustom(entry.custom().with(LootTableFlushAdapter.DISABLED_KEY, !current));
    }
}
