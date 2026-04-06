package fr.hardel.asset_editor.workspace.action;

import fr.hardel.asset_editor.tag.TagSeed;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

import java.util.Optional;

public interface EditorAction {

    StreamCodec<ByteBuf, TagSeed> OPTIONAL_TAG_SEED_CODEC = ByteBufCodecs.optional(TagSeed.STREAM_CODEC)
        .map(optional -> optional.orElse(null), Optional::ofNullable);

    StreamCodec<ByteBuf, EditorAction> STREAM_CODEC = EditorActionRegistry.streamCodec();

    EditorActionType<?, ? extends EditorAction> type();

    default Identifier typeId() {
        return type().id();
    }
}
