package fr.hardel.asset_editor.workspace.action;

import fr.hardel.asset_editor.workspace.flush.ElementEntry;
import fr.hardel.asset_editor.workspace.io.RegistryMutationContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record SetEntryDataAction(String json) implements EditorAction<Object> {

    private static final int MAX_JSON_BYTES = 1 << 20;

    public static final StreamCodec<ByteBuf, SetEntryDataAction> CODEC = StreamCodec.composite(
        ByteBufCodecs.stringUtf8(MAX_JSON_BYTES), SetEntryDataAction::json,
        SetEntryDataAction::new);

    @Override
    public ElementEntry<Object> apply(ElementEntry<Object> entry, RegistryMutationContext ctx) {
        throw new UnsupportedOperationException("SetEntryDataAction is dispatched by WorkspaceDefinition");
    }
}
